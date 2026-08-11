package com.blockpuzzle.rotate.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.blockpuzzle.rotate.data.LevelsRepository
import com.blockpuzzle.rotate.data.RecordsRepository
import com.blockpuzzle.rotate.data.seedDefaultLevelsIfNeeded
import com.blockpuzzle.rotate.domain.Board
import com.blockpuzzle.rotate.domain.EasyPieceGenerator
import com.blockpuzzle.rotate.domain.GameEngine
import com.blockpuzzle.rotate.domain.GameMode
import com.blockpuzzle.rotate.domain.GameState
import com.blockpuzzle.rotate.domain.HardModePieceSelector
import com.blockpuzzle.rotate.domain.LevelDefinition
import com.blockpuzzle.rotate.domain.LevelShape
import com.blockpuzzle.rotate.domain.Piece
import com.blockpuzzle.rotate.domain.PieceColor
import com.blockpuzzle.rotate.domain.PlacementResult
import com.blockpuzzle.rotate.domain.ScoringMode
import com.blockpuzzle.rotate.ui.navigation.Screen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GameUiState(
    val board: Board,
    val tray: List<Piece?>,
    val score: Int,
    val level: LevelDefinition,
    val isGameOver: Boolean,
    val canUndo: Boolean,
    val lastClear: PlacementResult? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val levelsRepository = LevelsRepository(app)
    private val recordsRepository = RecordsRepository(app)

    private var activeEngine: GameEngine? = null

    /** One backgrounded (paused, not-yet-over) game per level tag, kept only for this process's lifetime. */
    private val pausedEngines = mutableMapOf<String, GameEngine>()

    private val _screen = MutableStateFlow<Screen>(Screen.Menu)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _gameUiState = MutableStateFlow<GameUiState?>(null)
    val gameUiState: StateFlow<GameUiState?> = _gameUiState.asStateFlow()

    private val _resumableLevelTags = MutableStateFlow<Set<String>>(emptySet())
    val resumableLevelTags: StateFlow<Set<String>> = _resumableLevelTags.asStateFlow()

    val levels: StateFlow<List<LevelDefinition>> = levelsRepository.levels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val records: StateFlow<Map<String, Int>> = levelsRepository.levels
        .flatMapLatest { list -> recordsRepository.allBestScores(list.map { it.tag }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        viewModelScope.launch { seedDefaultLevelsIfNeeded(levelsRepository) }
    }

    /** Starts a fresh game for [level], discarding any paused game previously left for it. */
    fun startGame(level: LevelDefinition) {
        pausedEngines.remove(level.tag)
        updateResumable()

        val generator = if (level.algorithm == GameMode.EASY) {
            EasyPieceGenerator(level.shapes, allowRotation = level.allowRotation)
        } else {
            HardModePieceSelector(level.shapes, allowRotation = level.allowRotation)
        }
        val colorProvider = if (level.colorMode == ScoringMode.CLASSIC) {
            { PieceColor.BLUE }
        } else {
            { PieceColor.random() }
        }
        val newEngine = GameEngine(level, generator, colorProvider)
        activeEngine = newEngine
        publish(newEngine.startNewGame(), newEngine, lastClear = null)
        _screen.value = Screen.Game(level)
    }

    fun place(trayIndex: Int, row: Int, col: Int) {
        val e = activeEngine ?: return
        val result = e.place(trayIndex, row, col) ?: return
        publish(e.state, e, lastClear = result)
        if (e.state.isGameOver) finishGame(e.state)
    }

    fun canPlace(trayIndex: Int, row: Int, col: Int): Boolean {
        val e = activeEngine ?: return false
        val piece = e.state.tray[trayIndex] ?: return false
        return e.state.board.canPlace(piece, row, col)
    }

    fun linesPreview(trayIndex: Int, row: Int, col: Int): Pair<Set<Int>, Set<Int>> {
        val e = activeEngine ?: return emptySet<Int>() to emptySet()
        val piece = e.state.tray[trayIndex] ?: return emptySet<Int>() to emptySet()
        return e.state.board.linesClearedByPlacing(piece, row, col)
    }

    fun rotate(trayIndex: Int) {
        val e = activeEngine ?: return
        e.rotate(trayIndex)
        publish(e.state, e, lastClear = null)
    }

    fun undo() {
        val e = activeEngine ?: return
        val restored = e.undo() ?: return
        publish(restored, e, lastClear = null)
    }

    fun playAgain() {
        val level = activeEngine?.level ?: return
        startGame(level)
    }

    /**
     * Leaves the game screen for the level list, parking the current game (if not over and it
     * has a nonzero score) so it can be resumed later. A score of 0 is treated as an accidental
     * tap-and-leave rather than a real in-progress game — not parking it means the level starts
     * fresh next time, instead of resuming a leftover engine that may reference stale rules if
     * the level gets edited in the meantime (its shapes/board size are captured at start time).
     */
    fun exitToLevelList() {
        val e = activeEngine
        if (e != null && !e.state.isGameOver && e.state.score > 0) {
            pausedEngines[e.level.tag] = e
            updateResumable()
        }
        activeEngine = null
        _gameUiState.value = null
        _screen.value = Screen.LevelList
    }

    /** Returns to the game previously parked for [tag] by [exitToLevelList], if any. */
    fun resumeGame(tag: String) {
        val e = pausedEngines.remove(tag) ?: return
        updateResumable()
        activeEngine = e
        publish(e.state, e, lastClear = null)
        _screen.value = Screen.Game(e.level)
    }

    /** Called once a game has actually ended — nothing left to park, just head back to the level list. */
    fun abandonGame() {
        activeEngine = null
        _gameUiState.value = null
        _screen.value = Screen.LevelList
    }

    fun goToMenu() {
        _screen.value = Screen.Menu
    }

    fun goToRules() {
        _screen.value = Screen.Rules
    }

    fun goToLevelList() {
        _screen.value = Screen.LevelList
    }

    fun goToConstructor() {
        _screen.value = Screen.Constructor
    }

    fun openLevelEditor(editingTag: String?) {
        _screen.value = Screen.LevelEditor(editingTag)
    }

    /**
     * Creates a new level or overwrites the one identified by [editingTag]. The tag itself is
     * assigned once (on creation) and never regenerated on edit. If any *rule* field differs
     * from the stored version (board size, color mode, algorithm, or the shape/weight set),
     * that level's record is reset — a plain rename does not touch it.
     *
     * [saveAsCopy] — when true, always saves as a brand-new level (fresh tag via
     * [LevelDefinition.nextAvailableTag]) regardless of [editingTag], leaving the original level
     * (and its record) completely untouched. `LevelEditorScreen` offers this as an alternative to
     * overwriting whenever the edited level currently has a nonzero record that the rule changes
     * would otherwise reset.
     */
    fun saveLevel(
        editingTag: String?,
        name: String,
        boardSize: Int,
        colorMode: ScoringMode,
        algorithm: GameMode,
        shapes: List<LevelShape>,
        allowRotation: Boolean,
        saveAsCopy: Boolean = false
    ) {
        viewModelScope.launch {
            val currentLevels = levels.value
            val existing = editingTag?.let { tag -> currentLevels.firstOrNull { it.tag == tag } }
            val tag = if (existing != null && !saveAsCopy) {
                existing.tag
            } else {
                val base = LevelDefinition.baseTag(colorMode, algorithm, boardSize)
                LevelDefinition.nextAvailableTag(base, currentLevels.map { it.tag })
            }
            val resolvedName = name.ifBlank { tag }
            val rulesChanged = existing != null && !saveAsCopy && (
                existing.boardSize != boardSize ||
                    existing.colorMode != colorMode ||
                    existing.algorithm != algorithm ||
                    existing.shapes != shapes ||
                    existing.allowRotation != allowRotation
                )

            levelsRepository.save(
                LevelDefinition(
                    tag = tag,
                    name = resolvedName,
                    boardSize = boardSize,
                    colorMode = colorMode,
                    algorithm = algorithm,
                    shapes = shapes,
                    allowRotation = allowRotation
                )
            )
            if (rulesChanged) recordsRepository.resetScore(tag)
            _screen.value = Screen.Constructor
        }
    }

    fun deleteLevel(tag: String) {
        viewModelScope.launch {
            levelsRepository.delete(tag)
            pausedEngines.remove(tag)
            updateResumable()
        }
    }

    private fun updateResumable() {
        _resumableLevelTags.value = pausedEngines.keys.toSet()
    }

    private fun publish(state: GameState, e: GameEngine, lastClear: PlacementResult?) {
        _gameUiState.value = GameUiState(
            board = state.board,
            tray = state.tray,
            score = state.score,
            level = state.level,
            isGameOver = state.isGameOver,
            canUndo = e.canUndo(),
            lastClear = lastClear
        )
    }

    private fun finishGame(state: GameState) {
        viewModelScope.launch {
            val isNewRecord = recordsRepository.submitScore(state.level.tag, state.score)
            _screen.value = Screen.GameOver(state.level, state.score, isNewRecord)
        }
    }
}
