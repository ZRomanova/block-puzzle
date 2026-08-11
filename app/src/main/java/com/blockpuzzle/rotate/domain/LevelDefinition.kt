package com.blockpuzzle.rotate.domain

import kotlinx.serialization.Serializable

/**
 * A user-defined (or legacy-seeded) game variant: board size, color mode, piece-selection
 * algorithm, and the pool of shapes allowed to appear. Replaces the old fixed 8-combination
 * [GameVariant] (now removed) — records are keyed by [tag], not by a compile-time enum
 * combination, so any number of levels can share the same board size/color mode/algorithm.
 *
 * [tag] is assigned once at creation (see [nextAvailableTag]) and never regenerated on edit,
 * so a level's identity — and its resumable/paused game, if any — survives rule changes.
 * [name] is freely user-editable and carries no identity.
 */
@Serializable
data class LevelDefinition(
    val tag: String,
    val name: String,
    val boardSize: Int,
    val colorMode: ScoringMode,
    val algorithm: GameMode,
    val shapes: List<LevelShape>
) {
    companion object {
        val ALLOWED_BOARD_SIZES = 5..8

        /** The square (side length) a shape drawn for this level's board must fit inside. */
        fun editableGridSize(boardSize: Int): Int = (boardSize * 0.8).toInt()

        private fun colorModeTagPart(colorMode: ScoringMode) =
            if (colorMode == ScoringMode.CLASSIC) "Однотонный" else "Цветной"

        private fun algorithmTagPart(algorithm: GameMode) =
            if (algorithm == GameMode.EASY) "Случайный" else "Хитрый"

        fun baseTag(colorMode: ScoringMode, algorithm: GameMode, boardSize: Int): String =
            "${colorModeTagPart(colorMode)}${algorithmTagPart(algorithm)}$boardSize"

        /** [base] itself if free, otherwise "base-2", "base-3", ... — whichever isn't already used by [existingTags]. */
        fun nextAvailableTag(base: String, existingTags: Collection<String>): String {
            if (base !in existingTags) return base
            var suffix = 2
            while ("$base-$suffix" in existingTags) suffix++
            return "$base-$suffix"
        }
    }
}
