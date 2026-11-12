package com.valenciaj.sudoku.core

import kotlin.random.Random

/** Resolución por fuerza bruta (backtracking), usada para validar unicidad y crear soluciones. */
object Solver {

    /** Cuenta soluciones hasta [limit] (con limit = 2 basta para saber si es única). */
    fun countSolutions(puzzle: IntArray, limit: Int = 2): Int {
        val grid = puzzle.copyOf()
        val counter = intArrayOf(0)
        search(grid, null, counter, limit)
        return counter[0]
    }

    fun hasUniqueSolution(puzzle: IntArray) = countSolutions(puzzle, 2) == 1

    /** Devuelve una solución, o null si no hay. Con [random] explora en orden aleatorio. */
    fun solve(puzzle: IntArray, random: Random? = null): IntArray? {
        val grid = puzzle.copyOf()
        val counter = intArrayOf(0)
        return if (search(grid, random, counter, 1)) grid else null
    }

    /** Devuelve true si se alcanzó el límite (y deja la última solución en [grid]). */
    private fun search(grid: IntArray, random: Random?, counter: IntArray, limit: Int): Boolean {
        // Elegir la celda vacía con menos candidatos (MRV).
        var bestCell = -1
        var bestMask = 0
        var bestCount = 10
        for (cell in 0 until Grid.CELLS) {
            if (grid[cell] != 0) continue
            val mask = candidates(grid, cell)
            val count = Integer.bitCount(mask)
            if (count == 0) return false
            if (count < bestCount) {
                bestCell = cell; bestMask = mask; bestCount = count
                if (count == 1) break
            }
        }
        if (bestCell == -1) {
            counter[0]++
            return counter[0] >= limit
        }
        val digits = (1..9).filter { bestMask and Grid.bit(it) != 0 }
            .let { if (random != null) it.shuffled(random) else it }
        for (d in digits) {
            grid[bestCell] = d
            if (search(grid, random, counter, limit)) return true
        }
        grid[bestCell] = 0
        return false
    }

    fun candidates(grid: IntArray, cell: Int): Int {
        var used = 0
        for (p in Grid.peers[cell]) {
            val v = grid[p]
            if (v != 0) used = used or Grid.bit(v)
        }
        return Grid.ALL_CANDIDATES and used.inv()
    }
}
