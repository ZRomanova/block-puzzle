package com.blockpuzzle.rotate.domain

/**
 * Shared geometry for piece shapes: normalization, the dihedral group of 8
 * transforms (4 rotations x mirror-or-not), and the canonicalization used to
 * detect that two user-drawn shapes are "the same" — up to whichever of
 * rotation/reflection the level actually lets a player reach in-game (see
 * [LevelDefinition.allowRotation]/[LevelDefinition.allowMirror]).
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

    /** The rotations of [cells] — just itself when [allowRotation] is false. */
    private fun rotationsOf(cells: List<Coordinate>, allowRotation: Boolean): List<List<Coordinate>> {
        if (!allowRotation) return listOf(normalize(cells))
        val rotations = mutableListOf(normalize(cells))
        repeat(3) { rotations.add(rotate90(rotations.last())) }
        return rotations
    }

    private fun sortedKey(cells: List<Coordinate>): String =
        cells.sortedWith(compareBy({ it.row }, { it.col })).joinToString(";") { "${it.row},${it.col}" }

    /**
     * Shape identity up to whichever transforms are actually reachable by a player of this
     * level: [allowRotation] controls whether the 4 rotations count as "the same shape"
     * (matches the in-game rotate button), [allowMirror] whether the mirrored orientations do
     * too. With both false this reduces to plain exact-shape equality. Used at duplicate-shape
     * detection time in the level editor.
     */
    fun canonicalKey(cells: List<Coordinate>, allowRotation: Boolean = true, allowMirror: Boolean = true): String {
        val rotations = rotationsOf(cells, allowRotation)
        val transforms = if (allowMirror) rotations + rotations.map { mirror(it) } else rotations
        return transforms.minOf { sortedKey(it) }
    }

    /**
     * True when [cells]' mirror image is *not* reachable by rotating [cells] alone (when
     * [allowRotation] is true) — a genuine chiral shape (e.g. the S/Z or L/J tetromino pair).
     * Achiral shapes (symmetric under reflection, or whose reflection equals one of their own
     * rotations, like the "corner" triomino) return false.
     *
     * With [allowRotation] false, there's no rotation available to close that gap, so only
     * shapes that are reflection-symmetric in their *exact drawn* orientation (e.g.
     * `TETROMINO_T`) count as achiral — a shape like the corner triomino, achiral only by
     * virtue of a rotation, becomes chiral here.
     */
    fun isChiral(cells: List<Coordinate>, allowRotation: Boolean = true): Boolean {
        val rotationOnlyKeys = rotationsOf(cells, allowRotation).map { sortedKey(it) }.toSet()
        return sortedKey(mirror(cells)) !in rotationOnlyKeys
    }
}
