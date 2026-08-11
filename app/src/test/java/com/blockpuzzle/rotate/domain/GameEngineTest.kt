package com.blockpuzzle.rotate.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A generator that always hands out a fixed sequence of shapes, for deterministic tests. Color
 *  is irrelevant here — GameEngine always overwrites it via its own colorProvider. */
private class FixedPieceGenerator(private val sequence: List<PieceShape>) : PieceGenerator {
    private var index = 0
    override fun nextPiece(board: Board, remainingTrayPieces: List<Piece>): Piece {
        val shape = sequence[index % sequence.size]
        index++
        return Piece(id = "fixed-$index", shape = shape, color = PieceColor.BLUE)
    }
}

private fun testLevel(
    boardSize: Int = 8,
    colorMode: ScoringMode = ScoringMode.CLASSIC,
    algorithm: GameMode = GameMode.EASY,
    allowRotation: Boolean = true,
    undoPenaltyPercent: Int = ScoringConfig.DEFAULT_UNDO_PENALTY_PERCENT
) = LevelDefinition(
    tag = "test",
    name = "test",
    boardSize = boardSize,
    colorMode = colorMode,
    algorithm = algorithm,
    shapes = PieceShape.LEGACY_CATALOG.map { LevelShape(it) },
    allowRotation = allowRotation,
    undoPenaltyPercent = undoPenaltyPercent
)

class GameEngineTest {

    @Test
    fun `starting a game fills all three tray slots`() {
        val engine = GameEngine(testLevel(), FixedPieceGenerator(listOf(PieceShape.SINGLE)))
        val state = engine.startNewGame()
        assertEquals(3, state.trayPieces.size)
    }

    @Test
    fun `board size follows the level`() {
        val bigEngine = GameEngine(testLevel(), FixedPieceGenerator(listOf(PieceShape.SINGLE)))
        val smallEngine = GameEngine(testLevel(boardSize = 6), FixedPieceGenerator(listOf(PieceShape.SINGLE)))
        assertEquals(8, bigEngine.startNewGame().board.size)
        assertEquals(6, smallEngine.startNewGame().board.size)
    }

    @Test
    fun `colorProvider decides every generated piece's color`() {
        val engine = GameEngine(testLevel(), FixedPieceGenerator(listOf(PieceShape.SINGLE)), colorProvider = { PieceColor.GREEN })
        val state = engine.startNewGame()
        assertTrue(state.trayPieces.all { it.color == PieceColor.GREEN })

        // Use up the whole batch to trigger a refill, and check the new batch too.
        engine.place(0, 0, 0)
        engine.place(1, 0, 1)
        engine.place(2, 0, 2)
        assertTrue(engine.state.trayPieces.all { it.color == PieceColor.GREEN })
    }

    @Test
    fun `placing one piece just empties its slot and scores points`() {
        val engine = GameEngine(testLevel(), FixedPieceGenerator(listOf(PieceShape.SINGLE)))
        val before = engine.startNewGame()
        val originalOtherPiece = before.tray[1]

        val result = engine.place(0, 0, 0)

        assertNotNull(result)
        val after = engine.state
        assertNull(after.tray[0])
        assertEquals(originalOtherPiece, after.tray[1])
        assertEquals(ScoringConfig.scoreForMove(1, 0), after.score)
    }

    @Test
    fun `a fresh trio only appears once all three tray slots have been used`() {
        val engine = GameEngine(testLevel(), FixedPieceGenerator(listOf(PieceShape.SINGLE)))
        val initial = engine.startNewGame()
        val originalP1 = initial.tray[1]
        val originalP2 = initial.tray[2]

        engine.place(0, 0, 0)
        assertNull(engine.state.tray[0])
        assertEquals(originalP1, engine.state.tray[1])
        assertEquals(originalP2, engine.state.tray[2])

        engine.place(1, 0, 1)
        assertNull(engine.state.tray[0])
        assertNull(engine.state.tray[1])
        assertEquals(originalP2, engine.state.tray[2])

        engine.place(2, 0, 2)
        // Batch complete: all three slots refill together.
        assertTrue(engine.state.tray.all { it != null })
    }

    @Test
    fun `illegal placement is rejected and state unchanged`() {
        val engine = GameEngine(testLevel(), FixedPieceGenerator(listOf(PieceShape.SINGLE)))
        engine.startNewGame()
        engine.place(0, 0, 0)
        val stateBefore = engine.state
        val result = engine.place(1, 0, 0) // already occupied by slot 0's piece
        assertNull(result)
        assertEquals(stateBefore, engine.state)
    }

    @Test
    fun `rotate updates only the targeted tray slot`() {
        val engine = GameEngine(testLevel(), FixedPieceGenerator(listOf(PieceShape.TETROMINO_L)))
        engine.startNewGame()
        val before = engine.state.tray[0]!!
        engine.rotate(0)
        val after = engine.state.tray[0]!!
        assertEquals((before.rotationSteps + 1) % 4, after.rotationSteps)
    }

    @Test
    fun `rotate is a no-op when the level disallows rotation`() {
        val engine = GameEngine(testLevel(allowRotation = false), FixedPieceGenerator(listOf(PieceShape.TETROMINO_L)))
        engine.startNewGame()
        val before = engine.state.tray[0]!!
        engine.rotate(0)
        val after = engine.state.tray[0]!!
        assertEquals(before.rotationSteps, after.rotationSteps)
    }

    @Test
    fun `undo restores board, tray and score`() {
        val engine = GameEngine(testLevel(), FixedPieceGenerator(listOf(PieceShape.SINGLE)))
        val initial = engine.startNewGame()
        assertFalse(engine.canUndo())

        engine.place(0, 0, 0)
        assertTrue(engine.canUndo())

        val restored = engine.undo()
        assertEquals(initial, restored)
        assertFalse(engine.canUndo())
    }

    @Test
    fun `undo deducts a percentage of the score at the moment undo is pressed`() {
        val engine = GameEngine(testLevel(undoPenaltyPercent = 50), FixedPieceGenerator(listOf(PieceShape.SINGLE)))
        engine.startNewGame()
        engine.place(0, 0, 0) // score 0 -> 1
        engine.place(1, 0, 1) // score 1 -> 2
        engine.place(2, 0, 2) // score 2 -> 3, batch refills here

        // Penalty = 50% of 3 (the score right before undo) = 1, taken off the destination
        // state's own score of 2, landing on 1 — not simply 50% of the destination score.
        val restored = engine.undo()
        assertEquals(1, restored?.score)
        assertEquals(1, engine.state.score)
    }

    @Test
    fun `undo penalty never drops the score below zero`() {
        val engine = GameEngine(testLevel(undoPenaltyPercent = 100), FixedPieceGenerator(listOf(PieceShape.SINGLE)))
        engine.startNewGame()
        engine.place(0, 0, 0) // score 0 -> 1
        engine.place(1, 0, 1) // score 1 -> 2

        // Penalty = 100% of 2 = 2, which would take the destination score of 1 negative.
        val restored = engine.undo()
        assertEquals(0, restored?.score)
    }

    @Test
    fun `a 0 percent undo penalty leaves the restored score untouched`() {
        val engine = GameEngine(testLevel(undoPenaltyPercent = 0), FixedPieceGenerator(listOf(PieceShape.SINGLE)))
        engine.startNewGame()
        engine.place(0, 0, 0) // score 0 -> 1
        engine.place(1, 0, 1) // score 1 -> 2

        val restored = engine.undo()
        assertEquals(1, restored?.score)
    }

    @Test
    fun `game over is detected when no tray piece fits`() {
        // Fill the board leaving only isolated single-cell gaps (diagonal), then hand
        // out a domino, which cannot fit anywhere.
        val engine = GameEngine(testLevel(), FixedPieceGenerator(listOf(PieceShape.SINGLE)))
        engine.startNewGame()

        // Every row/column always keeps exactly one empty cell (its diagonal position), so
        // no row or column is ever fully completed and nothing auto-clears while building this.
        var board = Board()
        val single = Piece(id = "s", shape = PieceShape.SINGLE, color = PieceColor.RED)
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                if (row == col) continue
                board = board.place(single, row, col).board
            }
        }

        val dominoEngine = GameEngine(testLevel(), FixedPieceGenerator(listOf(PieceShape.DOMINO)))
        dominoEngine.startNewGame()
        // Reach into the private state via a legal single placement isn't representative
        // of a full board, so directly verify Board + GameState game-over computation instead.
        val trayWithOnlyDomino = listOf(dominoEngine.state.tray[0], null, null)
        val gs = GameState(board = board, tray = trayWithOnlyDomino, level = testLevel())
        assertTrue(gs.computeGameOver())
    }

    @Test
    fun `COLOR_BONUS level awards extra points for a monochrome line clear, CLASSIC does not`() {
        // Both engines place 8 RED singles across row 0 through the real tray API; the 8th
        // placement completes and clears the row. Only the COLOR_BONUS engine should score extra for it.
        val classicEngine = GameEngine(testLevel(), FixedPieceGenerator(listOf(PieceShape.SINGLE)), colorProvider = { PieceColor.RED })
        val colorEngine = GameEngine(
            testLevel(colorMode = ScoringMode.COLOR_BONUS),
            FixedPieceGenerator(listOf(PieceShape.SINGLE)),
            colorProvider = { PieceColor.RED }
        )

        for (engine in listOf(classicEngine, colorEngine)) {
            engine.startNewGame()
            // Cycle through the 3 tray slots so each placement is always legal, regardless
            // of when a slot happens to be empty waiting for its batch to complete.
            for (col in 0 until 8) engine.place(col % 3, 0, col)
        }

        val expectedBonus = ScoringConfig.colorBonusForLine(List(8) { PieceColor.RED })
        assertEquals(colorEngine.state.score, classicEngine.state.score + expectedBonus)
    }

    @Test
    fun `colorBonusForLine rewards a fully monochrome line more than a mixed one`() {
        val monochromeLine = List(8) { PieceColor.RED }
        val mixedLine = listOf(
            PieceColor.RED, PieceColor.BLUE, PieceColor.RED, PieceColor.GREEN,
            PieceColor.RED, PieceColor.YELLOW, PieceColor.RED, PieceColor.PURPLE
        )
        assertTrue(ScoringConfig.colorBonusForLine(monochromeLine) > ScoringConfig.colorBonusForLine(mixedLine))
        assertEquals(0, ScoringConfig.colorBonusForLine(mixedLine.distinct()))
    }
}
