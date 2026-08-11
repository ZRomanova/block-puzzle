package com.blockpuzzle.rotate.domain

import kotlin.random.Random

/**
 * Hard mode piece generator: instead of picking uniformly at random, it looks
 * ahead at candidate pieces for the empty tray slot and picks the one that
 * makes the board hardest to keep playing, without (usually) making the game
 * unwinnable in the very next move.
 *
 * Algorithm, run every time a new tray piece is needed:
 *  1. Sample a handful of candidate shapes.
 *  2. For each candidate, look at the *trio* = the two pieces still sitting in
 *     the tray + this candidate, and simulate:
 *       - [totalValidPlacements]: how many (piece, position) placements are
 *         legal anywhere on the board for that trio.
 *       - [uncoveredCells]: how many currently-empty cells aren't reachable by
 *         *any* placement of *any* piece in the trio — cells that would be
 *         stranded no matter what the player does with this tray.
 *  3. Discard candidates that would drop total valid placements to (near)
 *     zero — that's an instant, unavoidable game over — unless every
 *     candidate does, in which case a loss is unavoidable and we might as
 *     well accept it.
 *  4. Among what's left, pick the candidate that minimizes valid placements,
 *     tie-broken by maximizing stranded cells — i.e. the "meanest" option
 *     that still leaves the player a fighting chance.
 *
 * Pure domain logic, no UI/Android dependency, so it's easy to unit test in isolation.
 *
 * When the level disallows rotation, each candidate's orientation is randomized the same way
 * [EasyPieceGenerator] does ([initialRotationSteps]) before being evaluated — simplest way to
 * keep every reachable orientation a theoretical possibility over repeated calls, without
 * restructuring the sampling step to enumerate all of a shape's orientations as separate
 * candidates.
 */
class HardModePieceSelector(
    private val shapePool: List<LevelShape> = PieceShape.LEGACY_CATALOG.map { LevelShape(it) },
    private val candidateSampleSize: Int = 6,
    private val minPlayableMoves: Int = 3,
    private val random: Random = Random.Default,
    private val allowRotation: Boolean = true,
    private val allowMirror: Boolean = true
) : PieceGenerator {

    internal data class Candidate(
        val piece: Piece,
        val totalValidPlacements: Int,
        val uncoveredCells: Int
    )

    override fun nextPiece(board: Board, remainingTrayPieces: List<Piece>): Piece {
        val candidateEntries =
            if (shapePool.size <= candidateSampleSize) shapePool
            else shapePool.shuffled(random).take(candidateSampleSize)

        val candidates = candidateEntries.map { entry ->
            val candidatePiece = Piece(
                id = "hard-${random.nextLong()}",
                shape = PieceShape(entry.shape.id, entry.resolveCells(random, allowMirror)),
                color = PieceColor.entries[random.nextInt(PieceColor.entries.size)],
                rotationSteps = initialRotationSteps(allowRotation, random)
            )
            evaluate(board, remainingTrayPieces, candidatePiece)
        }

        val playable = candidates.filter { it.totalValidPlacements >= minPlayableMoves }
        val pool = playable.ifEmpty { candidates }

        val chosen = pool.minWithOrNull(
            compareBy<Candidate> { it.totalValidPlacements }
                .thenByDescending { it.uncoveredCells }
        ) ?: candidates.first()

        return chosen.piece
    }

    internal fun evaluate(board: Board, remainingTrayPieces: List<Piece>, candidate: Piece): Candidate {
        val trio = remainingTrayPieces + candidate
        val placementsByPiece = trio.associateWith { board.validPlacements(it) }
        val totalValidPlacements = placementsByPiece.values.sumOf { it.size }

        val covered = HashSet<Coordinate>()
        for ((piece, anchors) in placementsByPiece) {
            for (anchor in anchors) {
                for (cell in piece.cells) {
                    covered.add(Coordinate(cell.row + anchor.row, cell.col + anchor.col))
                }
            }
        }

        var uncovered = 0
        for (row in 0 until board.size) {
            for (col in 0 until board.size) {
                if (board.isEmpty(row, col) && Coordinate(row, col) !in covered) uncovered++
            }
        }

        return Candidate(candidate, totalValidPlacements, uncovered)
    }
}
