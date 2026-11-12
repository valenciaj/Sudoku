package com.valenciaj.sudoku.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeneratorTest {

    @Test
    fun `cada nivel genera un sudoku valido, unico y con la dificultad pedida`() {
        val generator = Generator(Random(42))
        for (level in Difficulty.entries) {
            repeat(3) {
                val start = System.currentTimeMillis()
                val p = generator.generate(level)
                val ms = System.currentTimeMillis() - start
                println("${level.label}: ${p.clueCount} pistas, ${ms} ms")
                assertEquals(level, p.difficulty)
                assertEquals(level, LogicalSolver.grade(p.givens))
                assertTrue(Solver.hasUniqueSolution(p.givens))
                assertTrue(isValidSolution(p.solution))
                for (i in 0 until Grid.CELLS) {
                    if (p.givens[i] != 0) assertEquals(p.solution[i], p.givens[i])
                }
            }
        }
    }

    @Test
    fun `un sudoku famoso dificil se clasifica como demencial`() {
        // "AI Escargot" (Arto Inkala), requiere técnicas muy avanzadas.
        val escargot = "100007090030020008009600500005300900010080002600004000300000010040000007007000300"
        val grid = escargot.map { it - '0' }.toIntArray()
        assertEquals(Difficulty.DEMENTIAL, LogicalSolver.grade(grid))
        assertTrue(Solver.hasUniqueSolution(grid))
    }

    @Test
    fun `el juego cuenta errores y detecta la victoria`() {
        val p = Generator(Random(1)).generate(Difficulty.EASY)
        var game = Game(p)
        val empty = (0 until Grid.CELLS).first { !game.isGiven(it) }
        val wrongDigit = p.solution[empty] % 9 + 1
        game = game.input(empty, wrongDigit)
        assertEquals(1, game.mistakes)
        assertTrue(game.isWrong(empty))
        for (i in 0 until Grid.CELLS) game = game.input(i, p.solution[i])
        assertTrue(game.isSolved)
        // Las pistas no se pueden sobrescribir.
        val given = (0 until Grid.CELLS).first { game.isGiven(it) }
        assertEquals(game, game.input(given, 0))
    }

    private fun isValidSolution(grid: IntArray) =
        Grid.units.all { unit -> unit.map { grid[it] }.toSet() == (1..9).toSet() }
}
