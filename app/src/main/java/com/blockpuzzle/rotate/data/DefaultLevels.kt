package com.blockpuzzle.rotate.data

import com.blockpuzzle.rotate.domain.GameMode
import com.blockpuzzle.rotate.domain.LevelDefinition
import com.blockpuzzle.rotate.domain.LevelShape
import com.blockpuzzle.rotate.domain.PieceShape
import com.blockpuzzle.rotate.domain.ScoringMode
import com.blockpuzzle.rotate.domain.ShapeSymmetry
import kotlinx.coroutines.flow.first

/**
 * Seeds a small, curated set of example levels the first time the app runs, so a new player (or
 * a friend trying the constructor for the first time) has something to play and a template to
 * learn from, instead of an empty level list. Deliberately kept to 3 levels that each show off a
 * different combination of constructor knobs (board size, color mode, algorithm, a hand-picked
 * weighted shape pool vs. the full classic set, and — since 2026-08-11 — rotation/mirror) rather
 * than exhaustively covering every combination — an earlier version seeded all 8 old difficulty
 * x scoring x board-size combos and that was confusing to pick from (see CLAUDE.md).
 *
 * Gated by [LevelsRepository.defaultsSeeded], not "is the level list empty", so a player who
 * later deletes every level doesn't get them silently reseeded.
 */
suspend fun seedDefaultLevelsIfNeeded(levelsRepository: LevelsRepository) {
    if (levelsRepository.defaultsSeeded.first()) return

    val classicCatalog = classicShapePool()
    val miniCatalog = listOf(
        LevelShape.userDrawn(PieceShape.SINGLE, weight = 1),
        LevelShape.userDrawn(PieceShape.DOMINO, weight = 2),
        LevelShape.userDrawn(PieceShape.TRIOMINO_L, weight = 2),
        LevelShape.userDrawn(PieceShape.TETROMINO_O, weight = 1),
        LevelShape.userDrawn(PieceShape.TETROMINO_T, weight = 1),
        LevelShape.userDrawn(PieceShape.TETROMINO_S, weight = 1)
    )

    val defaults = listOf(
        // The familiar baseline: rotation and mirror both on, like the game always played before either was configurable.
        LevelDefinition(
            tag = LevelDefinition.baseTag(ScoringMode.CLASSIC, GameMode.EASY, 8),
            name = "Классика 8×8",
            boardSize = 8,
            colorMode = ScoringMode.CLASSIC,
            algorithm = GameMode.EASY,
            shapes = classicCatalog,
            allowRotation = true,
            allowMirror = true
        ),
        // No rotation: pieces must be placed exactly as dealt — a meaningfully harder, different feel, paired with Хитрый.
        LevelDefinition(
            tag = LevelDefinition.baseTag(ScoringMode.COLOR_BONUS, GameMode.HARD, 6),
            name = "Цветной хитрец 6×6",
            boardSize = 6,
            colorMode = ScoringMode.COLOR_BONUS,
            algorithm = GameMode.HARD,
            shapes = classicCatalog,
            allowRotation = false,
            allowMirror = true
        ),
        // No mirroring: TETROMINO_S only ever spawns as drawn, never flips to its Z mirror.
        LevelDefinition(
            tag = LevelDefinition.baseTag(ScoringMode.CLASSIC, GameMode.EASY, 5),
            name = "Мини-вызов 5×5",
            boardSize = 5,
            colorMode = ScoringMode.CLASSIC,
            algorithm = GameMode.EASY,
            shapes = miniCatalog,
            allowRotation = true,
            allowMirror = false
        )
    )

    levelsRepository.seedDefaults(defaults)
}

/**
 * [PieceShape.LEGACY_CATALOG] deliberately keeps some mirror pairs as separate entries
 * (TETROMINO_L/J, TETROMINO_S/Z) for historical reasons (see the catalog's own doc comment) —
 * but seeding both halves of a pair as two separate [LevelShape]s here would show up as two
 * visually mirror-image rows in the constructor's shape list, which reads as a mistake now that
 * a shape and its mirror are always meant to be "one shape" (see `ShapeSymmetry`'s doc comment).
 * This collapses each such pair down to one entry (`LevelShape.userDrawn`, so it naturally gets
 * `includeMirror = true` and can still spawn as either half) before handing shapes to a fresh
 * example level.
 */
internal fun classicShapePool(): List<LevelShape> {
    val seenKeys = mutableSetOf<String>()
    return PieceShape.LEGACY_CATALOG.mapNotNull { shape ->
        val key = ShapeSymmetry.canonicalKey(shape.baseCells)
        if (!seenKeys.add(key)) null else LevelShape.userDrawn(shape)
    }
}
