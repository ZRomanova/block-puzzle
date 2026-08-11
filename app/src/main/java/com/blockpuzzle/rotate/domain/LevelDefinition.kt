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
 *
 * [allowRotation] (added 2026-08-11) defaults to `true` — the game's original, still-headline
 * mechanic ("pieces can be freely rotated before placing") is unchanged unless a level
 * explicitly opts out. Old persisted levels that predate this field decode with it `true`,
 * matching the behavior they always had. It's a genuine gameplay rule, on par with board
 * size/color mode/algorithm: changing it on an existing level counts as a rule change for
 * [GameViewModel.saveLevel]'s record-reset diff, same as any other field here.
 *
 * There is deliberately no equivalent `allowMirror` setting. An earlier version let a shape and
 * its mirror image count as one pool entry that could spawn as either chirality, with a manual
 * flip button in-game — the user found that confusing/annoying in practice and asked for it to
 * be fully reverted (see `ShapeSymmetry`'s doc comment). A shape's mirror image is now just a
 * second, independent shape the player draws separately if they want it.
 */
@Serializable
data class LevelDefinition(
    val tag: String,
    val name: String,
    val boardSize: Int,
    val colorMode: ScoringMode,
    val algorithm: GameMode,
    val shapes: List<LevelShape>,
    val allowRotation: Boolean = true
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

        /**
         * True when [shapes] is *provably* guaranteed to never produce a game over on a
         * [boardSize] board, given whether this level lets the player rotate a spawned piece
         * ([allowRotation]). Two cases are provable:
         *
         * 1. Every shape is a single cell (e.g. a pool of only [PieceShape.SINGLE]): a lone
         *    cell always fits in any non-full board — it needs no adjacent empty cell at all —
         *    so this holds for *any* board size, and doesn't depend on rotation at all (a point
         *    has no orientation to rotate).
         *
         * 2. Every shape is a domino (2 cells), [boardSize] is even, **and [allowRotation] is
         *    true**: color the board like a checkerboard. Any 2-cell orthogonally-adjacent
         *    placement — horizontal or vertical — always covers exactly one black and one white
         *    cell, and a full row/column of *even* length always contains equally many of each
         *    color, so placing dominoes and clearing full lines can never unbalance the
         *    black-filled-count == white-filled-count invariant. That rules out the classic
         *    "scattered, mutually non-adjacent single-cell gaps" deadlock (the same
         *    configuration [ShapeConnectivityTest]/[BoardTest] use to demonstrate a domino
         *    *can* be blocked in general) — it would require an all-one-color set of leftover
         *    cells, which is exactly what the invariant forbids. On an **odd**-sized board a
         *    full line has unequal color counts, so a line clear *can* unbalance the two
         *    colors — there is no such proof there. **Requiring [allowRotation]** matters
         *    because a domino's `baseCells` fix *one* orientation (say horizontal), and
         *    mirroring a straight 2-cell piece doesn't change that (its mirror image is itself)
         *    — without rotation, every spawned domino can only ever be placed in that one fixed
         *    orientation, so a board full of only *vertically*-adjacent empty pairs would block
         *    it completely even though plenty of empty cells remain. The color-balance argument
         *    doesn't rescue that case, so it's excluded.
         *
         * This is deliberately narrow. It does not attempt to prove — or disprove — unlosability
         * for any shape with 3+ cells (a 3-cell placement already unbalances the checkerboard
         * invariant by construction, so the same argument doesn't extend), or for shape pools
         * mixing sizes. Not being flagged here is not proof a level *is* losable, only that
         * there's no proof it isn't — the conservative default is to allow it rather than risk
         * blocking a level that's actually fine.
         */
        fun isUnlosable(shapes: List<LevelShape>, boardSize: Int, allowRotation: Boolean): Boolean {
            if (shapes.isEmpty()) return false
            if (shapes.all { it.shape.cellCount <= 1 }) return true
            if (allowRotation && boardSize % 2 == 0 && shapes.all { it.shape.cellCount == 2 }) return true
            return false
        }
    }
}
