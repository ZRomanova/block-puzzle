package com.blockpuzzle.rotate.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShapeSymmetryTest {

    @Test
    fun `canonicalKey is invariant across all 8 dihedral transforms`() {
        val cells = PieceShape.TETROMINO_L.baseCells
        val canonical = ShapeSymmetry.canonicalKey(cells)
        for (transform in ShapeSymmetry.allTransforms(cells)) {
            assertEquals(canonical, ShapeSymmetry.canonicalKey(transform))
        }
    }

    @Test
    fun `S and Z tetrominoes share a canonical key (mirror images of each other)`() {
        assertEquals(
            ShapeSymmetry.canonicalKey(PieceShape.TETROMINO_S.baseCells),
            ShapeSymmetry.canonicalKey(PieceShape.TETROMINO_Z.baseCells)
        )
    }

    @Test
    fun `L and J tetrominoes share a canonical key (mirror images of each other)`() {
        assertEquals(
            ShapeSymmetry.canonicalKey(PieceShape.TETROMINO_L.baseCells),
            ShapeSymmetry.canonicalKey(PieceShape.TETROMINO_J.baseCells)
        )
    }

    @Test
    fun `S tetromino is chiral despite being symmetric under 180 degree rotation`() {
        val cells = PieceShape.TETROMINO_S.baseCells
        val rotated180 = ShapeSymmetry.rotate90(ShapeSymmetry.rotate90(cells))
        assertEquals(ShapeSymmetry.normalize(cells).toSet(), rotated180.toSet())
        assertTrue(ShapeSymmetry.isChiral(cells))
    }

    @Test
    fun `L and J tetrominoes are each chiral`() {
        assertTrue(ShapeSymmetry.isChiral(PieceShape.TETROMINO_L.baseCells))
        assertTrue(ShapeSymmetry.isChiral(PieceShape.TETROMINO_J.baseCells))
    }

    @Test
    fun `single, domino and reflection-symmetric shapes are achiral`() {
        assertFalse(ShapeSymmetry.isChiral(PieceShape.SINGLE.baseCells))
        assertFalse(ShapeSymmetry.isChiral(PieceShape.DOMINO.baseCells))
        assertFalse(ShapeSymmetry.isChiral(PieceShape.TETROMINO_O.baseCells))
        assertFalse(ShapeSymmetry.isChiral(PieceShape.TETROMINO_T.baseCells))
        assertFalse(ShapeSymmetry.isChiral(PieceShape.TETROMINO_I.baseCells))
        assertFalse(ShapeSymmetry.isChiral(PieceShape.PENTOMINO_PLUS.baseCells))
        assertFalse(ShapeSymmetry.isChiral(PieceShape.PENTOMINO_T.baseCells))
        assertFalse(ShapeSymmetry.isChiral(PieceShape.PENTOMINO_I.baseCells))
    }

    @Test
    fun `the corner triomino is achiral even though it looks asymmetric`() {
        // Its mirror image equals one of its own 90-degree rotations.
        assertFalse(ShapeSymmetry.isChiral(PieceShape.TRIOMINO_L.baseCells))
    }

    @Test
    fun `tetromino L has exactly one mirror entry (J) in the legacy catalog`() {
        val key = ShapeSymmetry.canonicalKey(PieceShape.TETROMINO_L.baseCells)
        val matching = PieceShape.LEGACY_CATALOG.count { ShapeSymmetry.canonicalKey(it.baseCells) == key }
        assertEquals(2, matching)
    }

    @Test
    fun `pentomino L is chiral but has no separate mirror entry in the legacy catalog`() {
        // This is exactly why LevelShape.includeMirror must be stored explicitly for migrated
        // legacy shapes rather than derived from isChiral() at spawn time (see LevelShape.kt).
        assertTrue(ShapeSymmetry.isChiral(PieceShape.PENTOMINO_L.baseCells))
        val key = ShapeSymmetry.canonicalKey(PieceShape.PENTOMINO_L.baseCells)
        val matching = PieceShape.LEGACY_CATALOG.count { ShapeSymmetry.canonicalKey(it.baseCells) == key }
        assertEquals(1, matching)
    }

    @Test
    fun `distinct shapes have different canonical keys`() {
        assertNotEquals(
            ShapeSymmetry.canonicalKey(PieceShape.TETROMINO_T.baseCells),
            ShapeSymmetry.canonicalKey(PieceShape.TETROMINO_I.baseCells)
        )
    }
}
