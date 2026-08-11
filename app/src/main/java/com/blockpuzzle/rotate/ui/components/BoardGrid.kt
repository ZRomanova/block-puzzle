package com.blockpuzzle.rotate.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.blockpuzzle.rotate.domain.Board
import com.blockpuzzle.rotate.domain.Coordinate
import com.blockpuzzle.rotate.ui.theme.BoardCellBorder
import com.blockpuzzle.rotate.ui.theme.BoardCellEmpty
import com.blockpuzzle.rotate.ui.theme.toComposeColor

/** Renders the 8x8 board plus an optional per-cell highlight overlay (drag preview / clear preview). */
@Composable
fun BoardGrid(
    board: Board,
    cellSize: Dp,
    highlight: Map<Coordinate, Color> = emptyMap(),
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(cellSize * board.size)) {
        val cellPx = cellSize.toPx()
        val gap = cellPx * 0.06f
        for (row in 0 until board.size) {
            for (col in 0 until board.size) {
                val topLeft = Offset(col * cellPx, row * cellPx)

                val fillColor = board.cellAt(row, col)?.toComposeColor() ?: BoardCellEmpty
                drawRoundRect(
                    color = fillColor,
                    topLeft = topLeft + Offset(gap, gap),
                    size = Size(cellPx - gap * 2, cellPx - gap * 2),
                    cornerRadius = CornerRadius(cellPx * 0.15f, cellPx * 0.15f)
                )

                highlight[Coordinate(row, col)]?.let { overlayColor ->
                    drawRoundRect(
                        color = overlayColor,
                        topLeft = topLeft + Offset(gap, gap),
                        size = Size(cellPx - gap * 2, cellPx - gap * 2),
                        cornerRadius = CornerRadius(cellPx * 0.15f, cellPx * 0.15f)
                    )
                }

                drawRect(
                    color = BoardCellBorder,
                    topLeft = topLeft,
                    size = Size(cellPx, cellPx),
                    style = Stroke(width = 1f)
                )
            }
        }
    }
}
