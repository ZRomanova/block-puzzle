package com.blockpuzzle.rotate.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = darkColorScheme(
    primary = SeedPrimary,
    secondary = SeedSecondary,
    background = SurfaceDark,
    surface = SurfaceDarkVariant,
)

@Composable
fun BlockPuzzleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = AppTypography,
        content = content
    )
}
