package com.valenciaj.sudoku.core

/**
 * Resuelve como lo haría una persona, aplicando técnicas por orden de dificultad.
 * La dificultad de un sudoku es la técnica más avanzada que hizo falta:
 *
 * - Fácil: solo singles (desnudos y ocultos).
 * - Intermedio: + pares/líneas apuntadoras, reducción caja-línea, pares desnudos y ocultos.
 * - Difícil: + tríos desnudos y ocultos, X-Wing.
 * - Demencial: no se resuelve con lo anterior (necesita técnicas avanzadas o tanteo).
 */
object LogicalSolver {

    fun grade(puzzle: IntArray): Difficulty = State(puzzle).run()

    /**
     * Busca, resolviendo paso a paso, el primer momento en que se usa [technique].
     * Devuelve el estado justo antes de ese paso (para el tutorial) o null si no aparece.
     */
    fun findExample(puzzle: IntArray, technique: Technique): Example? {
        val state = State(puzzle)
        while (state.values.any { it == 0 }) {
            val values = state.values.copyOf()
            val cand = state.cand.copyOf()
            val used = state.step() ?: return null
            if (used == technique) {
                val eliminations = (0 until Grid.CELLS)
                    .associateWith { cand[it] and state.cand[it].inv() }
                    .filter { (cell, mask) -> mask != 0 && state.values[cell] == 0 }
                val placed = (0 until Grid.CELLS).firstOrNull { values[it] == 0 && state.values[it] != 0 }
                return Example(
                    technique, values, cand, state.pattern, state.patternDigits, eliminations,
                    placed?.let { it to state.values[it] },
                )
            }
        }
        return null
    }

    private class State(puzzle: IntArray) {
        val values = puzzle.copyOf()
        val cand = IntArray(Grid.CELLS)

        /** Casillas y dígitos que forman el patrón del último paso (para explicarlo). */
        var pattern: List<Int> = emptyList()
        var patternDigits = 0

        init {
            for (cell in 0 until Grid.CELLS) {
                cand[cell] = if (values[cell] == 0) Solver.candidates(values, cell) else 0
            }
        }

        fun run(): Difficulty {
            var level = Difficulty.EASY
            while (true) {
                if (values.none { it == 0 }) return level
                val technique = step() ?: return Difficulty.DEMENTIAL
                level = maxOf(level, technique.level)
            }
        }

        /** Aplica la técnica más sencilla posible; null si ninguna sirve. */
        fun step(): Technique? = when {
            nakedSingle() -> Technique.NAKED_SINGLE
            hiddenSingle() -> Technique.HIDDEN_SINGLE
            pointingAndClaiming() -> Technique.POINTING
            nakedSubsets(2) -> Technique.NAKED_PAIR
            hiddenSubsets(2) -> Technique.HIDDEN_PAIR
            nakedSubsets(3) -> Technique.NAKED_TRIPLE
            hiddenSubsets(3) -> Technique.HIDDEN_TRIPLE
            xWing() -> Technique.X_WING
            else -> null
        }

        fun place(cell: Int, digit: Int) {
            values[cell] = digit
            cand[cell] = 0
            val b = Grid.bit(digit).inv()
            for (p in Grid.peers[cell]) cand[p] = cand[p] and b
        }

        /** Elimina [mask] de [cell]; true si cambió algo. */
        fun eliminate(cell: Int, mask: Int): Boolean {
            if (cand[cell] and mask == 0) return false
            cand[cell] = cand[cell] and mask.inv()
            return true
        }

        fun nakedSingle(): Boolean {
            for (cell in 0 until Grid.CELLS) {
                if (values[cell] == 0 && Integer.bitCount(cand[cell]) == 1) {
                    pattern = listOf(cell)
                    patternDigits = cand[cell]
                    place(cell, Grid.firstDigit(cand[cell])); return true
                }
            }
            return false
        }

        fun hiddenSingle(): Boolean {
            for (unit in Grid.units) {
                for (d in 1..9) {
                    val b = Grid.bit(d)
                    var only = -1
                    var count = 0
                    for (cell in unit) if (cand[cell] and b != 0) { only = cell; count++ }
                    if (count == 1) {
                        pattern = unit.toList()
                        patternDigits = b
                        place(only, d); return true
                    }
                }
            }
            return false
        }

        /** Pares/tríos apuntadores (caja → línea) y reducción línea → caja. */
        fun pointingAndClaiming(): Boolean {
            for (d in 1..9) {
                val b = Grid.bit(d)
                fun found(cells: List<Int>): Boolean {
                    pattern = cells
                    patternDigits = b
                    return true
                }
                for (box in Grid.boxes) {
                    val cells = box.filter { cand[it] and b != 0 }
                    if (cells.isEmpty()) continue
                    val r = Grid.row(cells[0])
                    if (cells.all { Grid.row(it) == r }) {
                        var changed = false
                        for (c in Grid.rows[r]) if (c !in box) changed = eliminate(c, b) || changed
                        if (changed) return found(cells)
                    }
                    val col = Grid.col(cells[0])
                    if (cells.all { Grid.col(it) == col }) {
                        var changed = false
                        for (c in Grid.cols[col]) if (c !in box) changed = eliminate(c, b) || changed
                        if (changed) return found(cells)
                    }
                }
                for (line in Grid.rows + Grid.cols) {
                    val cells = line.filter { cand[it] and b != 0 }
                    if (cells.isEmpty()) continue
                    val bx = Grid.box(cells[0])
                    if (cells.all { Grid.box(it) == bx }) {
                        var changed = false
                        for (c in Grid.boxes[bx]) if (c !in line) changed = eliminate(c, b) || changed
                        if (changed) return found(cells)
                    }
                }
            }
            return false
        }

        /** k celdas de una unidad cuyos candidatos suman exactamente k dígitos. */
        fun nakedSubsets(k: Int): Boolean {
            for (unit in Grid.units) {
                val empties = unit.filter { values[it] == 0 }
                if (empties.size <= k) continue
                for (combo in combinations(empties, k)) {
                    var union = 0
                    for (c in combo) union = union or cand[c]
                    if (Integer.bitCount(union) != k) continue
                    var changed = false
                    for (c in empties) if (c !in combo) changed = eliminate(c, union) || changed
                    if (changed) {
                        pattern = combo
                        patternDigits = union
                        return true
                    }
                }
            }
            return false
        }

        /** k dígitos que en una unidad solo caben en las mismas k celdas. */
        fun hiddenSubsets(k: Int): Boolean {
            for (unit in Grid.units) {
                val digits = (1..9).filter { d -> unit.any { cand[it] and Grid.bit(d) != 0 } }
                if (digits.size <= k) continue
                for (combo in combinations(digits, k)) {
                    var mask = 0
                    for (d in combo) mask = mask or Grid.bit(d)
                    val cells = unit.filter { cand[it] and mask != 0 }
                    if (cells.size != k) continue
                    var changed = false
                    for (c in cells) changed = eliminate(c, mask.inv() and Grid.ALL_CANDIDATES) || changed
                    if (changed) {
                        pattern = cells
                        patternDigits = mask
                        return true
                    }
                }
            }
            return false
        }

        fun xWing(): Boolean {
            for (d in 1..9) {
                val b = Grid.bit(d)
                if (xWing(b, Grid.rows, Grid::col, Grid.cols)) return true
                if (xWing(b, Grid.cols, Grid::row, Grid.rows)) return true
            }
            return false
        }

        private fun xWing(b: Int, base: List<IntArray>, cross: (Int) -> Int, crossUnits: List<IntArray>): Boolean {
            val pairs = base.mapIndexedNotNull { i, line ->
                val pos = line.filter { cand[it] and b != 0 }.map(cross)
                if (pos.size == 2) i to pos else null
            }
            for (i in pairs.indices) for (j in i + 1 until pairs.size) {
                if (pairs[i].second != pairs[j].second) continue
                val baseLines = setOf(pairs[i].first, pairs[j].first)
                var changed = false
                for (x in pairs[i].second) {
                    for (cell in crossUnits[x]) {
                        val lineIndex = if (base === Grid.rows) Grid.row(cell) else Grid.col(cell)
                        if (lineIndex !in baseLines) changed = eliminate(cell, b) || changed
                    }
                }
                if (changed) {
                    pattern = (base[pairs[i].first].toList() + base[pairs[j].first].toList()).filter { cand[it] and b != 0 }
                    patternDigits = b
                    return true
                }
            }
            return false
        }
    }

    private fun <T> combinations(items: List<T>, k: Int): Sequence<List<T>> = sequence {
        if (k == 0) { yield(emptyList()); return@sequence }
        for (i in 0..items.size - k) {
            for (rest in combinations(items.subList(i + 1, items.size), k - 1)) {
                yield(listOf(items[i]) + rest)
            }
        }
    }
}

/** Técnicas de resolución, con el nivel en el que empiezan a hacer falta. */
enum class Technique(val level: Difficulty) {
    NAKED_SINGLE(Difficulty.EASY),
    HIDDEN_SINGLE(Difficulty.EASY),
    POINTING(Difficulty.MEDIUM),
    NAKED_PAIR(Difficulty.MEDIUM),
    HIDDEN_PAIR(Difficulty.MEDIUM),
    NAKED_TRIPLE(Difficulty.HARD),
    HIDDEN_TRIPLE(Difficulty.HARD),
    X_WING(Difficulty.HARD),
    ;

    /** True si la técnica coloca un número; false si solo descarta candidatos. */
    val places get() = this == NAKED_SINGLE || this == HIDDEN_SINGLE
}

/** Un momento de una partida real en el que se usa una técnica (para el tutorial). */
class Example(
    val technique: Technique,
    /** Tablero antes del paso. */
    val values: IntArray,
    /** Candidatos de cada casilla antes del paso (máscara de bits). */
    val candidates: IntArray,
    /** Casillas que forman el patrón. */
    val pattern: List<Int>,
    /** Dígitos del patrón (máscara de bits). */
    val patternDigits: Int,
    /** Candidatos que se pueden descartar: casilla → máscara. */
    val eliminations: Map<Int, Int>,
    /** Número que se coloca (en las técnicas de "single"): casilla → dígito. */
    val placement: Pair<Int, Int>?,
)

object Tutorial {
    /** Técnicas que tienen lección. Los tríos son tan raros en partidas reales que no se incluyen. */
    val lessons = listOf(
        Technique.NAKED_SINGLE,
        Technique.HIDDEN_SINGLE,
        Technique.POINTING,
        Technique.NAKED_PAIR,
        Technique.HIDDEN_PAIR,
        Technique.X_WING,
    )

    /** Genera un ejemplo nuevo de [technique] a partir de sudokus aleatorios. */
    fun example(technique: Technique, random: kotlin.random.Random = kotlin.random.Random.Default): Example? {
        val level = maxOf(technique.level, Difficulty.MEDIUM)
        repeat(300) {
            val puzzle = Generator(random).generate(level, maxAttempts = 5)
            LogicalSolver.findExample(puzzle.givens, technique)?.let { return it }
        }
        return null
    }
}
