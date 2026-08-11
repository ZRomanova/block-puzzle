package com.blockpuzzle.rotate.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PieceTest {

    @Test
    fun `flipping a chiral piece changes its cells`() {
        val piece = Piece(id = "p", shape = PieceShape.TETROMINO_L, color = PieceColor.RED)
        val flipped = piece.flippedHorizontally()
        assertNotEquals(piece.cells.toSet(), flipped.cells.toSet())
    }

    @Test
    fun `flipping twice returns to the original cells`() {
        val piece = Piece(id = "p", shape = PieceShape.TETROMINO_L, color = PieceColor.RED)
        val flippedTwice = piece.flippedHorizontally().flippedHorizontally()
        assertEquals(piece.cells.toSet(), flippedTwice.cells.toSet())
    }

    @Test
    fun `flipping resets rotation to 0 but the resulting cells still reflect the prior rotation`() {
        val piece = Piece(id = "p", shape = PieceShape.TETROMINO_L, color = PieceColor.RED, rotationSteps = 1)
        val flipped = piece.flippedHorizontally()
        assertEquals(0, flipped.rotationSteps)
        assertEquals(ShapeSymmetry.mirror(piece.cells).toSet(), flipped.cells.toSet())
    }

    @Test
    fun `flipping an achiral piece still changes rotationSteps bookkeeping but cell count is unchanged`() {
        val piece = Piece(id = "p", shape = PieceShape.TETROMINO_O, color = PieceColor.RED)
        val flipped = piece.flippedHorizontally()
        assertEquals(piece.cellCount, flipped.cellCount)
    }
}
