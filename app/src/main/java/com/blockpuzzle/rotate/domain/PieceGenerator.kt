package com.blockpuzzle.rotate.domain

/**
 * Produces one new tray piece at a time. [remainingTrayPieces] are the pieces
 * still sitting in the other tray slots, which a "smart" generator (hard mode)
 * needs in order to reason about the trio as a whole.
 */
interface PieceGenerator {
    fun nextPiece(board: Board, remainingTrayPieces: List<Piece>): Piece
}

/**
 * The `rotationSteps` a freshly spawned piece should start with. When rotation is allowed the
 * player fixes the orientation themselves, so it never matters which one a piece spawns in — 0
 * always. When it's disallowed, the spawned orientation is final for that piece's whole
 * lifetime, so it's picked uniformly from all 4 raw steps: [Piece.cells] normalizes the result,
 * so a shape with rotational symmetry naturally lands on fewer *distinct* outcomes (e.g. 2 for a
 * straight domino) with those outcomes still coming up equally often — no separate dedup needed.
 */
internal fun initialRotationSteps(allowRotation: Boolean, random: kotlin.random.Random): Int =
    if (allowRotation) 0 else random.nextInt(4)
