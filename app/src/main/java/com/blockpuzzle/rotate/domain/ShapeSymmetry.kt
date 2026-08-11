package com.blockpuzzle.rotate.domain

/**
 * Shared geometry for piece shapes: normalization, 90° rotation, and the canonicalization used
 * to detect that two user-drawn shapes are "the same" up to rotation.
 *
 * A shape and its **mirror image are deliberately NOT treated as the same shape** — an earlier
 * version of the constructor merged mirror pairs into one pool entry that could spawn as either
 * chirality, with a matching manual flip button in-game. The user found that experience
 * confusing/annoying in practice and asked for it to be fully reverted: drawing a shape's mirror
 * image is now just drawing a second, independent shape, exactly like drawing any other two
 * different shapes. Don't reintroduce mirror-awareness anywhere without asking again.
 *
 * [Piece] delegates its own rotation here so there is exactly one
 * implementation of "rotate a cell list" in the codebase.
 */
object ShapeSymmetry {

    fun normalize(cells: List<Coordinate>): List<Coordinate> {
        val minRow = cells.minOf { it.row }
        val minCol = cells.minOf { it.col }
        return cells.map { Coordinate(it.row - minRow, it.col - minCol) }
    }

    fun rotate90(cells: List<Coordinate>): List<Coordinate> =
        normalize(cells.map { Coordinate(it.col, -it.row) })

    private fun sortedKey(cells: List<Coordinate>): String =
        cells.sortedWith(compareBy({ it.row }, { it.col })).joinToString(";") { "${it.row},${it.col}" }

    /** Shape identity independent of rotation: the lexicographically smallest key among the 4 rotations. */
    fun canonicalKey(cells: List<Coordinate>): String {
        var current = normalize(cells)
        var best = sortedKey(current)
        repeat(3) {
            current = rotate90(current)
            val key = sortedKey(current)
            if (key < best) best = key
        }
        return best
    }
}
