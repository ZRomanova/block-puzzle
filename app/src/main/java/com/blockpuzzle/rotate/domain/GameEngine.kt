package com.blockpuzzle.rotate.domain

/**
 * Pure, UI-free game session: owns the current [GameState], applies moves and
 * refills, and keeps an in-memory undo stack. No persistence here — the
 * ViewModel is responsible for reading/writing the high score via DataStore.
 *
 * Tray refills happen in batches of 3, not one at a time: placing a piece just
 * empties its slot, and a fresh trio only appears once all three slots are
 * empty. This is what makes losing possible — with an immediate one-for-one
 * refill there's always a fresh roll to bail you out.
 *
 * [colorProvider] decides the color of each newly generated piece — the CLASSIC
 * scoring mode wants every piece the same (user-chosen) color, COLOR_BONUS wants
 * them random. Whatever color [pieceGenerator] itself put on the piece is
 * provisional and gets overwritten here, so the shape-selection algorithms stay
 * free of any color/scoring concern.
 */
class GameEngine(
    val level: LevelDefinition,
    private val pieceGenerator: PieceGenerator,
    private val colorProvider: () -> PieceColor = { PieceColor.random() }
) {
    private val history = ArrayDeque<GameState>()

    var state: GameState = GameState(board = Board(level.boardSize), level = level)
        private set

    private fun nextColoredPiece(board: Board, remainingTrayPieces: List<Piece>): Piece =
        pieceGenerator.nextPiece(board, remainingTrayPieces).copy(color = colorProvider())

    fun startNewGame(): GameState {
        history.clear()
        var s = GameState(board = Board(state.board.size), level = level)
        for (i in 0..2) {
            val piece = nextColoredPiece(s.board, s.trayPieces)
            s = s.copy(tray = s.tray.toMutableList().also { it[i] = piece })
        }
        state = s
        return state
    }

    fun canUndo(): Boolean = history.isNotEmpty()

    /**
     * Reverts to the prior [GameState] and additionally deducts [LevelDefinition.undoPenaltyPercent]
     * percent of the score at the moment undo was pressed (not the restored state's own score) —
     * see [ScoringConfig.undoPenalty]. Never lets the score drop below 0.
     */
    fun undo(): GameState? {
        val previous = history.removeLastOrNull() ?: return null
        val penalty = ScoringConfig.undoPenalty(state.score, level.undoPenaltyPercent)
        state = previous.copy(score = (previous.score - penalty).coerceAtLeast(0))
        return state
    }

    /** No-op when [LevelDefinition.allowRotation] is false — the domain layer is the single source of truth for this, not just the UI hiding the button. */
    fun rotate(trayIndex: Int): GameState {
        if (!level.allowRotation) return state
        val piece = state.tray[trayIndex] ?: return state
        val newTray = state.tray.toMutableList().also { it[trayIndex] = piece.rotatedClockwise() }
        state = state.copy(tray = newTray)
        return state
    }

    /** Attempts to place the piece at [trayIndex] with its top-left cell at (anchorRow, anchorCol). */
    fun place(trayIndex: Int, anchorRow: Int, anchorCol: Int): PlacementResult? {
        if (state.isGameOver) return null
        val piece = state.tray[trayIndex] ?: return null
        if (!state.board.canPlace(piece, anchorRow, anchorCol)) return null

        history.addLast(state)

        val result = state.board.place(piece, anchorRow, anchorCol)
        var scoreGain = ScoringConfig.scoreForMove(result.cellsPlaced, result.linesCleared)
        if (level.colorMode == ScoringMode.COLOR_BONUS) {
            scoreGain += ScoringConfig.colorBonusForMove(result.clearedLineColors)
        }

        val newTray = state.tray.toMutableList().also { it[trayIndex] = null }
        if (newTray.all { it == null }) {
            for (i in newTray.indices) {
                newTray[i] = nextColoredPiece(result.board, newTray.filterNotNull())
            }
        }

        val newState = GameState(
            board = result.board,
            tray = newTray,
            score = state.score + scoreGain,
            level = level,
            isGameOver = false
        ).let { it.copy(isGameOver = it.computeGameOver()) }

        state = newState
        return result
    }
}
