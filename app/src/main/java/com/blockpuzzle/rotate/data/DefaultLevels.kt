package com.blockpuzzle.rotate.data

import com.blockpuzzle.rotate.domain.GameMode
import com.blockpuzzle.rotate.domain.LevelDefinition
import com.blockpuzzle.rotate.domain.LevelShape
import com.blockpuzzle.rotate.domain.PieceShape
import com.blockpuzzle.rotate.domain.ScoringMode
import kotlinx.coroutines.flow.first

/**
 * Seeds a small, curated set of example levels the first time the app runs, so a new player (or
 * a friend trying the constructor for the first time) has something to play and a template to
 * learn from, instead of an empty level list. Deliberately kept to 3 levels that each show off a
 * different combination of constructor knobs (board size, color mode, algorithm, rotation, and a
 * hand-picked weighted shape pool vs. the full classic set) rather than exhaustively covering
 * every combination — an earlier version seeded all 8 old difficulty x scoring x board-size
 * combos and that was confusing to pick from (see CLAUDE.md).
 *
 * Gated by [LevelsRepository.defaultsSeeded], not "is the level list empty", so a player who
 * later deletes every level doesn't get them silently reseeded.
 */
suspend fun seedDefaultLevelsIfNeeded(levelsRepository: LevelsRepository) {
    if (levelsRepository.defaultsSeeded.first()) return

    val classicCatalog = PieceShape.LEGACY_CATALOG.map { LevelShape(it, weight = 1) }
    val miniCatalog = listOf(
        LevelShape(PieceShape.SINGLE, weight = 1),
        LevelShape(PieceShape.DOMINO, weight = 2),
        LevelShape(PieceShape.TRIOMINO_L, weight = 2),
        LevelShape(PieceShape.TETROMINO_O, weight = 1),
        LevelShape(PieceShape.TETROMINO_T, weight = 1),
        LevelShape(PieceShape.TETROMINO_S, weight = 1)
    )

    val defaults = listOf(
        // The familiar baseline: the full legacy shape catalog, rotation on, like the game always played.
        LevelDefinition(
            tag = LevelDefinition.baseTag(ScoringMode.CLASSIC, GameMode.EASY, 8),
            name = "Классика 8×8",
            boardSize = 8,
            colorMode = ScoringMode.CLASSIC,
            algorithm = GameMode.EASY,
            shapes = classicCatalog,
            allowRotation = true
        ),
        // No rotation: pieces must be placed exactly as dealt — a meaningfully harder, different feel, paired with Хитрый.
        LevelDefinition(
            tag = LevelDefinition.baseTag(ScoringMode.COLOR_BONUS, GameMode.HARD, 6),
            name = "Цветной хитрец 6×6",
            boardSize = 6,
            colorMode = ScoringMode.COLOR_BONUS,
            algorithm = GameMode.HARD,
            shapes = classicCatalog,
            allowRotation = false
        ),
        // A small, hand-picked, non-uniformly weighted shape pool instead of the full legacy set.
        LevelDefinition(
            tag = LevelDefinition.baseTag(ScoringMode.CLASSIC, GameMode.EASY, 5),
            name = "Мини-вызов 5×5",
            boardSize = 5,
            colorMode = ScoringMode.CLASSIC,
            algorithm = GameMode.EASY,
            shapes = miniCatalog,
            allowRotation = true
        )
    )

    levelsRepository.seedDefaults(defaults)
}

/**
 * One-shot migration (2026-08-12): zeroes [LevelDefinition.undoPenaltyPercent] on every level
 * that already exists at the moment this runs. Added right after the undo-penalty feature
 * shipped, once the user noticed she could no longer beat records she'd set *before* undo started
 * costing points — those old records were earned with free undos, so comparing them against a
 * penalized score isn't a fair contest. This resets the penalty to 0% on her existing levels
 * (default-seeded and custom alike) so the old records stay reachable; levels created *after* this
 * migration has run are untouched and keep the normal [ScoringConfig.DEFAULT_UNDO_PENALTY_PERCENT]
 * (or whatever she picks in the constructor) — she can still opt back into the feature for new
 * levels. Gated by [LevelsRepository.undoPenaltyMigrated] so it only ever fires once.
 */
suspend fun zeroExistingUndoPenaltiesIfNeeded(levelsRepository: LevelsRepository) {
    if (levelsRepository.undoPenaltyMigrated.first()) return
    levelsRepository.zeroOutUndoPenalties()
}
