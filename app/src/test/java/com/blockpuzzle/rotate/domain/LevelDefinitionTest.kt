package com.blockpuzzle.rotate.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelDefinitionTest {

    @Test
    fun `a pool of only single-cell shapes is unlosable on every board size, rotation on or off`() {
        val single = listOf(LevelShape(PieceShape.SINGLE))
        for (size in LevelDefinition.ALLOWED_BOARD_SIZES) {
            assertTrue("size $size, rotation on", LevelDefinition.isUnlosable(single, size, allowRotation = true))
            assertTrue("size $size, rotation off", LevelDefinition.isUnlosable(single, size, allowRotation = false))
        }
    }

    @Test
    fun `a custom-drawn single-cell shape is just as unlosable as the built-in SINGLE`() {
        val customDot = PieceShape(PieceShape.newCustomId(), listOf(Coordinate(0, 0)))
        assertTrue(LevelDefinition.isUnlosable(listOf(LevelShape(customDot)), boardSize = 8, allowRotation = true))
    }

    @Test
    fun `adding any shape bigger than one cell makes a single-cell pool losable again`() {
        val shapes = listOf(LevelShape(PieceShape.SINGLE), LevelShape(PieceShape.DOMINO))
        assertFalse(LevelDefinition.isUnlosable(shapes, boardSize = 8, allowRotation = true))
    }

    @Test
    fun `a domino-only pool is unlosable on even board sizes, when rotation is allowed`() {
        val dominoOnly = listOf(LevelShape(PieceShape.DOMINO))
        assertTrue(LevelDefinition.isUnlosable(dominoOnly, boardSize = 6, allowRotation = true))
        assertTrue(LevelDefinition.isUnlosable(dominoOnly, boardSize = 8, allowRotation = true))
    }

    @Test
    fun `a domino-only pool is NOT flagged unlosable on odd board sizes`() {
        val dominoOnly = listOf(LevelShape(PieceShape.DOMINO))
        assertFalse(LevelDefinition.isUnlosable(dominoOnly, boardSize = 5, allowRotation = true))
        assertFalse(LevelDefinition.isUnlosable(dominoOnly, boardSize = 7, allowRotation = true))
    }

    @Test
    fun `a domino-only pool is NOT flagged unlosable when rotation is off, even on an even board`() {
        // Without rotation, a domino is stuck in whichever single orientation it was drawn in
        // (mirroring a straight 2-cell piece doesn't change its orientation), so a board full of
        // only cross-oriented adjacent gaps would block it despite the checkerboard invariant.
        val dominoOnly = listOf(LevelShape(PieceShape.DOMINO))
        assertFalse(LevelDefinition.isUnlosable(dominoOnly, boardSize = 6, allowRotation = false))
        assertFalse(LevelDefinition.isUnlosable(dominoOnly, boardSize = 8, allowRotation = false))
    }

    @Test
    fun `a custom-drawn 2-cell shape counts the same as the built-in DOMINO`() {
        val customDomino = PieceShape(PieceShape.newCustomId(), listOf(Coordinate(0, 0), Coordinate(1, 0)))
        assertTrue(LevelDefinition.isUnlosable(listOf(LevelShape(customDomino)), boardSize = 8, allowRotation = true))
    }

    @Test
    fun `a pool with no single-cell or domino-only shapes is losable`() {
        val shapes = listOf(LevelShape(PieceShape.TETROMINO_T), LevelShape(PieceShape.PENTOMINO_L))
        assertFalse(LevelDefinition.isUnlosable(shapes, boardSize = 8, allowRotation = true))
    }

    @Test
    fun `an empty pool is not flagged as unlosable - that's a separate validation`() {
        assertFalse(LevelDefinition.isUnlosable(emptyList(), boardSize = 8, allowRotation = true))
    }
}
