package com.blockpuzzle.rotate.domain

/** Случайный (random) algorithm: pieces are chosen at random, weighted by each shape's configured weight, oblivious to board state. */
class EasyPieceGenerator(
    private val shapePool: List<LevelShape> = PieceShape.LEGACY_CATALOG.map { LevelShape(it) },
    private val random: kotlin.random.Random = kotlin.random.Random.Default,
    private val allowRotation: Boolean = true
) : PieceGenerator {

    override fun nextPiece(board: Board, remainingTrayPieces: List<Piece>): Piece {
        val chosen = pickWeighted(shapePool, random)
        return Piece(
            id = "p-${random.nextLong()}",
            shape = chosen.shape,
            color = PieceColor.entries[random.nextInt(PieceColor.entries.size)],
            rotationSteps = initialRotationSteps(allowRotation, random)
        )
    }

    companion object {
        internal fun pickWeighted(pool: List<LevelShape>, random: kotlin.random.Random): LevelShape {
            val totalWeight = pool.sumOf { it.weight }
            var roll = random.nextInt(totalWeight)
            for (entry in pool) {
                roll -= entry.weight
                if (roll < 0) return entry
            }
            return pool.last()
        }
    }
}
