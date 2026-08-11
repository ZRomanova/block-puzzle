package com.blockpuzzle.rotate.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.blockpuzzle.rotate.domain.Coordinate
import com.blockpuzzle.rotate.domain.LevelDefinition
import com.blockpuzzle.rotate.domain.Piece
import com.blockpuzzle.rotate.ui.components.BoardGrid
import com.blockpuzzle.rotate.ui.components.ShapeGlyph
import com.blockpuzzle.rotate.ui.components.TraySlot
import com.blockpuzzle.rotate.ui.components.glyphHeight
import com.blockpuzzle.rotate.ui.components.glyphWidth
import com.blockpuzzle.rotate.ui.components.rulesSummary
import com.blockpuzzle.rotate.ui.theme.HighlightClearPreview
import com.blockpuzzle.rotate.ui.theme.HighlightInvalid
import com.blockpuzzle.rotate.ui.theme.HighlightValid
import com.blockpuzzle.rotate.ui.viewmodel.GameUiState
import kotlin.math.roundToInt

private data class DragState(
    val trayIndex: Int,
    val piece: Piece,
    val positionInRoot: Offset
)

@Composable
fun GameScreen(
    uiState: GameUiState,
    record: Int,
    onPlace: (trayIndex: Int, row: Int, col: Int) -> Unit,
    onCanPlace: (trayIndex: Int, row: Int, col: Int) -> Boolean,
    onLinesPreview: (trayIndex: Int, row: Int, col: Int) -> Pair<Set<Int>, Set<Int>>,
    onRotate: (trayIndex: Int) -> Unit,
    onUndo: () -> Unit,
    onExitToMenu: () -> Unit
) {
    // Cell size adapts to the board's dimension so an 8x8 and a 6x6 board occupy roughly the same footprint.
    val cellSize = 320.dp / uiState.board.size
    val trayCellSize = 18.dp
    val traySlotSize = 96.dp
    var boardRootOrigin by remember { mutableStateOf(Offset.Zero) }
    var overlayRootOrigin by remember { mutableStateOf(Offset.Zero) }
    var dragState by remember { mutableStateOf<DragState?>(null) }

    val cellSizePx = with(androidx.compose.ui.platform.LocalDensity.current) { cellSize.toPx() }

    // Small clearance kept between the finger and the ghost's bottom edge.
    val gapAboveFingerPx = cellSizePx * 0.4f

    // Top-left corner (in root coords) the piece's bounding box should be drawn at: horizontally
    // centered on the finger, and — regardless of how tall the piece is — always fully above it.
    fun ghostTopLeftInRoot(ds: DragState): Offset {
        val rows = ds.piece.cells.maxOf { it.row } + 1
        val cols = ds.piece.cells.maxOf { it.col } + 1
        val widthPx = cols * cellSizePx
        val heightPx = rows * cellSizePx
        return Offset(
            ds.positionInRoot.x - widthPx / 2f,
            ds.positionInRoot.y - gapAboveFingerPx - heightPx
        )
    }

    fun hoverAnchor(): Coordinate? {
        val ds = dragState ?: return null
        val topLeft = ghostTopLeftInRoot(ds) - boardRootOrigin
        val anchorCol = (topLeft.x / cellSizePx).roundToInt()
        val anchorRow = (topLeft.y / cellSizePx).roundToInt()
        return Coordinate(anchorRow, anchorCol)
    }

    val highlight = remember(dragState, uiState.board) {
        val anchor = hoverAnchor()
        val ds = dragState
        if (ds == null || anchor == null) emptyMap()
        else {
            val valid = onCanPlace(ds.trayIndex, anchor.row, anchor.col)
            val cellsColor = mutableMapOf<Coordinate, Color>()
            val overlayColor = if (valid) HighlightValid else HighlightInvalid
            for (cell in ds.piece.cells) {
                cellsColor[Coordinate(cell.row + anchor.row, cell.col + anchor.col)] = overlayColor
            }
            if (valid) {
                val (rows, cols) = onLinesPreview(ds.trayIndex, anchor.row, anchor.col)
                for (r in rows) for (c in 0 until uiState.board.size) cellsColor[Coordinate(r, c)] = HighlightClearPreview
                for (c in cols) for (r in 0 until uiState.board.size) cellsColor[Coordinate(r, c)] = HighlightClearPreview
            }
            cellsColor
        }
    }

    // Brief flash on cells that were just cleared by the last placement.
    val clearFlashAlpha = remember { Animatable(0f) }
    LaunchedEffect(uiState.lastClear) {
        val clear = uiState.lastClear
        if (clear != null && clear.linesCleared > 0) {
            clearFlashAlpha.snapTo(1f)
            clearFlashAlpha.animateTo(0f, animationSpec = tween(400))
        }
    }
    val clearFlashCells = remember(uiState.lastClear) {
        val clear = uiState.lastClear
        if (clear == null) emptySet()
        else buildSet {
            for (r in clear.clearedRows) for (c in 0 until uiState.board.size) add(Coordinate(r, c))
            for (c in clear.clearedCols) for (r in 0 until uiState.board.size) add(Coordinate(r, c))
        }
    }
    val boardHighlight = if (dragState != null) {
        highlight
    } else if (clearFlashAlpha.value > 0.01f) {
        clearFlashCells.associateWith { HighlightClearPreview.copy(alpha = clearFlashAlpha.value) }
    } else {
        emptyMap()
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .onGloballyPositioned { overlayRootOrigin = it.positionInRoot() }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            GameTopBar(
                score = uiState.score,
                record = record,
                level = uiState.level,
                canUndo = uiState.canUndo,
                onUndo = onUndo,
                onExitToMenu = onExitToMenu
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                BoardGrid(
                    board = uiState.board,
                    cellSize = cellSize,
                    highlight = boardHighlight,
                    modifier = Modifier.onGloballyPositioned { boardRootOrigin = it.positionInRoot() }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
            ) {
                for (index in uiState.tray.indices) {
                    TraySlot(
                        piece = uiState.tray[index],
                        cellSize = trayCellSize,
                        shapeAreaSize = traySlotSize,
                        isDragging = dragState?.trayIndex == index,
                        showRotateButton = uiState.level.allowRotation,
                        onRotate = { onRotate(index) },
                        onDragStart = { rootPos ->
                            uiState.tray[index]?.let { piece ->
                                dragState = DragState(index, piece, rootPos)
                            }
                        },
                        onDrag = { delta ->
                            dragState = dragState?.copy(positionInRoot = dragState!!.positionInRoot + delta)
                        },
                        onDragEnd = {
                            val ds = dragState
                            val anchor = hoverAnchor()
                            if (ds != null && anchor != null && onCanPlace(ds.trayIndex, anchor.row, anchor.col)) {
                                onPlace(ds.trayIndex, anchor.row, anchor.col)
                            }
                            dragState = null
                        },
                        onDragCancel = { dragState = null }
                    )
                }
            }
        }

        // Floating ghost that follows the finger while dragging.
        dragState?.let { ds ->
            val topLeft = ghostTopLeftInRoot(ds) - overlayRootOrigin
            Box(
                modifier = Modifier.offset {
                    IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt())
                }
            ) {
                ShapeGlyph(
                    piece = ds.piece,
                    cellSize = cellSize,
                    alpha = 0.9f,
                    modifier = Modifier.size(ds.piece.glyphWidth(cellSize), ds.piece.glyphHeight(cellSize))
                )
            }
        }
    }
}

@Composable
private fun GameTopBar(
    score: Int,
    record: Int,
    level: LevelDefinition,
    canUndo: Boolean,
    onUndo: () -> Unit,
    onExitToMenu: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onExitToMenu) {
                Icon(Icons.Default.Home, contentDescription = "В меню")
            }
            Text(
                text = level.rulesSummary(),
                style = MaterialTheme.typography.labelMedium
            )
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Отменить ход",
                    tint = if (canUndo) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Once the live score overtakes the stored record, the record readout rises with it —
        // so both numbers climb together at the moment a new record is being set.
        val displayedRecord = maxOf(record, score)
        val animatedScore by androidx.compose.animation.core.animateIntAsState(targetValue = score, label = "score")
        val animatedRecord by androidx.compose.animation.core.animateIntAsState(targetValue = displayedRecord, label = "record")

        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(32.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "счёт",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(text = animatedScore.toString(), style = MaterialTheme.typography.headlineMedium)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "рекорд",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(text = animatedRecord.toString(), style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}
