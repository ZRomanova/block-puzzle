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
 * different combination of constructor knobs (board size, color mode, algorithm, and a
 * hand-picked weighted shape pool vs. the full classic set) rather than exhaustively covering
 * every combination — an earlier version seeded all 8 old difficulty x scoring x board-size
 * combos and that was confusing to pick from (see CLAUDE.md).
 *
 * Gated by [LevelsRepository.defaultsSeeded], not "is the level list empty", so a player who
 * later deletes every level doesn't get them silently reseeded.
 */
suspend fun seedDefaultLevelsIfNeeded(levelsRepository: LevelsRepository) {
    if (levelsRepository.defaultsSeeded.first()) return

    val classicCatalog = PieceShape.LEGACY_CATALOG.map { LevelShape(it, weight = 1, includeMirror = false) }
    val miniCatalog = listOf(
        LevelShape(PieceShape.SINGLE, weight = 1),
        LevelShape(PieceShape.DOMINO, weight = 2),
        LevelShape(PieceShape.TRIOMINO_L, weight = 2),
        LevelShape(PieceShape.TETROMINO_O, weight = 1),
        LevelShape(PieceShape.TETROMINO_T, weight = 1),
        LevelShape(PieceShape.PENTOMINO_PLUS, weight = 1)
    )

    val defaults = listOf(
        LevelDefinition(
            tag = LevelDefinition.baseTag(ScoringMode.CLASSIC, GameMode.EASY, 8),
            name = "Классика 8×8",
            boardSize = 8,
            colorMode = ScoringMode.CLASSIC,
            algorithm = GameMode.EASY,
            shapes = classicCatalog
        ),
        LevelDefinition(
            tag = LevelDefinition.baseTag(ScoringMode.COLOR_BONUS, GameMode.HARD, 6),
            name = "Цветной хитрец 6×6",
            boardSize = 6,
            colorMode = ScoringMode.COLOR_BONUS,
            algorithm = GameMode.HARD,
            shapes = classicCatalog
        ),
        LevelDefinition(
            tag = LevelDefinition.baseTag(ScoringMode.CLASSIC, GameMode.EASY, 5),
            name = "Мини-вызов 5×5",
            boardSize = 5,
            colorMode = ScoringMode.CLASSIC,
            algorithm = GameMode.EASY,
            shapes = miniCatalog
        )
    )

    levelsRepository.seedDefaults(defaults)
}
