package com.blockpuzzle.rotate.domain

/**
 * Shared geometry for piece shapes: normalization, the dihedral group of 8
 * transforms (4 rotations x mirror-or-not), and the canonicalization used to
 * detect that two user-drawn shapes are "the same" up to rotation/reflection.
 *
 * The level editor always treats a shape and any of its 8 dihedral transforms
 * as one and the same shape when checking for duplicates, **regardless of a
 * level's [LevelDefinition.allowRotation]/[LevelDefinition.allowMirror]
 * settings** — kept deliberately simple rather than tracking which specific
 * transforms a given level's player can actually reach. Those two settings
 * instead govern spawn-time behavior (see [LevelShape.resolveCells] for
 * mirroring, [EasyPieceGenerator]/[HardModePieceSelector] for rotation).
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

    /** Left-right reflection (flips the column axis). */
    fun mirror(cells: List<Coordinate>): List<Coordinate> =
        normalize(cells.map { Coordinate(it.row, -it.col) })

    /** All 8 dihedral transforms of [cells] (4 rotations, then the same 4 mirrored), each normalized. */
    fun allTransforms(cells: List<Coordinate>): List<List<Coordinate>> {
        val rotations = mutableListOf(normalize(cells))
        repeat(3) { rotations.add(rotate90(rotations.last())) }
        return rotations + rotations.map { mirror(it) }
    }

    private fun sortedKey(cells: List<Coordinate>): String =
        cells.sortedWith(compareBy({ it.row }, { it.col })).joinToString(";") { "${it.row},${it.col}" }

    /** Shape identity independent of rotation/reflection: the lexicographically smallest key among all 8 transforms. */
    fun canonicalKey(cells: List<Coordinate>): String =
        allTransforms(cells).minOf { sortedKey(it) }

    /**
     * True when [cells]' mirror image is *not* reachable by rotating [cells] alone — a genuine
     * chiral shape (e.g. the S/Z or L/J tetromino pair). Achiral shapes (symmetric under
     * reflection, or whose reflection equals one of their own rotations, like the "corner"
     * triomino) return false.
     */
    fun isChiral(cells: List<Coordinate>): Boolean {
        var current = normalize(cells)
        val rotationOnlyKeys = mutableSetOf(sortedKey(current))
        repeat(3) {
            current = rotate90(current)
            rotationOnlyKeys.add(sortedKey(current))
        }
        return sortedKey(mirror(cells)) !in rotationOnlyKeys
    }
}
