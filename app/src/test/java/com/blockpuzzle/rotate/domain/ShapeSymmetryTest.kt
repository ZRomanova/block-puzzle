package com.blockpuzzle.rotate.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ShapeSymmetryTest {

    @Test
    fun `canonicalKey is invariant across all 4 rotations`() {
        val cells = PieceShape.TETROMINO_L.baseCells
        val canonical = ShapeSymmetry.canonicalKey(cells)
        var current = cells
        repeat(4) {
            assertEquals(canonical, ShapeSymmetry.canonicalKey(current))
            current = ShapeSymmetry.rotate90(current)
        }
    }

    @Test
    fun `a shape and its mirror image do NOT share a canonical key`() {
        // A shape's mirror image is a different shape now — this was an explicit, deliberate
        // reversal (see ShapeSymmetry's doc comment): an earlier version treated a shape and
        // its mirror as "the same shape" and the user asked for that to be fully undone.
        assertNotEquals(
            ShapeSymmetry.canonicalKey(PieceShape.TETROMINO_L.baseCells),
            ShapeSymmetry.canonicalKey(PieceShape.TETROMINO_J.baseCells)
        )
        assertNotEquals(
            ShapeSymmetry.canonicalKey(PieceShape.TETROMINO_S.baseCells),
            ShapeSymmetry.canonicalKey(PieceShape.TETROMINO_Z.baseCells)
        )
    }

    @Test
    fun `every shape in the legacy catalog has a distinct canonical key`() {
        val keys = PieceShape.LEGACY_CATALOG.map { ShapeSymmetry.canonicalKey(it.baseCells) }
        assertEquals(PieceShape.LEGACY_CATALOG.size, keys.toSet().size)
    }

    @Test
    fun `distinct shapes have different canonical keys`() {
        assertNotEquals(
            ShapeSymmetry.canonicalKey(PieceShape.TETROMINO_T.baseCells),
            ShapeSymmetry.canonicalKey(PieceShape.TETROMINO_I.baseCells)
        )
    }
}
