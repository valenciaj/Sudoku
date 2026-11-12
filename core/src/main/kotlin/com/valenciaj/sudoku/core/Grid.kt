package com.valenciaj.sudoku.core

/**
 * Utilidades de geometría del tablero. Las celdas se indexan 0..80 (fila * 9 + columna);
 * 0 significa celda vacía y 1..9 son los dígitos.
 */
object Grid {
    const val SIZE = 9
    const val CELLS = 81
    const val ALL_CANDIDATES = 0x1FF

    fun row(cell: Int) = cell / SIZE
    fun col(cell: Int) = cell % SIZE
    fun box(cell: Int) = (row(cell) / 3) * 3 + col(cell) / 3

    /** 27 unidades: 9 filas, 9 columnas y 9 cajas. */
    val rows: List<IntArray> = List(SIZE) { r -> IntArray(SIZE) { c -> r * SIZE + c } }
    val cols: List<IntArray> = List(SIZE) { c -> IntArray(SIZE) { r -> r * SIZE + c } }
    val boxes: List<IntArray> = List(SIZE) { b ->
        val r0 = (b / 3) * 3
        val c0 = (b % 3) * 3
        IntArray(SIZE) { i -> (r0 + i / 3) * SIZE + c0 + i % 3 }
    }
    val units: List<IntArray> = rows + cols + boxes

    /** Para cada celda, las 20 celdas que comparten fila, columna o caja. */
    val peers: Array<IntArray> = Array(CELLS) { cell ->
        (0 until CELLS).filter { other ->
            other != cell && (row(other) == row(cell) || col(other) == col(cell) || box(other) == box(cell))
        }.toIntArray()
    }

    fun bit(digit: Int) = 1 shl (digit - 1)

    fun firstDigit(mask: Int) = Integer.numberOfTrailingZeros(mask) + 1
}
