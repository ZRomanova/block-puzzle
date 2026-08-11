package com.blockpuzzle.rotate.domain

import kotlinx.serialization.Serializable

/**
 * One shape entry within a [LevelDefinition]'s pool. [weight] only matters for the
 * Случайный (EASY) algorithm — Хитрый (HARD) treats every entry in the pool as an
 * equally eligible candidate regardless of weight.
 */
@Serializable
data class LevelShape(
    val shape: PieceShape,
    val weight: Int = 1
)
