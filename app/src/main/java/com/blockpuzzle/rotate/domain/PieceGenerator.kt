package com.blockpuzzle.rotate.domain

/**
 * Produces one new tray piece at a time. [remainingTrayPieces] are the pieces
 * still sitting in the other tray slots, which a "smart" generator (hard mode)
 * needs in order to reason about the trio as a whole.
 */
interface PieceGenerator {
    fun nextPiece(board: Board, remainingTrayPieces: List<Piece>): Piece
}
