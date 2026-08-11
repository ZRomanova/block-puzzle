package com.blockpuzzle.rotate.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HardModePieceSelectorTest {

    @Test
    fun `evaluate on empty board covers every cell for a single-cell candidate`() {
        val selector = HardModePieceSelector()
        val board = Board()
        val candidate = Piece(id = "c", shape = PieceShape.SINGLE, color = PieceColor.RED)

        val evaluation = selector.evaluate(board, remainingTrayPieces = emptyList(), candidate = candidate)

        assertEquals(64, evaluation.totalValidPlacements)
        assertEquals(0, evaluation.uncoveredCells)
    }

    @Test
    fun `a longer piece has fewer valid placements than a single cell on an empty board`() {
        val selector = HardModePieceSelector()
        val board = Board()
        val single = Piece(id = "s", shape = PieceShape.SINGLE, color = PieceColor.RED)
        val line = Piece(id = "l", shape = PieceShape.TETROMINO_I, color = PieceColor.BLUE)

        val singleEval = selector.evaluate(board, emptyList(), single)
        val lineEval = selector.evaluate(board, emptyList(), line)

        assertTrue(lineEval.totalValidPlacements < singleEval.totalValidPlacements)
    }

    @Test
    fun `selector prefers the harder candidate between two very different shapes`() {
        val selector = HardModePieceSelector(
            shapePool = listOf(LevelShape(PieceShape.SINGLE), LevelShape(PieceShape.TETROMINO_I)),
            candidateSampleSize = 2,
            minPlayableMoves = 1
        )
        val board = Board()

        val chosen = selector.nextPiece(board, remainingTrayPieces = emptyList())

        assertEquals(PieceShape.TETROMINO_I, chosen.shape)
    }

    @Test
    fun `selector never returns null and does not crash when every candidate is nearly unplayable`() {
        var board = Board()
        val single = Piece(id = "s", shape = PieceShape.SINGLE, color = PieceColor.RED)
        // Fill everything except a lone diagonal of single-cell gaps.
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                if (row == col) continue
                board = board.place(single, row, col).board
            }
        }

        val selector = HardModePieceSelector(
            shapePool = listOf(LevelShape(PieceShape.SINGLE), LevelShape(PieceShape.DOMINO), LevelShape(PieceShape.TETROMINO_I)),
            candidateSampleSize = 3,
            minPlayableMoves = 5
        )

        val chosen = selector.nextPiece(board, remainingTrayPieces = emptyList())
        assertNotNull(chosen)
    }

    @Test
    fun `selector avoids an instant game-over candidate when a playable alternative exists`() {
        var board = Board()
        val single = Piece(id = "s", shape = PieceShape.SINGLE, color = PieceColor.RED)
        // Same diagonal-gaps board: SINGLE still has 8 placements, DOMINO has zero.
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                if (row == col) continue
                board = board.place(single, row, col).board
            }
        }

        val selector = HardModePieceSelector(
            shapePool = listOf(LevelShape(PieceShape.SINGLE), LevelShape(PieceShape.DOMINO)),
            candidateSampleSize = 2,
            minPlayableMoves = 1
        )

        val chosen = selector.nextPiece(board, remainingTrayPieces = emptyList())
        assertEquals(PieceShape.SINGLE, chosen.shape)
    }

    @Test
    fun `when rotation is disallowed, chosen pieces start at a varied rotation`() {
        val selector = HardModePieceSelector(
            shapePool = listOf(LevelShape(PieceShape.TETROMINO_L)),
            candidateSampleSize = 1,
            minPlayableMoves = 0,
            allowRotation = false
        )
        val board = Board()
        val seenSteps = mutableSetOf<Int>()
        repeat(50) {
            seenSteps.add(selector.nextPiece(board, emptyList()).rotationSteps)
        }
        assertTrue("expected more than one distinct starting rotation, got $seenSteps", seenSteps.size > 1)
    }
}
