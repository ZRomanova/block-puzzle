package com.blockpuzzle.rotate.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShapeConnectivityTest {

    @Test
    fun `single cell is connected`() {
        assertTrue(ShapeConnectivity.isConnected(setOf(Coordinate(0, 0))))
    }

    @Test
    fun `empty set is not connected`() {
        assertFalse(ShapeConnectivity.isConnected(emptySet()))
    }

    @Test
    fun `edge-adjacent cells are connected`() {
        val cells = setOf(Coordinate(0, 0), Coordinate(0, 1), Coordinate(1, 1))
        assertTrue(ShapeConnectivity.isConnected(cells))
    }

    @Test
    fun `diagonal-only touching cells are connected`() {
        val cells = setOf(Coordinate(0, 0), Coordinate(1, 1), Coordinate(2, 2))
        assertTrue(ShapeConnectivity.isConnected(cells))
    }

    @Test
    fun `disjoint clusters are not connected`() {
        val cells = setOf(Coordinate(0, 0), Coordinate(0, 1), Coordinate(5, 5), Coordinate(5, 6))
        assertFalse(ShapeConnectivity.isConnected(cells))
    }

    @Test
    fun `all legacy shapes are connected`() {
        for (shape in PieceShape.LEGACY_CATALOG) {
            assertTrue("${shape.id} should be connected", ShapeConnectivity.isConnected(shape.baseCells.toSet()))
        }
    }
}
