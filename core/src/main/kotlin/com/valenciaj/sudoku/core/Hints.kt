package com.valenciaj.sudoku.core

/**
 * Una pista: qué casilla mirar, qué número va y por qué.
 * El texto lo pone la app (según el idioma) a partir de [kind] y [unit].
 */
data class Hint(val cell: Int, val digit: Int, val kind: Kind, val unit: UnitKind? = null) {
    enum class Kind { WRONG, NAKED_SINGLE, HIDDEN_SINGLE, REVEAL }
    enum class UnitKind { ROW, COLUMN, BOX }
}

object Hints {
    private val unitKinds = List(9) { Hint.UnitKind.ROW } + List(9) { Hint.UnitKind.COLUMN } + List(9) { Hint.UnitKind.BOX }

    /** Busca el siguiente paso más sencillo. Null si el sudoku ya está resuelto. */
    fun next(game: Game): Hint? {
        if (game.isSolved) return null
        val solution = game.puzzle.solution

        val wrong = game.values.indices.firstOrNull { game.isWrong(it) }
        if (wrong != null) return Hint(wrong, solution[wrong], Hint.Kind.WRONG)

        val grid = game.values.toIntArray()
        val cand = IntArray(Grid.CELLS) { if (grid[it] == 0) Solver.candidates(grid, it) else 0 }

        for (cell in 0 until Grid.CELLS) {
            if (grid[cell] == 0 && Integer.bitCount(cand[cell]) == 1) {
                return Hint(cell, Grid.firstDigit(cand[cell]), Hint.Kind.NAKED_SINGLE)
            }
        }
        Grid.units.forEachIndexed { i, unit ->
            for (d in 1..9) {
                val places = unit.filter { cand[it] and Grid.bit(d) != 0 }
                if (places.size == 1) return Hint(places[0], d, Hint.Kind.HIDDEN_SINGLE, unitKinds[i])
            }
        }
        val cell = (0 until Grid.CELLS).filter { grid[it] == 0 }.minBy { Integer.bitCount(cand[it]) }
        return Hint(cell, solution[cell], Hint.Kind.REVEAL)
    }
}
