package com.valenciaj.sudoku.core

/** Una pista: qué casilla mirar, qué número va y por qué. */
data class Hint(val cell: Int, val digit: Int, val kind: Kind, val message: String) {
    enum class Kind { WRONG, NAKED_SINGLE, HIDDEN_SINGLE, REVEAL }
}

object Hints {
    private val unitNames = List(9) { "fila" } + List(9) { "columna" } + List(9) { "caja" }

    /** Busca el siguiente paso más sencillo. Null si el sudoku ya está resuelto. */
    fun next(game: Game): Hint? {
        if (game.isSolved) return null
        val solution = game.puzzle.solution

        val wrong = game.values.indices.firstOrNull { game.isWrong(it) }
        if (wrong != null) {
            return Hint(wrong, solution[wrong], Hint.Kind.WRONG, "Esta casilla tiene un número incorrecto.")
        }

        val grid = game.values.toIntArray()
        val cand = IntArray(Grid.CELLS) { if (grid[it] == 0) Solver.candidates(grid, it) else 0 }

        for (cell in 0 until Grid.CELLS) {
            if (grid[cell] == 0 && Integer.bitCount(cand[cell]) == 1) {
                val d = Grid.firstDigit(cand[cell])
                return Hint(
                    cell, d, Hint.Kind.NAKED_SINGLE,
                    "Aquí solo cabe el $d: los demás números ya están en su fila, columna o caja.",
                )
            }
        }
        Grid.units.forEachIndexed { i, unit ->
            for (d in 1..9) {
                val places = unit.filter { cand[it] and Grid.bit(d) != 0 }
                if (places.size == 1) {
                    return Hint(
                        places[0], d, Hint.Kind.HIDDEN_SINGLE,
                        "En esta ${unitNames[i]}, el $d solo puede ir en esta casilla.",
                    )
                }
            }
        }
        val cell = (0 until Grid.CELLS).filter { grid[it] == 0 }.minBy { Integer.bitCount(cand[it]) }
        return Hint(
            cell, solution[cell], Hint.Kind.REVEAL,
            "No hay un paso sencillo ahora mismo. Aquí va el ${solution[cell]}.",
        )
    }
}
