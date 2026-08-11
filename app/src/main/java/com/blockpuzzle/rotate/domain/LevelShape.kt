package com.blockpuzzle.rotate.domain

import kotlinx.serialization.Serializable

/**
 * One shape entry within a [LevelDefinition]'s pool. [weight] only matters for the
 * Случайный (EASY) algorithm — Хитрый (HARD) treats every entry in the pool as an
 * equally eligible candidate regardless of weight.
 *
 * [includeMirror] is a geometric fact about [shape], decided once when the shape is
 * added (rather than derived on the fly from [ShapeSymmetry.isChiral]) and never
 * touched again. The legacy catalog deliberately keeps some mirror pairs as separate
 * entries (TETROMINO_L/J, TETROMINO_S/Z) and at least one shape with no mirror
 * counterpart at all (PENTOMINO_L), so spawn-time piece distribution must not
 * silently change if mirroring were re-derived from geometry alone for shapes that
 * were never meant to spawn a mirrored twin. Whether that fact actually results in a
 * mirrored spawn is a *separate*, live question — see [resolveCells].
 */
@Serializable
data class LevelShape(
    val shape: PieceShape,
    val weight: Int = 1,
    val includeMirror: Boolean = false
) {
    /**
     * The concrete (possibly mirrored) base cells a freshly generated piece from this entry
     * should use. [allowMirror] is the *level's current* setting, not baked into [includeMirror]
     * — so flipping a level's mirror toggle takes effect immediately for every shape in it,
     * including ones added before the flip, with nothing to retroactively recompute.
     */
    fun resolveCells(random: kotlin.random.Random, allowMirror: Boolean): List<Coordinate> =
        if (includeMirror && allowMirror && random.nextBoolean()) ShapeSymmetry.mirror(shape.baseCells) else shape.baseCells

    companion object {
        /** For a shape the user just drew in the constructor: mirror-inclusion is decided once, from its geometry. */
        fun userDrawn(shape: PieceShape, weight: Int = 1): LevelShape =
            LevelShape(shape, weight, includeMirror = ShapeSymmetry.isChiral(shape.baseCells))
    }
}
