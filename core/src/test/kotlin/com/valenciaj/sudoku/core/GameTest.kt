package com.valenciaj.sudoku.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameTest {
    private val puzzle = Generator(Random(7)).generate(Difficulty.MEDIUM)
    private val empty = (0 until Grid.CELLS).filter { puzzle.givens[it] == 0 }

    @Test
    fun `notas se ponen, se quitan y se limpian al acertar en una celda relacionada`() {
        val a = empty.first()
        val digit = puzzle.solution[a]
        val peer = empty.first { it != a && it in Grid.peers[a] }
        var game = Game(puzzle).toggleNote(peer, digit).toggleNote(peer, digit % 9 + 1)
        assertTrue(game.hasNote(peer, digit))
        game = game.input(a, digit)
        assertFalse(game.hasNote(peer, digit))
        assertTrue(game.hasNote(peer, digit % 9 + 1))
        game = game.toggleNote(peer, digit % 9 + 1)
        assertEquals(0, game.notes[peer])
    }

    @Test
    fun `deshacer revierte valores y notas pero no los errores`() {
        val a = empty.first()
        val wrong = puzzle.solution[a] % 9 + 1
        val start = Game(puzzle)
        var game = start.toggleNote(a, 5).input(a, wrong)
        assertEquals(1, game.mistakes)
        game = game.undo()
        assertEquals(0, game.values[a])
        assertTrue(game.hasNote(a, 5))
        assertEquals(1, game.mistakes)
        game = game.undo()
        assertEquals(start.values, game.values)
        assertEquals(start.notes, game.notes)
        assertFalse(game.canUndo)
    }

    @Test
    fun `el cronometro se para al resolver`() {
        var game = Game(puzzle).tick().tick()
        assertEquals(2, game.elapsedSeconds)
        for (i in 0 until Grid.CELLS) game = game.input(i, puzzle.solution[i])
        assertEquals(2, game.tick().elapsedSeconds)
    }

    @Test
    fun `guardar y cargar conserva la partida`() {
        val game = Game(puzzle).input(empty[0], 3).toggleNote(empty[1], 4).tick(42)
        val loaded = assertNotNull(Game.decode(game.encode()))
        assertEquals(game.values, loaded.values)
        assertEquals(game.notes, loaded.notes)
        assertEquals(game.mistakes, loaded.mistakes)
        assertEquals(42, loaded.elapsedSeconds)
        assertEquals(puzzle.difficulty, loaded.puzzle.difficulty)
        assertTrue(puzzle.solution.contentEquals(loaded.puzzle.solution))
        assertNull(Game.decode("basura"))
    }
}
