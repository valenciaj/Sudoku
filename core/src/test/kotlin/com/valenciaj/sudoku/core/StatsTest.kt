package com.valenciaj.sudoku.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StatsTest {
    @Test
    fun `registra partidas, victorias, mejor tiempo y media`() {
        val stats = Stats()
            .recordStart(Difficulty.HARD).recordWin(Difficulty.HARD, 300, mistakes = 2)
            .recordStart(Difficulty.HARD).recordWin(Difficulty.HARD, 200, mistakes = 0)
            .recordStart(Difficulty.HARD)
        val hard = stats[Difficulty.HARD]
        assertEquals(3, hard.played)
        assertEquals(2, hard.won)
        assertEquals(1, hard.perfect)
        assertEquals(200, hard.bestSeconds)
        assertEquals(250, hard.averageSeconds)
        assertEquals(66, hard.winRate)
        assertEquals(LevelStats(), stats[Difficulty.EASY])
        assertNull(stats[Difficulty.EASY].averageSeconds)
    }

    @Test
    fun `guardar y cargar`() {
        val stats = Stats().recordStart(Difficulty.EASY).recordWin(Difficulty.EASY, 90, 0)
            .recordStart(Difficulty.DEMENTIAL)
        assertEquals(stats, Stats.decode(stats.encode()))
        assertEquals(Stats(), Stats.decode(null))
        assertEquals(Stats(), Stats.decode("basura"))
    }

    @Test
    fun `sumar estadisticas de dos dispositivos`() {
        val phone = Stats().recordStart(Difficulty.EASY).recordWin(Difficulty.EASY, 200, 0, 0)
            .recordDailyWin(10).recordDailyWin(11)
        val tablet = Stats().recordStart(Difficulty.EASY).recordWin(Difficulty.EASY, 150, 1, 2)
            .recordStart(Difficulty.HARD).recordDailyWin(12)
        val total = phone + tablet
        assertEquals(2, total[Difficulty.EASY].played)
        assertEquals(2, total[Difficulty.EASY].won)
        assertEquals(1, total[Difficulty.EASY].perfect)
        assertEquals(1, total[Difficulty.EASY].noHints)
        assertEquals(150, total[Difficulty.EASY].bestSeconds)
        assertEquals(1, total[Difficulty.HARD].played)
        assertEquals(3, total.daily.completed)
        assertEquals(12L, total.daily.lastDay)
        assertEquals(2, total.daily.bestStreak)
        assertEquals(phone, phone + Stats())
    }
}
