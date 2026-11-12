package com.valenciaj.sudoku.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ModesTest {

    @Test
    fun `killer valido y unico en todos los niveles`() {
        for (level in Difficulty.entries) {
            repeat(3) { seed ->
                val start = System.currentTimeMillis()
                val p = Killer.generate(level, Random(seed * 10 + level.ordinal))
                println("Killer ${level.name}: ${p.cages.size} jaulas, ${p.clueCount} pistas, ${System.currentTimeMillis() - start} ms")
                // Cada casilla en exactamente una jaula.
                assertEquals((0 until Grid.CELLS).toList(), p.cages.flatMap { it.cells.toList() }.sorted())
                for (cage in p.cages) {
                    assertEquals(cage.sum, cage.cells.sumOf { p.solution[it] })
                    assertEquals(cage.cells.size, cage.cells.map { p.solution[it] }.toSet().size)
                }
                assertEquals(1, KillerSolver.countSolutions(p.givens, p.cages, 2))
                for (i in 0 until Grid.CELLS) if (p.givens[i] != 0) assertEquals(p.solution[i], p.givens[i])
            }
        }
    }

    @Test
    fun `contrarreloj penaliza errores y se pierde al acabar el tiempo`() {
        val p = Generator(Random(5)).generate(Difficulty.EASY)
        var g = Game(p, mode = Mode.TIME_ATTACK, timeLimitSeconds = 100)
        val cell = p.givens.indices.first { p.givens[it] == 0 }
        g = g.input(cell, p.solution[cell] % 9 + 1)
        assertEquals(TimeAttack.PENALTY_SECONDS, g.elapsedSeconds)
        assertEquals(70, g.remainingSeconds)
        g = g.tick(80)
        assertTrue(g.isLost)
        assertEquals(100, g.elapsedSeconds)
        assertEquals(g, g.input(cell, p.solution[cell])) // ya no se puede jugar
        assertFalse(Game(p).isLost)
    }

    @Test
    fun `guardar y cargar conserva modo y jaulas`() {
        val p = Killer.generate(Difficulty.MEDIUM, Random(1))
        val g = Game(p, mode = Mode.KILLER)
        val loaded = assertNotNull(Game.decode(g.encode()))
        assertEquals(Mode.KILLER, loaded.mode)
        assertEquals(p.cages, loaded.puzzle.cages)
        val t = Game(Generator(Random(2)).generate(Difficulty.EASY), mode = Mode.TIME_ATTACK, timeLimitSeconds = 300)
        assertEquals(300L, Game.decode(t.encode())?.timeLimitSeconds)
    }

    @Test
    fun `estadisticas por modo y logros`() {
        var s = Stats().recordStart(Difficulty.HARD, Mode.KILLER)
            .recordWin(Difficulty.HARD, 500, 0, 0, Mode.KILLER)
        assertEquals(1, s.mode(Mode.KILLER).won)
        assertEquals(0, s[Difficulty.HARD].won) // el clásico no cambia
        assertTrue(Achievement.KILLER.isUnlocked(s))
        assertFalse(Achievement.TIME_ATTACK.isUnlocked(s))
        s = s.recordStart(Difficulty.EASY, Mode.TIME_ATTACK).recordWin(Difficulty.EASY, 100, 1, 0, Mode.TIME_ATTACK)
        assertTrue(Achievement.TIME_ATTACK.isUnlocked(s))
        assertEquals(s, Stats.decode(s.encode()))
        assertEquals(2, (s + s).mode(Mode.KILLER).won)
    }
}
