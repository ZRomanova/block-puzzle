package com.blockpuzzle.rotate.data

import com.blockpuzzle.rotate.domain.PieceShape
import com.blockpuzzle.rotate.domain.ShapeSymmetry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultLevelsTest {

    @Test
    fun `no two entries in the classic shape pool share a canonical key`() {
        val pool = classicShapePool()
        val keys = pool.map { ShapeSymmetry.canonicalKey(it.shape.baseCells) }
        assertEquals("expected no duplicate mirror-image shapes, got $pool", keys.size, keys.toSet().size)
    }

    @Test
    fun `mirror pairs from the legacy catalog collapse to one entry each, flagged as mirrorable`() {
        val pool = classicShapePool()
        val lKey = ShapeSymmetry.canonicalKey(PieceShape.TETROMINO_L.baseCells)
        val sKey = ShapeSymmetry.canonicalKey(PieceShape.TETROMINO_S.baseCells)

        val lEntries = pool.filter { ShapeSymmetry.canonicalKey(it.shape.baseCells) == lKey }
        val sEntries = pool.filter { ShapeSymmetry.canonicalKey(it.shape.baseCells) == sKey }

        assertEquals(1, lEntries.size)
        assertEquals(1, sEntries.size)
        assertTrue(lEntries.single().includeMirror)
        assertTrue(sEntries.single().includeMirror)
    }

    @Test
    fun `the pool has fewer entries than the full legacy catalog, one less per merged pair`() {
        val pool = classicShapePool()
        // TETROMINO_L/J and TETROMINO_S/Z are the only mirror pairs in the legacy catalog.
        assertEquals(PieceShape.LEGACY_CATALOG.size - 2, pool.size)
    }
}
