package com.blockpuzzle.rotate.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardTest {

    private fun piece(shape: PieceShape, rotationSteps: Int = 0) =
        Piece(id = "t", shape = shape, color = PieceColor.BLUE, rotationSteps = rotationSteps)

    @Test
    fun `single cell piece can be placed on empty board`() {
        val board = Board()
        assertTrue(board.canPlace(piece(PieceShape.SINGLE), 0, 0))
        assertTrue(board.canPlace(piece(PieceShape.SINGLE), 7, 7))
    }

    @Test
    fun `placement out of bounds is rejected`() {
        val board = Board()
        assertFalse(board.canPlace(piece(PieceShape.TETROMINO_I), 0, 6))
        assertFalse(board.canPlace(piece(PieceShape.SINGLE), 8, 0))
        assertFalse(board.canPlace(piece(PieceShape.SINGLE), -1, 0))
    }

    @Test
    fun `placement onto an occupied cell is rejected`() {
        val board = Board().place(piece(PieceShape.SINGLE), 0, 0).board
        assertFalse(board.canPlace(piece(PieceShape.SINGLE), 0, 0))
        assertTrue(board.canPlace(piece(PieceShape.SINGLE), 0, 1))
    }

    @Test
    fun `filling a full row clears it`() {
        var board = Board()
        for (col in 0 until 7) {
            board = board.place(piece(PieceShape.SINGLE), 0, col).board
        }
        assertFalse(board.isEmpty(0, 0))
        val result = board.place(piece(PieceShape.SINGLE), 0, 7)
        assertEquals(setOf(0), result.clearedRows)
        assertTrue(result.board.isEmpty(0, 0))
        assertTrue(result.board.isEmpty(0, 7))
    }

    @Test
    fun `filling a full column clears it`() {
        var board = Board()
        for (row in 0 until 7) {
            board = board.place(piece(PieceShape.SINGLE), row, 3).board
        }
        val result = board.place(piece(PieceShape.SINGLE), 7, 3)
        assertEquals(setOf(3), result.clearedCols)
        assertTrue((0 until 8).all { result.board.isEmpty(it, 3) })
    }

    @Test
    fun `simultaneous row and column clear counts as combo`() {
        var board = Board()
        // Fill row 0 except (0,0), and column 0 except (0,0).
        for (col in 1 until 8) board = board.place(piece(PieceShape.SINGLE), 0, col).board
        for (row in 1 until 8) board = board.place(piece(PieceShape.SINGLE), row, 0).board

        val result = board.place(piece(PieceShape.SINGLE), 0, 0)
        assertEquals(2, result.linesCleared)
        assertEquals(setOf(0), result.clearedRows)
        assertEquals(setOf(0), result.clearedCols)
        assertEquals(64, result.board.emptyCellCount())
    }

    @Test
    fun `line clear preview matches actual clear`() {
        var board = Board()
        for (col in 0 until 7) board = board.place(piece(PieceShape.SINGLE), 0, col).board
        val (rows, cols) = board.linesClearedByPlacing(piece(PieceShape.SINGLE), 0, 7)
        assertEquals(setOf(0), rows)
        assertTrue(cols.isEmpty())
    }

    @Test
    fun `rotating L clockwise never produces J`() {
        val j = piece(PieceShape.TETROMINO_J)
        val lRotations = (0..3).map { piece(PieceShape.TETROMINO_L, it).cells.toSet() }
        assertFalse(lRotations.contains(j.cells.toSet()))
    }

    @Test
    fun `four rotations return piece to original shape`() {
        val p = piece(PieceShape.PENTOMINO_L)
        var rotated = p
        repeat(4) { rotated = rotated.rotatedClockwise() }
        assertEquals(p.cells.toSet(), rotated.cells.toSet())
    }

    @Test
    fun `board reports no valid placement when only scattered single gaps remain`() {
        var board = Board()
        // Fill every cell except the main diagonal: no row or column is ever
        // fully completed during construction, so nothing auto-clears.
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                if (row == col) continue
                board = board.place(piece(PieceShape.SINGLE), row, col).board
            }
        }
        assertEquals(8, board.emptyCellCount())
        assertTrue(board.hasAnyValidPlacement(piece(PieceShape.SINGLE)))
        assertFalse(board.hasAnyValidPlacement(piece(PieceShape.DOMINO)))
    }
}
