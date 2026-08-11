package com.blockpuzzle.rotate.domain

/** Result of placing a piece on the board: the resulting board plus what got cleared. */
data class PlacementResult(
    val board: Board,
    val cellsPlaced: Int,
    val clearedRows: Set<Int>,
    val clearedCols: Set<Int>,
    /** The 8 cell colors of each cleared row/column, captured just before it was wiped — used for color-bonus scoring. */
    val clearedLineColors: List<List<PieceColor>> = emptyList()
) {
    val linesCleared: Int get() = clearedRows.size + clearedCols.size
}

/** Immutable 8x8 (by default) game board. Each cell is either empty (null) or holds a [PieceColor]. */
data class Board(
    val size: Int = 8,
    private val grid: List<List<PieceColor?>> = List(size) { List(size) { null } }
) {
    fun cellAt(row: Int, col: Int): PieceColor? = grid[row][col]

    fun isEmpty(row: Int, col: Int): Boolean = grid[row][col] == null

    fun isInBounds(row: Int, col: Int): Boolean = row in 0 until size && col in 0 until size

    /** Absolute board cells the piece would occupy if its top-left aligns at (anchorRow, anchorCol). */
    private fun absoluteCells(piece: Piece, anchorRow: Int, anchorCol: Int): List<Coordinate> =
        piece.cells.map { Coordinate(it.row + anchorRow, it.col + anchorCol) }

    fun canPlace(piece: Piece, anchorRow: Int, anchorCol: Int): Boolean {
        val cells = absoluteCells(piece, anchorRow, anchorCol)
        return cells.all { isInBounds(it.row, it.col) && isEmpty(it.row, it.col) }
    }

    /** All anchor positions where [piece] can legally be placed. */
    fun validPlacements(piece: Piece): List<Coordinate> {
        val result = mutableListOf<Coordinate>()
        for (row in 0 until size) {
            for (col in 0 until size) {
                if (canPlace(piece, row, col)) result.add(Coordinate(row, col))
            }
        }
        return result
    }

    fun hasAnyValidPlacement(piece: Piece): Boolean {
        for (row in 0 until size) {
            for (col in 0 until size) {
                if (canPlace(piece, row, col)) return true
            }
        }
        return false
    }

    /** Rows/cols that would become fully occupied if [piece] were placed at the given anchor, without mutating anything. */
    fun linesClearedByPlacing(piece: Piece, anchorRow: Int, anchorCol: Int): Pair<Set<Int>, Set<Int>> {
        if (!canPlace(piece, anchorRow, anchorCol)) return emptySet<Int>() to emptySet()
        val occupied = absoluteCells(piece, anchorRow, anchorCol).toSet()
        val rows = (0 until size).filter { r -> (0 until size).all { c -> occupied.contains(Coordinate(r, c)) || !isEmpty(r, c) } }.toSet()
        val cols = (0 until size).filter { c -> (0 until size).all { r -> occupied.contains(Coordinate(r, c)) || !isEmpty(r, c) } }.toSet()
        return rows to cols
    }

    /** Places [piece] at the anchor and clears any fully-filled rows/columns. Requires [canPlace] to be true. */
    fun place(piece: Piece, anchorRow: Int, anchorCol: Int): PlacementResult {
        require(canPlace(piece, anchorRow, anchorCol)) { "Illegal placement" }
        val mutable = grid.map { it.toMutableList() }.toMutableList()
        for (cell in absoluteCells(piece, anchorRow, anchorCol)) {
            mutable[cell.row][cell.col] = piece.color
        }
        val (clearedRows, clearedCols) = linesFullyOccupied(mutable)
        val clearedLineColors = mutableListOf<List<PieceColor>>()
        for (r in clearedRows) clearedLineColors.add((0 until size).map { c -> mutable[r][c]!! })
        for (c in clearedCols) clearedLineColors.add((0 until size).map { r -> mutable[r][c]!! })
        for (r in clearedRows) for (c in 0 until size) mutable[r][c] = null
        for (c in clearedCols) for (r in 0 until size) mutable[r][c] = null
        return PlacementResult(
            board = Board(size, mutable),
            cellsPlaced = piece.cellCount,
            clearedRows = clearedRows,
            clearedCols = clearedCols,
            clearedLineColors = clearedLineColors
        )
    }

    private fun linesFullyOccupied(g: List<List<PieceColor?>>): Pair<Set<Int>, Set<Int>> {
        val rows = (0 until size).filter { r -> (0 until size).all { c -> g[r][c] != null } }.toSet()
        val cols = (0 until size).filter { c -> (0 until size).all { r -> g[r][c] != null } }.toSet()
        return rows to cols
    }

    /** Number of empty cells left on the board. */
    fun emptyCellCount(): Int = grid.sumOf { row -> row.count { it == null } }
}
