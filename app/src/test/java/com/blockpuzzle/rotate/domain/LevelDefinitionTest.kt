package com.blockpuzzle.rotate.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelDefinitionTest {

    @Test
    fun `a pool of only single-cell shapes is unlosable on every board size`() {
        val single = listOf(LevelShape(PieceShape.SINGLE))
        for (size in LevelDefinition.ALLOWED_BOARD_SIZES) {
            assertTrue("size $size", LevelDefinition.isUnlosable(single, size))
        }
    }

    @Test
    fun `a custom-drawn single-cell shape is just as unlosable as the built-in SINGLE`() {
        val customDot = PieceShape(PieceShape.newCustomId(), listOf(Coordinate(0, 0)))
        assertTrue(LevelDefinition.isUnlosable(listOf(LevelShape(customDot)), boardSize = 8))
    }

    @Test
    fun `adding any shape bigger than one cell makes a single-cell pool losable again`() {
        val shapes = listOf(LevelShape(PieceShape.SINGLE), LevelShape(PieceShape.DOMINO))
        assertFalse(LevelDefinition.isUnlosable(shapes, boardSize = 8))
    }

    @Test
    fun `a domino-only pool is unlosable on even board sizes`() {
        val dominoOnly = listOf(LevelShape(PieceShape.DOMINO))
        assertTrue(LevelDefinition.isUnlosable(dominoOnly, boardSize = 6))
        assertTrue(LevelDefinition.isUnlosable(dominoOnly, boardSize = 8))
    }

    @Test
    fun `a domino-only pool is NOT flagged unlosable on odd board sizes`() {
        val dominoOnly = listOf(LevelShape(PieceShape.DOMINO))
        assertFalse(LevelDefinition.isUnlosable(dominoOnly, boardSize = 5))
        assertFalse(LevelDefinition.isUnlosable(dominoOnly, boardSize = 7))
    }

    @Test
    fun `a custom-drawn 2-cell shape counts the same as the built-in DOMINO`() {
        val customDomino = PieceShape(PieceShape.newCustomId(), listOf(Coordinate(0, 0), Coordinate(1, 0)))
        assertTrue(LevelDefinition.isUnlosable(listOf(LevelShape(customDomino)), boardSize = 8))
    }

    @Test
    fun `a pool with no single-cell or domino-only shapes is losable`() {
        val shapes = listOf(LevelShape(PieceShape.TETROMINO_T), LevelShape(PieceShape.PENTOMINO_L))
        assertFalse(LevelDefinition.isUnlosable(shapes, boardSize = 8))
    }

    @Test
    fun `an empty pool is not flagged as unlosable - that's a separate validation`() {
        assertFalse(LevelDefinition.isUnlosable(emptyList(), boardSize = 8))
    }
}
