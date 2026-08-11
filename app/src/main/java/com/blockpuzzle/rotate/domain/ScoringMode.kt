package com.blockpuzzle.rotate.domain

import kotlinx.serialization.Serializable

/**
 * CLASSIC ("Однотонный" in the UI) scores only cells + line clears and forces every piece blue;
 * COLOR_BONUS ("Цветной") additionally rewards clearing a line of matching colors.
 */
@Serializable
enum class ScoringMode {
    CLASSIC, COLOR_BONUS
}
