package com.blockpuzzle.rotate.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import com.blockpuzzle.rotate.domain.Piece
import com.blockpuzzle.rotate.ui.theme.toComposeColor

/** Draws a piece's cells at [cellSize] each, used both inside a tray slot and as the floating drag ghost. */
@Composable
fun ShapeGlyph(
    piece: Piece,
    cellSize: Dp,
    modifier: Modifier = Modifier,
    alpha: Float = 1f
) {
    val color = piece.color.toComposeColor()
    Canvas(modifier = modifier) {
        val cellPx = cellSize.toPx()
        val gap = cellPx * 0.08f
        for (cell in piece.cells) {
            val topLeft = Offset(cell.col * cellPx + gap, cell.row * cellPx + gap)
            val side = cellPx - gap * 2
            drawRoundRect(
                color = color.copy(alpha = alpha),
                topLeft = topLeft,
                size = Size(side, side),
                cornerRadius = CornerRadius(side * 0.2f, side * 0.2f)
            )
        }
    }
}

fun Piece.glyphWidth(cellSize: Dp): Dp = cellSize * (cells.maxOf { it.col } + 1)
fun Piece.glyphHeight(cellSize: Dp): Dp = cellSize * (cells.maxOf { it.row } + 1)
