package com.blockpuzzle.rotate.domain

/**
 * All scoring coefficients live here so tuning them never touches game logic.
 *
 * Line bonus grows super-linearly with how many lines clear simultaneously
 * (triangular term), so clearing e.g. 3 lines at once is worth more than
 * 3 separate single-line clears.
 */
object ScoringConfig {
    const val POINTS_PER_CELL = 1
    const val POINTS_PER_LINE = 10
    const val COMBO_STEP_BONUS = 5

    /** Per cell of a cleared line's dominant color, beyond the first (COLOR_BONUS scoring mode only). */
    const val COLOR_BONUS_PER_MATCHING_CELL = 2

    /** Extra flat bonus when an entire cleared line is a single color. */
    const val COLOR_FULL_LINE_BONUS = 10

    fun scoreForMove(cellsPlaced: Int, linesCleared: Int): Int {
        val cellScore = cellsPlaced * POINTS_PER_CELL
        if (linesCleared <= 0) return cellScore
        val lineScore = linesCleared * POINTS_PER_LINE +
            COMBO_STEP_BONUS * (linesCleared * (linesCleared - 1) / 2)
        return cellScore + lineScore
    }

    /** Bonus for one cleared line: rewards its most common color, with an extra kicker if it's fully monochrome. */
    fun colorBonusForLine(colors: List<PieceColor>): Int {
        if (colors.isEmpty()) return 0
        val maxCount = colors.groupingBy { it }.eachCount().values.max()
        var bonus = (maxCount - 1) * COLOR_BONUS_PER_MATCHING_CELL
        if (maxCount == colors.size) bonus += COLOR_FULL_LINE_BONUS
        return bonus
    }

    fun colorBonusForMove(clearedLineColors: List<List<PieceColor>>): Int =
        clearedLineColors.sumOf { colorBonusForLine(it) }
}
