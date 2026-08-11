package com.blockpuzzle.rotate.domain

/**
 * Palette identifier assigned to a piece when it is generated. Kept as a plain
 * enum (no UI dependency) so domain code stays testable; the ui layer maps
 * each value to an actual color.
 */
enum class PieceColor {
    RED, ORANGE, YELLOW, GREEN, BLUE, PURPLE;

    companion object {
        fun random(): PieceColor = entries[kotlin.random.Random.nextInt(entries.size)]
    }
}
