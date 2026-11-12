package com.valenciaj.sudoku.core

/** Estado inmutable de una partida en curso. */
data class Game(
    val puzzle: Puzzle,
    val values: List<Int> = puzzle.givens.toList(),
    val mistakes: Int = 0,
) {
    fun isGiven(cell: Int) = puzzle.givens[cell] != 0

    fun isWrong(cell: Int) = values[cell] != 0 && values[cell] != puzzle.solution[cell]

    val isSolved get() = values.indices.all { values[it] == puzzle.solution[it] }

    /** Escribe [digit] (0 = borrar). Las pistas iniciales no se pueden cambiar. */
    fun input(cell: Int, digit: Int): Game {
        if (isGiven(cell) || values[cell] == digit) return this
        val newValues = values.toMutableList().also { it[cell] = digit }
        val wrong = digit != 0 && digit != puzzle.solution[cell]
        return copy(values = newValues, mistakes = mistakes + if (wrong) 1 else 0)
    }
}
