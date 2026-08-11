package com.blockpuzzle.rotate.domain

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EasyPieceGeneratorTest {

    @Test
    fun `generated pieces only come from the configured shape pool`() {
        val pool = listOf(LevelShape(PieceShape.SINGLE), LevelShape(PieceShape.DOMINO), LevelShape(PieceShape.TETROMINO_T))
        val generator = EasyPieceGenerator(shapePool = pool, random = Random(42))
        val board = Board()

        repeat(200) {
            val piece = generator.nextPiece(board, emptyList())
            assertTrue(piece.shape in pool.map { it.shape })
        }
    }

    @Test
    fun `is oblivious to board state - same seed produces same sequence regardless of board fill`() {
        val emptyBoard = Board()
        val piece = Piece(id = "s", shape = PieceShape.SINGLE, color = PieceColor.RED)
        val fullerBoard = emptyBoard.place(piece, 0, 0).board

        val genA = EasyPieceGenerator(random = Random(7))
        val genB = EasyPieceGenerator(random = Random(7))

        val fromEmpty = genA.nextPiece(emptyBoard, emptyList())
        val fromFuller = genB.nextPiece(fullerBoard, emptyList())

        assertEquals(fromEmpty.shape, fromFuller.shape)
    }

    @Test
    fun `with a large sample, every shape in the pool eventually appears`() {
        val generator = EasyPieceGenerator(random = Random(1))
        val board = Board()
        val seenShapes = mutableSetOf<PieceShape>()

        repeat(2000) {
            seenShapes.add(generator.nextPiece(board, emptyList()).shape)
        }

        assertEquals(PieceShape.LEGACY_CATALOG.toSet(), seenShapes)
    }

    @Test
    fun `when rotation is allowed, spawned pieces always start unrotated`() {
        val generator = EasyPieceGenerator(random = Random(3), allowRotation = true)
        val board = Board()
        repeat(50) {
            assertEquals(0, generator.nextPiece(board, emptyList()).rotationSteps)
        }
    }

    @Test
    fun `when rotation is disallowed, spawned pieces start at a varied rotation`() {
        val pool = listOf(LevelShape(PieceShape.TETROMINO_L))
        val generator = EasyPieceGenerator(shapePool = pool, random = Random(3), allowRotation = false)
        val board = Board()
        val seenSteps = mutableSetOf<Int>()
        repeat(50) {
            seenSteps.add(generator.nextPiece(board, emptyList()).rotationSteps)
        }
        assertTrue("expected more than one distinct starting rotation, got $seenSteps", seenSteps.size > 1)
    }
}
