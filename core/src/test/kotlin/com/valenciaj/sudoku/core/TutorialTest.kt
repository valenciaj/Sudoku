package com.valenciaj.sudoku.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TutorialTest {
    @Test
    fun `hay ejemplo correcto de cada tecnica`() {
        for (t in Tutorial.lessons) {
            val start = System.currentTimeMillis()
            val e = assertNotNull(Tutorial.example(t, Random(t.ordinal)), "sin ejemplo de $t")
            println("$t: ${System.currentTimeMillis() - start} ms, patrón ${e.pattern.size} casillas")
            assertEquals(t, e.technique)
            assertTrue(e.pattern.isNotEmpty())
            val solution = Solver.solve(e.values)!!
            if (t.places) {
                val (cell, digit) = assertNotNull(e.placement)
                assertEquals(solution[cell], digit)
            } else {
                assertTrue(e.eliminations.isNotEmpty())
                // Nunca se descarta el número correcto.
                for ((cell, mask) in e.eliminations) assertEquals(0, mask and Grid.bit(solution[cell]))
            }
        }
    }
}
