package com.blockpuzzle.rotate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.blockpuzzle.rotate.domain.Coordinate
import com.blockpuzzle.rotate.domain.GameMode
import com.blockpuzzle.rotate.domain.LevelDefinition
import com.blockpuzzle.rotate.domain.LevelShape
import com.blockpuzzle.rotate.domain.Piece
import com.blockpuzzle.rotate.domain.PieceColor
import com.blockpuzzle.rotate.domain.PieceShape
import com.blockpuzzle.rotate.domain.ScoringConfig
import com.blockpuzzle.rotate.domain.ScoringMode
import com.blockpuzzle.rotate.domain.ShapeConnectivity
import com.blockpuzzle.rotate.domain.ShapeSymmetry
import com.blockpuzzle.rotate.ui.components.LabeledToggleRow
import com.blockpuzzle.rotate.ui.components.ShapeGlyph
import com.blockpuzzle.rotate.ui.components.glyphHeight
import com.blockpuzzle.rotate.ui.components.glyphWidth
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val DEFAULT_BOARD_SIZE = 6
private val editorJson = Json { ignoreUnknownKeys = true }

/**
 * One scrollable screen, no wizard, for creating or editing a level — name, board size,
 * color mode, algorithm, then the shape pool. All scalar fields use [rememberSaveable] and the
 * shape list is kept as a saveable JSON string (it's already `@Serializable` for persistence),
 * so an in-progress draft survives a screen rotation.
 */
@Composable
fun LevelEditorScreen(
    editingLevel: LevelDefinition?,
    record: Int,
    onBack: () -> Unit,
    onSave: (name: String, boardSize: Int, colorMode: ScoringMode, algorithm: GameMode, shapes: List<LevelShape>, allowRotation: Boolean, undoPenaltyPercent: Int, saveAsCopy: Boolean) -> Unit
) {
    val draftKey = editingLevel?.tag
    var name by rememberSaveable(draftKey) { mutableStateOf(editingLevel?.name ?: "") }
    var boardSize by rememberSaveable(draftKey) { mutableStateOf(editingLevel?.boardSize ?: DEFAULT_BOARD_SIZE) }
    var colorModeName by rememberSaveable(draftKey) { mutableStateOf((editingLevel?.colorMode ?: ScoringMode.CLASSIC).name) }
    var algorithmName by rememberSaveable(draftKey) { mutableStateOf((editingLevel?.algorithm ?: GameMode.EASY).name) }
    var allowRotation by rememberSaveable(draftKey) { mutableStateOf(editingLevel?.allowRotation ?: true) }
    var undoPenaltyPercent by rememberSaveable(draftKey) {
        mutableStateOf(editingLevel?.undoPenaltyPercent ?: ScoringConfig.DEFAULT_UNDO_PENALTY_PERCENT)
    }
    var shapesJson by rememberSaveable(draftKey) { mutableStateOf(editorJson.encodeToString(editingLevel?.shapes ?: emptyList())) }
    var showShapeDialog by rememberSaveable(draftKey) { mutableStateOf(false) }
    var shapesRemovedNotice by rememberSaveable(draftKey) { mutableStateOf<Int?>(null) }
    var showRecordChoiceDialog by rememberSaveable(draftKey) { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val colorMode = ScoringMode.valueOf(colorModeName)
    val algorithm = GameMode.valueOf(algorithmName)
    val shapes = remember(shapesJson) { editorJson.decodeFromString<List<LevelShape>>(shapesJson) }
    fun replaceShapes(newShapes: List<LevelShape>) {
        shapesJson = editorJson.encodeToString(newShapes)
    }

    /** A shape fits a board size if its bounding box is within Trunc(boardSize*0.8) on both axes. */
    fun fitsBoardSize(shape: PieceShape, size: Int): Boolean {
        val gridSize = LevelDefinition.editableGridSize(size)
        val height = shape.baseCells.maxOf { it.row } + 1
        val width = shape.baseCells.maxOf { it.col } + 1
        return height <= gridSize && width <= gridSize
    }

    fun changeBoardSize(newSize: Int) {
        val fitting = shapes.filter { fitsBoardSize(it.shape, newSize) }
        val removedCount = shapes.size - fitting.size
        boardSize = newSize
        if (removedCount > 0) {
            replaceShapes(fitting)
            shapesRemovedNotice = removedCount
        } else {
            shapesRemovedNotice = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
            Text(
                if (editingLevel == null) "Новый уровень" else "Редактирование уровня",
                style = MaterialTheme.typography.headlineSmall
            )
        }
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Название") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                keyboardController?.hide()
            }),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))

        LabeledToggleRow(
            label = "Поле",
            options = LevelDefinition.ALLOWED_BOARD_SIZES.toList(),
            selected = boardSize,
            optionText = { "$it×$it" },
            onSelect = { changeBoardSize(it) }
        )
        shapesRemovedNotice?.let { count ->
            Spacer(Modifier.height(4.dp))
            Text(
                "Убрано фигур, не поместившихся в новый размер поля: $count",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.height(20.dp))

        LabeledToggleRow(
            label = "Режим",
            options = listOf(ScoringMode.CLASSIC, ScoringMode.COLOR_BONUS),
            selected = colorMode,
            optionText = { if (it == ScoringMode.CLASSIC) "Однотонный" else "Цветной" },
            onSelect = { colorModeName = it.name }
        )
        Spacer(Modifier.height(20.dp))

        LabeledToggleRow(
            label = "Алгоритм",
            options = listOf(GameMode.EASY, GameMode.HARD),
            selected = algorithm,
            optionText = { if (it == GameMode.EASY) "Случайный" else "Хитрый" },
            onSelect = { algorithmName = it.name }
        )
        Spacer(Modifier.height(20.dp))

        LabeledToggleRow(
            label = "Вращение",
            options = listOf(true, false),
            selected = allowRotation,
            optionText = { if (it) "Включено" else "Выключено" },
            onSelect = { allowRotation = it }
        )
        Spacer(Modifier.height(20.dp))

        Text("Штраф за отмену хода (undo)", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { undoPenaltyPercent = (undoPenaltyPercent - 5).coerceAtLeast(0) }) {
                Icon(Icons.Default.Remove, contentDescription = "Меньше")
            }
            Text("$undoPenaltyPercent%", style = MaterialTheme.typography.bodyMedium)
            IconButton(onClick = { undoPenaltyPercent = (undoPenaltyPercent + 5).coerceAtMost(100) }) {
                Icon(Icons.Default.Add, contentDescription = "Больше")
            }
        }
        Spacer(Modifier.height(24.dp))

        Text("Фигуры", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))

        if (shapes.isEmpty()) {
            Text(
                "Добавьте хотя бы одну фигуру",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(8.dp))
        } else {
            val totalWeight = shapes.sumOf { it.weight }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                shapes.forEachIndexed { index, levelShape ->
                    ShapeRow(
                        levelShape = levelShape,
                        percent = if (algorithm == GameMode.EASY && totalWeight > 0) levelShape.weight * 100 / totalWeight else null,
                        onWeightChange = { newWeight ->
                            replaceShapes(shapes.toMutableList().also { it[index] = levelShape.copy(weight = newWeight) })
                        },
                        onDelete = { replaceShapes(shapes.toMutableList().also { it.removeAt(index) }) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        OutlinedButton(onClick = { showShapeDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Добавить фигуру")
        }

        val unlosable = LevelDefinition.isUnlosable(shapes, boardSize, allowRotation)
        if (unlosable) {
            Spacer(Modifier.height(8.dp))
            Text(
                if (shapes.all { it.shape.cellCount <= 1 }) {
                    "Нужна хотя бы одна фигура крупнее одной клетки — иначе уровень невозможно проиграть"
                } else {
                    "На поле $boardSize×$boardSize с вращением фигуры только по 2 клетки почти гарантированно не дадут проиграть — добавьте фигуру покрупнее"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(32.dp))

        val rulesChanged = editingLevel != null && (
            editingLevel.boardSize != boardSize ||
                editingLevel.colorMode != colorMode ||
                editingLevel.algorithm != algorithm ||
                editingLevel.shapes != shapes ||
                editingLevel.allowRotation != allowRotation ||
                editingLevel.undoPenaltyPercent != undoPenaltyPercent
            )
        val hasRecordAtRisk = editingLevel != null && record > 0 && rulesChanged

        Button(
            onClick = {
                if (hasRecordAtRisk) {
                    showRecordChoiceDialog = true
                } else {
                    onSave(name, boardSize, colorMode, algorithm, shapes, allowRotation, undoPenaltyPercent, false)
                }
            },
            enabled = shapes.isNotEmpty() && !unlosable,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сохранить")
        }
    }

    if (showShapeDialog) {
        ShapeDrawDialog(
            gridSize = LevelDefinition.editableGridSize(boardSize),
            existingShapes = shapes,
            onDismiss = { showShapeDialog = false },
            onConfirm = { newShape ->
                replaceShapes(shapes + LevelShape(newShape))
                showShapeDialog = false
            }
        )
    }

    if (showRecordChoiceDialog && editingLevel != null) {
        RecordAtRiskDialog(
            levelName = editingLevel.name,
            record = record,
            onDismiss = { showRecordChoiceDialog = false },
            onOverwrite = {
                showRecordChoiceDialog = false
                onSave(name, boardSize, colorMode, algorithm, shapes, allowRotation, undoPenaltyPercent, false)
            },
            onSaveAsCopy = {
                showRecordChoiceDialog = false
                val copyName = if (name == editingLevel.name) "$name (копия)" else name
                onSave(copyName, boardSize, colorMode, algorithm, shapes, allowRotation, undoPenaltyPercent, true)
            }
        )
    }
}

@Composable
private fun RecordAtRiskDialog(
    levelName: String,
    record: Int,
    onDismiss: () -> Unit,
    onOverwrite: () -> Unit,
    onSaveAsCopy: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Изменить правила уровня?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "У уровня «$levelName» есть рекорд ($record). Изменение поля, режима, " +
                        "алгоритма или фигур сбросит его. Можно вместо этого сохранить новые " +
                        "настройки как отдельную копию — тогда старый уровень с рекордом " +
                        "останется как есть.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(20.dp))

                Button(onClick = onSaveAsCopy, modifier = Modifier.fillMaxWidth()) {
                    Text("Сохранить как копию")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onOverwrite, modifier = Modifier.fillMaxWidth()) {
                    Text("Заменить (рекорд сбросится)")
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Отмена")
                }
            }
        }
    }
}

@Composable
private fun ShapeRow(
    levelShape: LevelShape,
    percent: Int?,
    onWeightChange: (Int) -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            val cellSize = 10.dp
            val previewPiece = remember(levelShape.shape) {
                Piece(id = "preview", shape = levelShape.shape, color = PieceColor.BLUE)
            }
            Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                ShapeGlyph(
                    piece = previewPiece,
                    cellSize = cellSize,
                    modifier = Modifier.size(previewPiece.glyphWidth(cellSize), previewPiece.glyphHeight(cellSize))
                )
            }
            Spacer(Modifier.width(8.dp))

            if (percent != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = { onWeightChange((levelShape.weight - 1).coerceAtLeast(1)) }) {
                        Icon(Icons.Default.Remove, contentDescription = "Меньше")
                    }
                    Text("${levelShape.weight}", style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = { onWeightChange(levelShape.weight + 1) }) {
                        Icon(Icons.Default.Add, contentDescription = "Больше")
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "$percent%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить фигуру")
            }
        }
    }
}

@Composable
private fun ShapeDrawDialog(
    gridSize: Int,
    existingShapes: List<LevelShape>,
    onDismiss: () -> Unit,
    onConfirm: (PieceShape) -> Unit
) {
    var selected by remember(gridSize) { mutableStateOf(setOf<Coordinate>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Новая фигура", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Нажимайте на клетки — соседние по стороне или по углу считаются соединёнными",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(12.dp))

                // The grid always spans the dialog's full width — cell size shrinks/grows with gridSize instead of being fixed.
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val cellSize = maxWidth / gridSize
                    Column {
                        for (row in 0 until gridSize) {
                            Row {
                                for (col in 0 until gridSize) {
                                    val coord = Coordinate(row, col)
                                    val isOn = coord in selected
                                    Box(
                                        modifier = Modifier
                                            .size(cellSize)
                                            .padding(2.dp)
                                            .background(
                                                color = if (isOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .clickable {
                                                errorMessage = null
                                                selected = if (isOn) selected - coord else selected + coord
                                            }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        val cells = selected
                        when {
                            cells.isEmpty() -> errorMessage = "Нарисуйте хотя бы одну клетку"
                            !ShapeConnectivity.isConnected(cells) -> errorMessage = "Клетки фигуры должны соприкасаться"
                            else -> {
                                val normalized = ShapeSymmetry.normalize(cells.toList())
                                val newKey = ShapeSymmetry.canonicalKey(normalized)
                                val isDuplicate = existingShapes.any { ShapeSymmetry.canonicalKey(it.shape.baseCells) == newKey }
                                if (isDuplicate) {
                                    errorMessage = "Такая фигура уже есть (с учётом поворота)"
                                } else {
                                    onConfirm(PieceShape(PieceShape.newCustomId(), normalized))
                                }
                            }
                        }
                    }) { Text("Добавить") }
                }
            }
        }
    }
}
