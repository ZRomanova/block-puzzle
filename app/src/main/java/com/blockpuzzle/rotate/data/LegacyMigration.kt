package com.blockpuzzle.rotate.data

import com.blockpuzzle.rotate.domain.GameMode
import com.blockpuzzle.rotate.domain.LevelDefinition
import com.blockpuzzle.rotate.domain.LevelShape
import com.blockpuzzle.rotate.domain.PieceShape
import com.blockpuzzle.rotate.domain.ScoringMode
import kotlinx.coroutines.flow.first

/** (board size, legacy `BoardSize` enum name) pairs the pre-constructor game shipped with. */
private val LEGACY_BOARD_SIZES = listOf(6 to "SIZE_6", 8 to "SIZE_8")

/**
 * One-time migration from the pre-constructor fixed 8-variant system (difficulty x scoring mode
 * x board size, `GameVariant` — now deleted) to the unified level list. Seeds the 8 old
 * combinations as regular, fully editable/deletable levels using the full legacy shape catalog
 * (weight 1 each, no auto-mirroring — matches exactly what shipped), and copies each old high
 * score (read via the old `"${mode.name}_${scoring.name}_${boardSize.name}"` key format) onto
 * its new tag so nothing the user already earned is lost.
 *
 * Kept in its own file, isolated from [LevelsRepository]/[RecordsRepository], so this one-off
 * wart is easy to find — and delete later — without touching either repository's otherwise
 * generic logic. Gated by [LevelsRepository.defaultsSeeded], not "is the level list empty",
 * so a user who later deletes every level doesn't get them silently reseeded.
 */
suspend fun migrateLegacyIfNeeded(levelsRepository: LevelsRepository, recordsRepository: RecordsRepository) {
    if (levelsRepository.defaultsSeeded.first()) return

    val legacyShapes = PieceShape.LEGACY_CATALOG.map { LevelShape(it, weight = 1, includeMirror = false) }
    val seededLevels = mutableListOf<LevelDefinition>()

    for (mode in GameMode.entries) {
        for (scoring in ScoringMode.entries) {
            for ((size, legacySizeName) in LEGACY_BOARD_SIZES) {
                val tag = LevelDefinition.baseTag(scoring, mode, size)
                val legacyKey = "${mode.name}_${scoring.name}_$legacySizeName"
                val oldScore = recordsRepository.scoreOnce(legacyKey)

                seededLevels.add(
                    LevelDefinition(
                        tag = tag,
                        name = tag,
                        boardSize = size,
                        colorMode = scoring,
                        algorithm = mode,
                        shapes = legacyShapes
                    )
                )
                if (oldScore > 0) recordsRepository.submitScore(tag, oldScore)
            }
        }
    }

    levelsRepository.seedDefaults(seededLevels)
}
