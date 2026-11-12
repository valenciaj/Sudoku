package com.valenciaj.sudoku.core

/**
 * Estado inmutable de una partida en curso.
 *
 * [notes] guarda, por celda, una máscara de bits con las notas a lápiz (bit 0 = dígito 1).
 * [history] permite deshacer; no se guarda en disco.
 */
data class Game(
    val puzzle: Puzzle,
    val values: List<Int> = puzzle.givens.toList(),
    val notes: List<Int> = List(Grid.CELLS) { 0 },
    val mistakes: Int = 0,
    val elapsedSeconds: Long = 0,
    val hintsUsed: Int = 0,
    /** Día (epoch day) si es el sudoku diario; null si es una partida normal. */
    val dailyDay: Long? = null,
    val mode: Mode = Mode.CLASSIC,
    /** Solo en contrarreloj: tiempo máximo en segundos. */
    val timeLimitSeconds: Long? = null,
    val history: List<Snapshot> = emptyList(),
) {
    data class Snapshot(val values: List<Int>, val notes: List<Int>)

    fun isGiven(cell: Int) = puzzle.givens[cell] != 0

    fun isWrong(cell: Int) = values[cell] != 0 && values[cell] != puzzle.solution[cell]

    fun hasNote(cell: Int, digit: Int) = notes[cell] and Grid.bit(digit) != 0

    val isSolved get() = values.indices.all { values[it] == puzzle.solution[it] }

    /** Contrarreloj: se acabó el tiempo sin terminar. */
    val isLost get() = timeLimitSeconds != null && elapsedSeconds >= timeLimitSeconds && !isSolved

    val isOver get() = isSolved || isLost

    val remainingSeconds: Long? get() = timeLimitSeconds?.let { maxOf(0, it - elapsedSeconds) }

    val canUndo get() = history.isNotEmpty()

    /** Cuántas veces está ya colocado correctamente cada dígito (índice 1..9). */
    fun placedCount(digit: Int) = values.indices.count { values[it] == digit && !isWrong(it) }

    /**
     * Escribe [digit] (0 = borrar). Las pistas iniciales no se pueden cambiar.
     * Al acertar, borra esa nota de las celdas relacionadas.
     */
    fun input(cell: Int, digit: Int): Game {
        if (isLost || isGiven(cell) || (values[cell] == digit && (digit != 0 || notes[cell] == 0))) return this
        val newValues = values.toMutableList().also { it[cell] = digit }
        val newNotes = notes.toMutableList().also { it[cell] = 0 }
        val wrong = digit != 0 && digit != puzzle.solution[cell]
        if (digit != 0 && !wrong) {
            val clear = Grid.bit(digit).inv()
            for (p in Grid.peers[cell]) newNotes[p] = newNotes[p] and clear
        }
        return copy(
            values = newValues,
            notes = newNotes,
            mistakes = mistakes + if (wrong) 1 else 0,
            history = history + snapshot(),
        ).let { if (wrong && mode == Mode.TIME_ATTACK) it.addTime(TimeAttack.PENALTY_SECONDS) else it }
    }

    /** Pone o quita una nota a lápiz. Solo en celdas vacías. */
    fun toggleNote(cell: Int, digit: Int): Game {
        if (isLost || isGiven(cell) || values[cell] != 0) return this
        val newNotes = notes.toMutableList().also { it[cell] = it[cell] xor Grid.bit(digit) }
        return copy(notes = newNotes, history = history + snapshot())
    }

    /** Deshace el último cambio. Los errores no se descuentan. */
    fun undo(): Game {
        val last = history.lastOrNull() ?: return this
        return copy(values = last.values, notes = last.notes, history = history.dropLast(1))
    }

    /** Pide una pista (cuenta como ayuda usada). */
    fun hint(): Pair<Game, Hint?> {
        if (isLost) return this to null
        val h = Hints.next(this) ?: return this to null
        return copy(hintsUsed = hintsUsed + 1) to h
    }

    /** Escribe la solución en la casilla (no cuenta como error). */
    fun reveal(cell: Int): Game {
        if (isGiven(cell) || values[cell] == puzzle.solution[cell]) return this
        return input(cell, puzzle.solution[cell])
    }

    fun tick(seconds: Long = 1) = if (isOver) this else addTime(seconds)

    private fun addTime(seconds: Long): Game {
        val t = elapsedSeconds + seconds
        return copy(elapsedSeconds = timeLimitSeconds?.let { minOf(it, t) } ?: t)
    }

    private fun snapshot() = Snapshot(values, notes)

    /** Texto para guardar la partida (sin el historial de deshacer). */
    fun encode(): String = listOf(
        FORMAT_VERSION,
        puzzle.difficulty.name,
        puzzle.givens.joinToString(""),
        puzzle.solution.joinToString(""),
        values.joinToString(""),
        notes.joinToString(","),
        mistakes.toString(),
        elapsedSeconds.toString(),
        hintsUsed.toString(),
        (dailyDay ?: -1).toString(),
        mode.name,
        (timeLimitSeconds ?: -1).toString(),
        puzzle.cages.joinToString(";") { c -> c.cells.joinToString(",") + ":" + c.sum },
    ).joinToString("\n")

    companion object {
        private const val FORMAT_VERSION = "3"

        /** Lee una partida guardada; null si el texto no es válido. */
        fun decode(text: String): Game? = runCatching {
            val l = text.split("\n")
            // Versión 1: sin pistas ni diario (8 líneas). Versión 2: 10 líneas.
            // v1: 8 líneas; v2: + pistas y diario (10); v3: + modo, límite y jaulas (13).
            require((l[0] == "1" && l.size == 8) || (l[0] == "2" && l.size == 10) || (l[0] == FORMAT_VERSION && l.size == 13))
            val cages = l.getOrNull(12).orEmpty().split(";").filter { it.isNotBlank() }.map { c ->
                val (cells, sum) = c.split(":")
                Cage(cells.split(",").map { it.toInt() }.toIntArray(), sum.toInt())
            }
            fun digits(s: String) = s.map { it.digitToInt() }.also { require(it.size == Grid.CELLS) }
            val puzzle = Puzzle(
                givens = digits(l[2]).toIntArray(),
                solution = digits(l[3]).toIntArray(),
                difficulty = Difficulty.valueOf(l[1]),
                cages = cages,
            )
            Game(
                puzzle = puzzle,
                values = digits(l[4]),
                notes = l[5].split(",").map { it.toInt() }.also { require(it.size == Grid.CELLS) },
                mistakes = l[6].toInt(),
                elapsedSeconds = l[7].toLong(),
                hintsUsed = l.getOrNull(8)?.toInt() ?: 0,
                dailyDay = l.getOrNull(9)?.toLong()?.takeIf { it >= 0 },
                mode = l.getOrNull(10)?.let { Mode.valueOf(it) } ?: Mode.CLASSIC,
                timeLimitSeconds = l.getOrNull(11)?.toLong()?.takeIf { it >= 0 },
            )
        }.getOrNull()
    }
}
