package com.blockpuzzle.rotate.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockpuzzle.rotate.domain.Piece

/**
 * One of the 3 tray slots: the piece (if any) on top, a rotate button below it.
 *
 * The drag-gesture detector is keyed on [piece]'s id only, so it survives a
 * rotation (rotating keeps the same id) without restarting mid-gesture. But
 * that also means its captured lambdas would otherwise go stale the moment
 * the piece rotates — [rememberUpdatedState] keeps every callback (and the
 * piece reference used for the null-check) reading the latest value even
 * though the underlying coroutine never relaunches.
 */
@Composable
fun TraySlot(
    piece: Piece?,
    cellSize: Dp,
    shapeAreaSize: Dp,
    isDragging: Boolean,
    onRotate: () -> Unit,
    onDragStart: (rootPosition: Offset) -> Unit,
    onDrag: (delta: Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rootPosition by remember { mutableStateOf(Offset.Zero) }

    val latestOnDragStart by rememberUpdatedState(onDragStart)
    val latestOnDrag by rememberUpdatedState(onDrag)
    val latestOnDragEnd by rememberUpdatedState(onDragEnd)
    val latestOnDragCancel by rememberUpdatedState(onDragCancel)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(shapeAreaSize)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .onGloballyPositioned { rootPosition = it.positionInRoot() }
                .pointerInput(piece?.id) {
                    if (piece == null) return@pointerInput
                    detectDragGestures(
                        onDragStart = { local -> latestOnDragStart(rootPosition + local) },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            latestOnDrag(dragAmount)
                        },
                        onDragEnd = { latestOnDragEnd() },
                        onDragCancel = { latestOnDragCancel() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val currentPiece = piece
            if (currentPiece != null) {
                // Quick spin-and-settle flourish each time the piece is rotated; the shape's
                // cell layout has already snapped to the new orientation, this is just juice.
                val rotationWobble = remember(currentPiece.id) { Animatable(0f) }
                LaunchedEffect(currentPiece.rotationSteps) {
                    rotationWobble.snapTo(24f)
                    rotationWobble.animateTo(0f, animationSpec = tween(220))
                }
                ShapeGlyph(
                    piece = currentPiece,
                    cellSize = cellSize,
                    alpha = if (isDragging) 0.25f else 1f,
                    modifier = Modifier
                        .graphicsLayer { rotationZ = rotationWobble.value }
                        .size(currentPiece.glyphWidth(cellSize), currentPiece.glyphHeight(cellSize))
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        IconButton(
            onClick = onRotate,
            enabled = piece != null,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Повернуть фигуру",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (piece != null) 0.7f else 0.2f)
            )
        }
    }
}
