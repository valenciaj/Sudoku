package com.valenciaj.sudoku.core

import kotlin.random.Random

/**
 * Sudoku Killer: se reparte el tablero en jaulas (grupos de casillas contiguas sin números
 * repetidos) y se muestra la suma de cada una. Se dan las mínimas pistas necesarias para
 * que la solución sea única.
 *
 * Dificultad: cuanto más difícil, jaulas más grandes y menos pistas.
 */
object Killer {

    private fun maxCageSize(level: Difficulty) = when (level) {
        Difficulty.EASY -> 2
        Difficulty.MEDIUM -> 3
        Difficulty.HARD -> 4
        Difficulty.DEMENTIAL -> 5
    }

    /** Pistas que se dejan como mínimo (en los niveles fáciles, de regalo). */
    private fun minGivens(level: Difficulty) = when (level) {
        Difficulty.EASY -> 30
        Difficulty.MEDIUM -> 16
        Difficulty.HARD -> 6
        Difficulty.DEMENTIAL -> 0
    }

    fun generate(level: Difficulty, random: Random = Random.Default): Puzzle {
        val solution = Solver.solve(IntArray(Grid.CELLS), random)!!
        val cages = makeCages(solution, maxCageSize(level), random)
        // Se parte del tablero lleno y se quitan pistas mientras la solución siga siendo única.
        val givens = solution.copyOf()
        var count = Grid.CELLS
        for (cell in (0 until Grid.CELLS).shuffled(random)) {
            if (count <= minGivens(level)) break
            givens[cell] = 0
            // Si la comprobación es demasiado larga, se deja la pista (por seguridad y rapidez).
            if (KillerSolver.countSolutions(givens, cages, 2, maxNodes = 20_000) == 1) count-- else givens[cell] = solution[cell]
        }
        return Puzzle(givens, solution, level, cages)
    }

    /** Reparte las casillas en jaulas contiguas, sin repetir dígito, de tamaño 1..[maxSize]. */
    fun makeCages(solution: IntArray, maxSize: Int, random: Random): List<Cage> {
        val owner = IntArray(Grid.CELLS) { -1 }
        val cages = mutableListOf<IntArray>()
        for (start in (0 until Grid.CELLS).shuffled(random)) {
            if (owner[start] != -1) continue
            val target = 1 + random.nextInt(maxSize) + if (maxSize > 2 && random.nextBoolean()) 1 else 0
            val cells = mutableListOf(start)
            owner[start] = cages.size
            while (cells.size < minOf(target, maxSize)) {
                val used = cells.map { solution[it] }.toSet()
                val options = cells.flatMap(::neighbours)
                    .filter { owner[it] == -1 && solution[it] !in used }
                    .distinct()
                if (options.isEmpty()) break
                val next = options[random.nextInt(options.size)]
                owner[next] = cages.size
                cells += next
            }
            cages += cells.sorted().toIntArray()
        }
        // Las jaulas de 1 casilla regalan el número: se unen a una vecina si se puede.
        return mergeSingles(cages, owner, solution, maxSize).map { cells ->
            Cage(cells, cells.sumOf { solution[it] })
        }
    }

    private fun mergeSingles(
        cages: MutableList<IntArray>,
        owner: IntArray,
        solution: IntArray,
        maxSize: Int,
    ): List<IntArray> {
        val alive = cages.map { it.toMutableList() }.toMutableList()
        for (i in alive.indices) {
            val single = alive[i]
            if (single.size != 1) continue
            val cell = single[0]
            val target = neighbours(cell).map { owner[it] }.distinct().firstOrNull { j ->
                j != i && alive[j].isNotEmpty() && alive[j].size < maxSize.coerceAtLeast(2) &&
                    alive[j].none { solution[it] == solution[cell] }
            } ?: continue
            alive[target].add(cell)
            owner[cell] = target
            single.clear()
        }
        return alive.filter { it.isNotEmpty() }.map { it.sorted().toIntArray() }
    }

    private fun neighbours(cell: Int): List<Int> {
        val r = Grid.row(cell)
        val c = Grid.col(cell)
        return buildList {
            if (r > 0) add(cell - 9)
            if (r < 8) add(cell + 9)
            if (c > 0) add(cell - 1)
            if (c < 8) add(cell + 1)
        }
    }
}

/** Resolución por fuerza bruta respetando también las jaulas (suma y sin repetir). */
object KillerSolver {

    /**
     * Cuenta soluciones hasta [limit]. Si se exploran más de [maxNodes] nodos, se rinde
     * y devuelve -1 (desconocido).
     */
    fun countSolutions(givens: IntArray, cages: List<Cage>, limit: Int = 2, maxNodes: Int = Int.MAX_VALUE): Int {
        val cageOf = IntArray(Grid.CELLS) { -1 }
        cages.forEachIndexed { i, cage -> cage.cells.forEach { cageOf[it] = i } }
        val grid = givens.copyOf()
        val counter = intArrayOf(0, maxNodes)
        search(grid, cages, cageOf, counter, limit)
        return if (counter[1] < 0) -1 else counter[0]
    }

    /** counter[0] = soluciones encontradas; counter[1] = nodos que quedan. */
    private fun search(grid: IntArray, cages: List<Cage>, cageOf: IntArray, counter: IntArray, limit: Int): Boolean {
        if (--counter[1] < 0) return true
        var best = -1
        var bestMask = 0
        var bestCount = 10
        for (cell in 0 until Grid.CELLS) {
            if (grid[cell] != 0) continue
            val mask = candidates(grid, cell, cages, cageOf)
            val n = Integer.bitCount(mask)
            if (n == 0) return false
            if (n < bestCount) {
                best = cell; bestMask = mask; bestCount = n
                if (n == 1) break
            }
        }
        if (best == -1) {
            counter[0]++
            return counter[0] >= limit
        }
        for (d in 1..9) {
            if (bestMask and Grid.bit(d) == 0) continue
            grid[best] = d
            if (search(grid, cages, cageOf, counter, limit)) return true
        }
        grid[best] = 0
        return false
    }

    private fun candidates(grid: IntArray, cell: Int, cages: List<Cage>, cageOf: IntArray): Int {
        var mask = Solver.candidates(grid, cell)
        val ci = cageOf[cell]
        if (ci < 0) return mask
        val cage = cages[ci]
        var used = 0
        var sum = 0
        var empty = 0
        for (c in cage.cells) {
            val v = grid[c]
            if (v == 0) empty++ else { used = used or Grid.bit(v); sum += v }
        }
        mask = mask and used.inv()
        val remaining = cage.sum - sum
        // Tras poner d quedan (empty - 1) casillas que deben sumar remaining - d con dígitos distintos libres.
        var result = 0
        for (d in 1..9) {
            if (mask and Grid.bit(d) == 0) continue
            val rest = remaining - d
            val k = empty - 1
            val free = (1..9).filter { it != d && used and Grid.bit(it) == 0 }
            if (k == 0) {
                if (rest == 0) result = result or Grid.bit(d)
            } else if (free.size >= k && rest >= free.take(k).sum() && rest <= free.takeLast(k).sum()) {
                result = result or Grid.bit(d)
            }
        }
        return result
    }
}
