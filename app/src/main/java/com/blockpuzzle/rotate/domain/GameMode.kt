package com.blockpuzzle.rotate.domain

import kotlinx.serialization.Serializable

/** EASY = "Случайный" (random) algorithm, HARD = "Хитрый" (clever/lookahead) algorithm in the UI. */
@Serializable
enum class GameMode {
    EASY, HARD
}
