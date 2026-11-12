package com.valenciaj.sudoku.core

/** Modos de juego. */
enum class Mode {
    CLASSIC,

    /** Hay que terminar antes de que acabe el tiempo; cada error resta [TimeAttack.PENALTY_SECONDS]. */
    TIME_ATTACK,

    /** Sudoku Killer: casi sin pistas, con jaulas que indican la suma de sus casillas. */
    KILLER,
}

object TimeAttack {
    const val PENALTY_SECONDS = 30L

    fun limitSeconds(level: Difficulty): Long = when (level) {
        Difficulty.EASY -> 5 * 60
        Difficulty.MEDIUM -> 10 * 60
        Difficulty.HARD -> 15 * 60
        Difficulty.DEMENTIAL -> 25 * 60
    }
}
