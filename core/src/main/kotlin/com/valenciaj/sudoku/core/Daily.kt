package com.valenciaj.sudoku.core

import kotlin.random.Random

/**
 * Sudoku diario: el mismo para todos en un día dado. El nivel depende del día:
 * lunes y martes Fácil, miércoles y jueves Intermedio, viernes y sábado Difícil, domingo Demencial.
 * Los días se cuentan como "epoch day" (días desde el 1-1-1970).
 */
object Daily {
    fun difficultyFor(epochDay: Long): Difficulty = when (Math.floorMod(epochDay + 3, 7L).toInt()) {
        0, 1 -> Difficulty.EASY
        2, 3 -> Difficulty.MEDIUM
        4, 5 -> Difficulty.HARD
        else -> Difficulty.DEMENTIAL
    }

    fun puzzle(epochDay: Long): Puzzle =
        Generator(Random(epochDay * 7919 + 17)).generate(difficultyFor(epochDay))
}
