package com.blockpuzzle.rotate.ui.navigation

import com.blockpuzzle.rotate.domain.LevelDefinition

/** The app has a fixed, shallow set of screens; no back stack complexity is needed. */
sealed interface Screen {
    data object Menu : Screen
    data object Rules : Screen
    data object LevelList : Screen
    data object Constructor : Screen

    /** [editingTag] null = creating a brand-new level; otherwise editing the level with that tag. */
    data class LevelEditor(val editingTag: String?) : Screen

    data class Game(val level: LevelDefinition) : Screen
    data class GameOver(val level: LevelDefinition, val score: Int, val isNewRecord: Boolean) : Screen
}
