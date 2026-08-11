package com.blockpuzzle.rotate.domain

import kotlinx.serialization.Serializable

/**
 * One shape entry within a [LevelDefinition]'s pool. [weight] only matters for the
 * Случайный (EASY) algorithm — Хитрый (HARD) treats every entry in the pool as an
 * equally eligible candidate regardless of weight.
 *
 * [includeMirror] is set once, when the shape is added, rather than derived on the
 * fly from [ShapeSymmetry.isChiral]. The legacy catalog deliberately keeps some
 * mirror pairs as separate entries (TETROMINO_L/J, TETROMINO_S/Z) and at least one
 * shape with no mirror counterpart at all (PENTOMINO_L), so spawn-time piece
 * distribution must not silently change if mirroring were re-derived from geometry
 * alone for shapes that were never meant to spawn a mirrored twin.
 */
@Serializable
data class LevelShape(
    val shape: PieceShape,
    val weight: Int = 1,
    val includeMirror: Boolean = false
) {
    /** The concrete (possibly mirrored) base cells a freshly generated piece from this entry should use. */
    fun resolveCells(random: kotlin.random.Random): List<Coordinate> =
        if (includeMirror && random.nextBoolean()) ShapeSymmetry.mirror(shape.baseCells) else shape.baseCells

    companion object {
        /**
         * For a shape the user just drew in the constructor: mirror-inclusion is decided once,
         * from its geometry, respecting the level's [allowMirror]/[allowRotation] settings — if
         * the level doesn't allow mirroring at all, [includeMirror] is forced false regardless
         * of chirality; [allowRotation] affects what counts as "chiral" in the first place (see
         * [ShapeSymmetry.isChiral]).
         */
        fun userDrawn(shape: PieceShape, weight: Int = 1, allowRotation: Boolean = true, allowMirror: Boolean = true): LevelShape =
            LevelShape(shape, weight, includeMirror = allowMirror && ShapeSymmetry.isChiral(shape.baseCells, allowRotation))
    }
}
