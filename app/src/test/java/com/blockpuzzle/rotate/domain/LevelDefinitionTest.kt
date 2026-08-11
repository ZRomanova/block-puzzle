package com.blockpuzzle.rotate.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelDefinitionTest {

    @Test
    fun `a pool of only single-cell shapes is unlosable`() {
        assertTrue(LevelDefinition.isUnlosable(listOf(LevelShape(PieceShape.SINGLE))))
    }

    @Test
    fun `a custom-drawn single-cell shape is just as unlosable as the built-in SINGLE`() {
        val customDot = PieceShape(PieceShape.newCustomId(), listOf(Coordinate(0, 0)))
        assertTrue(LevelDefinition.isUnlosable(listOf(LevelShape(customDot))))
    }

    @Test
    fun `adding any shape bigger than one cell makes the level losable again`() {
        val shapes = listOf(LevelShape(PieceShape.SINGLE), LevelShape(PieceShape.DOMINO))
        assertFalse(LevelDefinition.isUnlosable(shapes))
    }

    @Test
    fun `a pool with no single-cell shapes at all is losable`() {
        val shapes = listOf(LevelShape(PieceShape.TETROMINO_T), LevelShape(PieceShape.PENTOMINO_L))
        assertFalse(LevelDefinition.isUnlosable(shapes))
    }

    @Test
    fun `an empty pool is not flagged as unlosable - that's a separate validation`() {
        assertFalse(LevelDefinition.isUnlosable(emptyList()))
    }
}
