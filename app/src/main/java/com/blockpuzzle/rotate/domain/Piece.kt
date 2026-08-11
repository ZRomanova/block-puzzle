package com.blockpuzzle.rotate.domain

/**
 * A single piece instance sitting in the tray. [rotationSteps] counts 90°
 * clockwise rotations applied by the player (0..3); it never affects [cellCount]
 * or scoring, only which cells the piece occupies.
 */
data class Piece(
    val id: String,
    val shape: PieceShape,
    val color: PieceColor,
    val rotationSteps: Int = 0
) {
    val cellCount: Int get() = shape.cellCount

    /** Cells occupied by this piece, normalized so min row/col == 0. */
    val cells: List<Coordinate> by lazy { ShapeSymmetry.normalize(rotate(shape.baseCells, rotationSteps)) }

    fun rotatedClockwise(): Piece = copy(rotationSteps = (rotationSteps + 1) % 4)

    private fun rotate(cells: List<Coordinate>, steps: Int): List<Coordinate> {
        var current = cells
        repeat(((steps % 4) + 4) % 4) {
            current = ShapeSymmetry.rotate90(current)
        }
        return current
    }

    companion object {
        fun random(idPrefix: String, shape: PieceShape = PieceShape.LEGACY_CATALOG.random()): Piece =
            Piece(id = "$idPrefix-${System.nanoTime()}", shape = shape, color = PieceColor.random())
    }
}
