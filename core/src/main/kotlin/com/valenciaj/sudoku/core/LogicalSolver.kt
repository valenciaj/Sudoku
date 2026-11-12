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

    private class State(puzzle: IntArray) {
        val values = puzzle.copyOf()
        val cand = IntArray(Grid.CELLS)

        init {
            for (cell in 0 until Grid.CELLS) {
                cand[cell] = if (values[cell] == 0) Solver.candidates(values, cell) else 0
            }
        }

        fun run(): Difficulty {
            var level = Difficulty.EASY
            while (true) {
                if (values.none { it == 0 }) return level
                if (singles()) continue
                if (pointingAndClaiming() || nakedSubsets(2) || hiddenSubsets(2)) {
                    level = maxOf(level, Difficulty.MEDIUM); continue
                }
                if (nakedSubsets(3) || hiddenSubsets(3) || xWing()) {
                    level = maxOf(level, Difficulty.HARD); continue
                }
                return Difficulty.DEMENTIAL
            }
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

        fun singles(): Boolean {
            for (cell in 0 until Grid.CELLS) {
                if (values[cell] == 0 && Integer.bitCount(cand[cell]) == 1) {
                    place(cell, Grid.firstDigit(cand[cell])); return true
                }
            }
            for (unit in Grid.units) {
                for (d in 1..9) {
                    val b = Grid.bit(d)
                    var only = -1
                    var count = 0
                    for (cell in unit) if (cand[cell] and b != 0) { only = cell; count++ }
                    if (count == 1) { place(only, d); return true }
                }
            }
            return false
        }

        /** Pares/tríos apuntadores (caja → línea) y reducción línea → caja. */
        fun pointingAndClaiming(): Boolean {
            var changed = false
            for (d in 1..9) {
                val b = Grid.bit(d)
                for (box in Grid.boxes) {
                    val cells = box.filter { cand[it] and b != 0 }
                    if (cells.isEmpty()) continue
                    val r = Grid.row(cells[0])
                    if (cells.all { Grid.row(it) == r }) {
                        for (c in Grid.rows[r]) if (c !in box) changed = eliminate(c, b) || changed
                    }
                    val col = Grid.col(cells[0])
                    if (cells.all { Grid.col(it) == col }) {
                        for (c in Grid.cols[col]) if (c !in box) changed = eliminate(c, b) || changed
                    }
                }
                for (line in Grid.rows + Grid.cols) {
                    val cells = line.filter { cand[it] and b != 0 }
                    if (cells.isEmpty()) continue
                    val bx = Grid.box(cells[0])
                    if (cells.all { Grid.box(it) == bx }) {
                        for (c in Grid.boxes[bx]) if (c !in line) changed = eliminate(c, b) || changed
                    }
                }
                if (changed) return true
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
                    if (changed) return true
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
                    if (changed) return true
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
                if (changed) return true
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
