package com.blockpuzzle.rotate.domain

/**
 * Full snapshot of a game in progress. Immutable — every move produces a new
 * instance, which is what makes the undo stack ([com.blockpuzzle.rotate.domain.GameEngine])
 * trivial: it's just a list of these.
 */
data class GameState(
    val board: Board,
    val level: LevelDefinition,
    val tray: List<Piece?> = listOf(null, null, null),
    val score: Int = 0,
    val isGameOver: Boolean = false
) {
    val trayPieces: List<Piece> get() = tray.filterNotNull()

    fun isTraySlotEmpty(index: Int): Boolean = tray[index] == null

    /** True once none of the remaining tray pieces fit anywhere on the board. */
    fun computeGameOver(): Boolean =
        trayPieces.isNotEmpty() && trayPieces.none { board.hasAnyValidPlacement(it) }
}
