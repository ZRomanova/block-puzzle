package com.blockpuzzle.rotate.ui.theme

import androidx.compose.ui.graphics.Color
import com.blockpuzzle.rotate.domain.PieceColor

val SeedPrimary = Color(0xFF4F8DFF)
val SeedSecondary = Color(0xFF5CD68A)
val SurfaceDark = Color(0xFF14142B)
val SurfaceDarkVariant = Color(0xFF1E1E3F)
val BoardCellEmpty = Color(0xFF23234A)
val BoardCellBorder = Color(0xFF33335C)
val HighlightValid = Color(0x805CD68A)
val HighlightInvalid = Color(0x80FF5C5C)
val HighlightClearPreview = Color(0xB3FFC857)

fun PieceColor.toComposeColor(): Color = when (this) {
    PieceColor.RED -> Color(0xFFFF5C5C)
    PieceColor.ORANGE -> Color(0xFFFF9142)
    PieceColor.YELLOW -> Color(0xFFF4E04D)
    PieceColor.GREEN -> Color(0xFF5CD68A)
    PieceColor.BLUE -> Color(0xFF4F8DFF)
    PieceColor.PURPLE -> Color(0xFFB366FF)
}
