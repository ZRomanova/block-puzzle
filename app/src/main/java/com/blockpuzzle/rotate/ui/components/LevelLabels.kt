package com.blockpuzzle.rotate.ui.components

import com.blockpuzzle.rotate.domain.GameMode
import com.blockpuzzle.rotate.domain.LevelDefinition
import com.blockpuzzle.rotate.domain.ScoringMode

/** Short "Однотонный · Случайный · 6×6"-style summary of a level's rules, reused across the level list, constructor, game and game-over screens. */
fun LevelDefinition.rulesSummary(): String = buildString {
    append(if (colorMode == ScoringMode.CLASSIC) "Однотонный" else "Цветной")
    append(" · ")
    append(if (algorithm == GameMode.EASY) "Случайный" else "Хитрый")
    append(" · $boardSize×$boardSize")
}
