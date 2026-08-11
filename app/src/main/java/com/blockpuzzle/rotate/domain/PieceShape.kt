package com.blockpuzzle.rotate.domain

import kotlinx.serialization.Serializable

/**
 * A piece's shape: a stable [id] plus the cells it occupies before any player
 * rotation. Unlike the old closed `ShapeType` enum this replaces, it's an open
 * value type — the level constructor lets players draw their own shapes
 * alongside the legacy catalog kept below for backward compatibility.
 *
 * Mirror pairs (L/J, S/Z) are listed as distinct legacy constants and are
 * treated as fully independent shapes elsewhere in the domain — a 90/180/270
 * rotation of L never produces J, since that relationship is a reflection,
 * not a rotation, and reflections deliberately don't count as "the same
 * shape" here (see `ShapeSymmetry`'s doc comment).
 */
@Serializable
data class PieceShape(val id: String, val baseCells: List<Coordinate>) {
    val cellCount: Int get() = baseCells.size

    companion object {
        val SINGLE = PieceShape("SINGLE", listOf(Coordinate(0, 0)))
        val DOMINO = PieceShape("DOMINO", listOf(Coordinate(0, 0), Coordinate(0, 1)))
        val TRIOMINO_I = PieceShape("TRIOMINO_I", listOf(Coordinate(0, 0), Coordinate(0, 1), Coordinate(0, 2)))
        val TRIOMINO_L = PieceShape("TRIOMINO_L", listOf(Coordinate(0, 0), Coordinate(1, 0), Coordinate(1, 1)))
        val TETROMINO_O = PieceShape(
            "TETROMINO_O",
            listOf(Coordinate(0, 0), Coordinate(0, 1), Coordinate(1, 0), Coordinate(1, 1))
        )
        val TETROMINO_I = PieceShape(
            "TETROMINO_I",
            listOf(Coordinate(0, 0), Coordinate(0, 1), Coordinate(0, 2), Coordinate(0, 3))
        )
        val TETROMINO_L = PieceShape(
            "TETROMINO_L",
            listOf(Coordinate(0, 0), Coordinate(1, 0), Coordinate(2, 0), Coordinate(2, 1))
        )
        val TETROMINO_J = PieceShape(
            "TETROMINO_J",
            listOf(Coordinate(0, 1), Coordinate(1, 1), Coordinate(2, 0), Coordinate(2, 1))
        )
        val TETROMINO_T = PieceShape(
            "TETROMINO_T",
            listOf(Coordinate(0, 0), Coordinate(0, 1), Coordinate(0, 2), Coordinate(1, 1))
        )
        val TETROMINO_S = PieceShape(
            "TETROMINO_S",
            listOf(Coordinate(0, 1), Coordinate(0, 2), Coordinate(1, 0), Coordinate(1, 1))
        )
        val TETROMINO_Z = PieceShape(
            "TETROMINO_Z",
            listOf(Coordinate(0, 0), Coordinate(0, 1), Coordinate(1, 1), Coordinate(1, 2))
        )
        val PENTOMINO_PLUS = PieceShape(
            "PENTOMINO_PLUS",
            listOf(
                Coordinate(0, 1),
                Coordinate(1, 0), Coordinate(1, 1), Coordinate(1, 2),
                Coordinate(2, 1)
            )
        )
        val PENTOMINO_L = PieceShape(
            "PENTOMINO_L",
            listOf(Coordinate(0, 0), Coordinate(1, 0), Coordinate(2, 0), Coordinate(3, 0), Coordinate(3, 1))
        )
        val PENTOMINO_P = PieceShape(
            "PENTOMINO_P",
            listOf(Coordinate(0, 0), Coordinate(0, 1), Coordinate(1, 0), Coordinate(1, 1), Coordinate(2, 0))
        )
        val PENTOMINO_I = PieceShape(
            "PENTOMINO_I",
            listOf(Coordinate(0, 0), Coordinate(0, 1), Coordinate(0, 2), Coordinate(0, 3), Coordinate(0, 4))
        )
        val PENTOMINO_T = PieceShape(
            "PENTOMINO_T",
            listOf(Coordinate(0, 0), Coordinate(0, 1), Coordinate(0, 2), Coordinate(1, 1), Coordinate(2, 1))
        )

        /** The 15 shapes the game shipped with before the level constructor existed. */
        val LEGACY_CATALOG: List<PieceShape> = listOf(
            SINGLE, DOMINO, TRIOMINO_I, TRIOMINO_L,
            TETROMINO_O, TETROMINO_I, TETROMINO_L, TETROMINO_J, TETROMINO_T, TETROMINO_S, TETROMINO_Z,
            PENTOMINO_PLUS, PENTOMINO_L, PENTOMINO_P, PENTOMINO_I, PENTOMINO_T
        )

        /** Fresh id for a shape drawn by the user in the level constructor. */
        fun newCustomId(): String = "custom-${System.nanoTime()}"
    }
}
