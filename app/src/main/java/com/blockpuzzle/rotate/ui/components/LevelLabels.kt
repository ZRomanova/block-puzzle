package com.blockpuzzle.rotate.ui.components

import com.blockpuzzle.rotate.domain.GameMode
import com.blockpuzzle.rotate.domain.LevelDefinition
import com.blockpuzzle.rotate.domain.ScoringConfig
import com.blockpuzzle.rotate.domain.ScoringMode

/**
 * Short "Однотонный · Случайный · 6×6"-style summary of a level's rules, reused across the
 * level list, constructor, game and game-over screens. Rotation and the undo penalty only get
 * called out when they deviate from their defaults (enabled / 20%) — most levels don't touch
 * them, and appending a badge for every default value to every single row would just be noise.
 */
fun LevelDefinition.rulesSummary(): String = buildString {
    append(if (colorMode == ScoringMode.CLASSIC) "Однотонный" else "Цветной")
    append(" · ")
    append(if (algorithm == GameMode.EASY) "Случайный" else "Хитрый")
    append(" · $boardSize×$boardSize")
    if (!allowRotation) append(" · без вращения")
    if (undoPenaltyPercent != ScoringConfig.DEFAULT_UNDO_PENALTY_PERCENT) {
        append(if (undoPenaltyPercent == 0) " · undo бесплатно" else " · undo -$undoPenaltyPercent%")
    }
}
