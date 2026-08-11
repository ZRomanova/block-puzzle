package com.blockpuzzle.rotate.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blockpuzzle.rotate.domain.LevelDefinition
import com.blockpuzzle.rotate.ui.navigation.Screen
import com.blockpuzzle.rotate.ui.screens.ConstructorScreen
import com.blockpuzzle.rotate.ui.screens.GameOverScreen
import com.blockpuzzle.rotate.ui.screens.GameScreen
import com.blockpuzzle.rotate.ui.screens.LevelEditorScreen
import com.blockpuzzle.rotate.ui.screens.LevelListScreen
import com.blockpuzzle.rotate.ui.screens.MenuScreen
import com.blockpuzzle.rotate.ui.screens.RulesScreen
import com.blockpuzzle.rotate.ui.theme.BlockPuzzleTheme
import com.blockpuzzle.rotate.ui.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BlockPuzzleTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BlockPuzzleApp()
                }
            }
        }
    }
}

@Composable
private fun BlockPuzzleApp(viewModel: GameViewModel = viewModel()) {
    val screen by viewModel.screen.collectAsState()
    val gameUiState by viewModel.gameUiState.collectAsState()
    val levels by viewModel.levels.collectAsState()
    val records by viewModel.records.collectAsState()
    val resumableLevelTags by viewModel.resumableLevelTags.collectAsState()

    val startOrResume: (LevelDefinition) -> Unit = { level ->
        if (level.tag in resumableLevelTags) viewModel.resumeGame(level.tag) else viewModel.startGame(level)
    }
    val topLevels = remember(levels, records) {
        levels.filter { (records[it.tag] ?: 0) > 0 }
            .sortedByDescending { records[it.tag] ?: 0 }
            .take(5)
    }

    AnimatedContent(
        targetState = screen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen"
    ) { targetScreen ->
        when (targetScreen) {
            is Screen.Menu -> MenuScreen(
                topLevels = topLevels,
                records = records,
                onOpenLevelList = viewModel::goToLevelList,
                onOpenConstructor = viewModel::goToConstructor,
                onOpenRules = viewModel::goToRules,
                onQuickPlay = startOrResume
            )

            is Screen.Rules -> RulesScreen(onBack = viewModel::goToMenu)

            is Screen.LevelList -> LevelListScreen(
                levels = levels,
                records = records,
                resumableTags = resumableLevelTags,
                onBack = viewModel::goToMenu,
                onSelect = startOrResume
            )

            is Screen.Constructor -> ConstructorScreen(
                levels = levels,
                onBack = viewModel::goToMenu,
                onCreate = { viewModel.openLevelEditor(null) },
                onEdit = { level -> viewModel.openLevelEditor(level.tag) },
                onDelete = { level -> viewModel.deleteLevel(level.tag) }
            )

            is Screen.LevelEditor -> {
                val editingLevel = remember(targetScreen.editingTag, levels) {
                    levels.firstOrNull { it.tag == targetScreen.editingTag }
                }
                LevelEditorScreen(
                    editingLevel = editingLevel,
                    record = records[targetScreen.editingTag] ?: 0,
                    onBack = viewModel::goToConstructor,
                    onSave = { name, boardSize, colorMode, algorithm, shapes, allowRotation, allowMirror, saveAsCopy ->
                        viewModel.saveLevel(
                            targetScreen.editingTag, name, boardSize, colorMode, algorithm, shapes,
                            allowRotation, allowMirror, saveAsCopy
                        )
                    }
                )
            }

            is Screen.Game -> gameUiState?.let { state ->
                GameScreen(
                    uiState = state,
                    record = records[state.level.tag] ?: 0,
                    onPlace = viewModel::place,
                    onCanPlace = viewModel::canPlace,
                    onLinesPreview = viewModel::linesPreview,
                    onRotate = viewModel::rotate,
                    onUndo = viewModel::undo,
                    onExitToMenu = viewModel::exitToLevelList
                )
            }

            is Screen.GameOver -> {
                val record = records[targetScreen.level.tag] ?: 0
                GameOverScreen(
                    level = targetScreen.level,
                    score = targetScreen.score,
                    record = record,
                    isNewRecord = targetScreen.isNewRecord,
                    onPlayAgain = viewModel::playAgain,
                    onExitToMenu = viewModel::abandonGame
                )
            }
        }
    }
}
