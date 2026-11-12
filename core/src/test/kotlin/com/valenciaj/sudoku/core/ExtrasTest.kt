package com.valenciaj.sudoku.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExtrasTest {

    @Test
    fun `las pistas siempre son correctas y permiten terminar el sudoku`() {
        for (level in Difficulty.entries) {
            var game = Game(Generator(Random(level.ordinal)).generate(level))
            var steps = 0
            while (!game.isSolved) {
                val (next, hint) = game.hint()
                assertNotNull(hint)
                assertEquals(game.puzzle.solution[hint.cell], hint.digit)
                assertEquals(0, game.values[hint.cell])
                game = next.reveal(hint.cell)
                steps++
            }
            assertEquals(0, game.mistakes)
            assertEquals(steps, game.hintsUsed)
            assertNull(game.hint().second)
        }
    }

    @Test
    fun `la pista avisa primero de un numero incorrecto`() {
        val p = Generator(Random(3)).generate(Difficulty.EASY)
        val cell = p.givens.indices.first { p.givens[it] == 0 }
        val game = Game(p).input(cell, p.solution[cell] % 9 + 1)
        val hint = assertNotNull(game.hint().second)
        assertEquals(Hint.Kind.WRONG, hint.kind)
        assertEquals(cell, hint.cell)
    }

    @Test
    fun `el diario es el mismo cada dia y su nivel depende del dia de la semana`() {
        val monday = java.time.LocalDate.of(2025, 11, 10).toEpochDay() // lunes
        assertEquals(Difficulty.EASY, Daily.difficultyFor(monday))
        assertEquals(Difficulty.MEDIUM, Daily.difficultyFor(monday + 2))
        assertEquals(Difficulty.HARD, Daily.difficultyFor(monday + 4))
        assertEquals(Difficulty.DEMENTIAL, Daily.difficultyFor(monday + 6))
        val a = Daily.puzzle(monday + 1)
        val b = Daily.puzzle(monday + 1)
        assertTrue(a.givens.contentEquals(b.givens))
        assertFalse(a.givens.contentEquals(Daily.puzzle(monday + 8).givens))
        assertEquals(Daily.difficultyFor(monday + 6), Daily.puzzle(monday + 6).difficulty)
    }

    @Test
    fun `racha diaria y logros`() {
        var s = Stats()
        assertTrue(Achievement.entries.none { it.isUnlocked(s) })
        for (day in 100L..106L) s = s.recordDailyWin(day)
        s = s.recordDailyWin(106) // repetido: no cuenta
        assertEquals(7, s.daily.completed)
        assertEquals(7, s.daily.currentStreak(today = 107))
        assertEquals(0, s.daily.currentStreak(today = 108))
        assertTrue(Achievement.STREAK_7.isUnlocked(s))
        s = s.recordDailyWin(110)
        assertEquals(1, s.daily.streak)
        assertEquals(7, s.daily.bestStreak)

        s = s.recordStart(Difficulty.EASY).recordWin(Difficulty.EASY, 120, mistakes = 0, hints = 0)
        assertTrue(Achievement.FIRST_WIN.isUnlocked(s))
        assertTrue(Achievement.FAST.isUnlocked(s))
        assertTrue(Achievement.NO_HELP.isUnlocked(s))
        assertFalse(Achievement.ALL_LEVELS.isUnlocked(s))
        assertEquals(s, Stats.decode(s.encode()))
    }

    @Test
    fun `la partida guardada conserva pistas y dia`() {
        val g = Game(Daily.puzzle(5), dailyDay = 5).hint().first
        val loaded = assertNotNull(Game.decode(g.encode()))
        assertEquals(1, loaded.hintsUsed)
        assertEquals(5L, loaded.dailyDay)
    }
}
