package com.valenciaj.sudoku.core

import kotlin.random.Random

class Puzzle(
    val givens: IntArray,
    val solution: IntArray,
    val difficulty: Difficulty,
) {
    val clueCount get() = givens.count { it != 0 }
}

/**
 * Genera sudokus con solución única y la dificultad pedida.
 *
 * Parte de una solución completa aleatoria y va quitando números (en orden aleatorio)
 * mientras la solución siga siendo única. A partir de cierto número de pistas evalúa
 * la dificultad: si coincide, termina; si se pasa, deshace esa eliminación.
 */
class Generator(private val random: Random = Random.Default) {

    fun generate(target: Difficulty, maxAttempts: Int = 500): Puzzle {
        var best: Puzzle? = null
        repeat(maxAttempts) {
            val p = attempt(target)
            if (p.difficulty == target) return p
            if (best == null || p.difficulty > best!!.difficulty) best = p
        }
        return best!!
    }

    private fun attempt(target: Difficulty): Puzzle {
        val solution = Solver.solve(IntArray(Grid.CELLS), random)!!
        val puzzle = solution.copyOf()
        var clues = Grid.CELLS
        var grade = Difficulty.EASY
        for (cell in (0 until Grid.CELLS).shuffled(random)) {
            val saved = puzzle[cell]
            puzzle[cell] = 0
            if (!Solver.hasUniqueSolution(puzzle)) { puzzle[cell] = saved; continue }
            clues--
            if (clues > MAX_CLUES.getValue(target)) continue
            val g = LogicalSolver.grade(puzzle)
            when {
                g == target -> return Puzzle(puzzle, solution, g)
                g > target -> { puzzle[cell] = saved; clues++ }
                else -> grade = g
            }
        }
        return Puzzle(puzzle, solution, grade)
    }

    companion object {
        /** Máximo de pistas con el que se acepta un sudoku de cada nivel. */
        private val MAX_CLUES = mapOf(
            Difficulty.EASY to 40,
            Difficulty.MEDIUM to 34,
            Difficulty.HARD to 30,
            Difficulty.DEMENTIAL to 28,
        )
    }
}
