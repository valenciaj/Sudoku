package com.valenciaj.sudoku.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp

@Composable
fun SudokuTheme(settings: Settings = Settings(), content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val c = settings.accent.color
    val scheme = if (dark) {
        val bg = Color(0xFF121212)
        val primary = lerp(c, Color.White, 0.35f)
        darkColorScheme(
            primary = primary,
            onPrimary = Color.Black,
            secondary = primary,
            primaryContainer = c.copy(alpha = 0.55f).compositeOver(bg),
            onPrimaryContainer = Color.White,
            secondaryContainer = c.copy(alpha = 0.30f).compositeOver(bg),
            onSecondaryContainer = Color.White,
            tertiaryContainer = Color(0xFF5D4A00),
            background = bg,
            surface = bg,
            surfaceVariant = Color(0xFF262626),
        )
    } else {
        lightColorScheme(
            primary = c,
            secondary = c,
            primaryContainer = c.copy(alpha = 0.30f).compositeOver(Color.White),
            onPrimaryContainer = Color.Black,
            secondaryContainer = c.copy(alpha = 0.15f).compositeOver(Color.White),
            onSecondaryContainer = Color.Black,
            tertiaryContainer = Color(0xFFFFE082),
            surfaceVariant = Color(0xFFEDEDED),
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
