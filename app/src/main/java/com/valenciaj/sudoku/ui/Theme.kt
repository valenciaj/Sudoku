package com.valenciaj.sudoku.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.valenciaj.sudoku.R

/** Tipografía Outfit: geométrica y limpia, con números muy legibles para el tablero. */
val Outfit = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
)

private val AppTypography = Typography().let { t ->
    fun TextStyle.outfit(weight: FontWeight? = null, spacing: Float? = null) = copy(
        fontFamily = Outfit,
        fontWeight = weight ?: fontWeight,
        letterSpacing = spacing?.sp ?: letterSpacing,
    )
    Typography(
        displayLarge = t.displayLarge.outfit(FontWeight.Bold, -1f),
        displayMedium = t.displayMedium.outfit(FontWeight.Bold, -0.8f),
        displaySmall = t.displaySmall.outfit(FontWeight.Bold, -0.5f),
        headlineLarge = t.headlineLarge.outfit(FontWeight.SemiBold, -0.4f),
        headlineMedium = t.headlineMedium.outfit(FontWeight.SemiBold, -0.3f),
        headlineSmall = t.headlineSmall.outfit(FontWeight.SemiBold, -0.2f),
        titleLarge = t.titleLarge.outfit(FontWeight.SemiBold),
        titleMedium = t.titleMedium.outfit(FontWeight.SemiBold, 0f),
        titleSmall = t.titleSmall.outfit(FontWeight.SemiBold),
        bodyLarge = t.bodyLarge.outfit(),
        bodyMedium = t.bodyMedium.outfit(spacing = 0.1f),
        bodySmall = t.bodySmall.outfit(),
        labelLarge = t.labelLarge.outfit(FontWeight.SemiBold),
        labelMedium = t.labelMedium.outfit(FontWeight.Medium),
        labelSmall = t.labelSmall.outfit(FontWeight.Medium),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Un color con el mismo tono que [base] pero con la luminosidad y saturación pedidas (HSL). */
private fun tone(base: Color, lightness: Float, saturation: Float = 1f, hueShift: Float = 0f): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(base.toArgb(), hsl)
    hsl[0] = (hsl[0] + hueShift + 360f) % 360f
    hsl[1] = (hsl[1] * saturation).coerceIn(0f, 1f)
    hsl[2] = lightness
    return Color(ColorUtils.HSLToColor(hsl))
}

/** Paleta tonal propia (estilo Material 3) a partir del color de acento elegido en Ajustes. */
internal fun appColorScheme(accent: Color, dark: Boolean): ColorScheme {
    val amber = Color(0xFFF5A524)
    return if (dark) {
        darkColorScheme(
            primary = tone(accent, 0.74f, 0.9f),
            onPrimary = tone(accent, 0.14f, 0.8f),
            primaryContainer = tone(accent, 0.28f, 0.55f),
            onPrimaryContainer = tone(accent, 0.92f, 0.8f),
            secondary = tone(accent, 0.78f, 0.35f, 25f),
            onSecondary = tone(accent, 0.16f, 0.4f),
            secondaryContainer = tone(accent, 0.22f, 0.35f),
            onSecondaryContainer = tone(accent, 0.90f, 0.4f),
            tertiary = tone(amber, 0.70f),
            onTertiary = tone(amber, 0.15f),
            tertiaryContainer = tone(amber, 0.24f, 0.65f),
            onTertiaryContainer = tone(amber, 0.90f),
            background = tone(accent, 0.065f, 0.30f),
            onBackground = tone(accent, 0.93f, 0.12f),
            surface = tone(accent, 0.065f, 0.30f),
            onSurface = tone(accent, 0.93f, 0.12f),
            surfaceVariant = tone(accent, 0.20f, 0.18f),
            onSurfaceVariant = tone(accent, 0.74f, 0.12f),
            surfaceContainerLowest = tone(accent, 0.05f, 0.30f),
            surfaceContainerLow = tone(accent, 0.095f, 0.26f),
            surfaceContainer = tone(accent, 0.115f, 0.24f),
            surfaceContainerHigh = tone(accent, 0.145f, 0.22f),
            surfaceContainerHighest = tone(accent, 0.18f, 0.20f),
            outline = tone(accent, 0.46f, 0.12f),
            outlineVariant = tone(accent, 0.26f, 0.15f),
            inverseSurface = tone(accent, 0.92f, 0.15f),
            inverseOnSurface = tone(accent, 0.15f, 0.2f),
            error = Color(0xFFFF8A80),
            errorContainer = Color(0xFF5C1A1A),
            onErrorContainer = Color(0xFFFFDAD6),
        )
    } else {
        lightColorScheme(
            primary = tone(accent, 0.47f, 0.95f),
            onPrimary = Color.White,
            primaryContainer = tone(accent, 0.90f, 0.85f),
            onPrimaryContainer = tone(accent, 0.20f, 0.9f),
            secondary = tone(accent, 0.42f, 0.35f, 25f),
            onSecondary = Color.White,
            secondaryContainer = tone(accent, 0.93f, 0.55f),
            onSecondaryContainer = tone(accent, 0.20f, 0.5f),
            tertiary = tone(amber, 0.40f),
            onTertiary = Color.White,
            tertiaryContainer = tone(amber, 0.88f, 0.95f),
            onTertiaryContainer = tone(amber, 0.20f),
            background = tone(accent, 0.975f, 0.35f),
            onBackground = tone(accent, 0.11f, 0.25f),
            surface = tone(accent, 0.975f, 0.35f),
            onSurface = tone(accent, 0.11f, 0.25f),
            surfaceVariant = tone(accent, 0.92f, 0.20f),
            onSurfaceVariant = tone(accent, 0.38f, 0.14f),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = tone(accent, 0.96f, 0.35f),
            surfaceContainer = tone(accent, 0.945f, 0.32f),
            surfaceContainerHigh = tone(accent, 0.93f, 0.30f),
            surfaceContainerHighest = tone(accent, 0.91f, 0.28f),
            outline = tone(accent, 0.58f, 0.12f),
            outlineVariant = tone(accent, 0.86f, 0.18f),
            inverseSurface = tone(accent, 0.18f, 0.2f),
            inverseOnSurface = tone(accent, 0.95f, 0.2f),
            error = Color(0xFFD93025),
            errorContainer = Color(0xFFFFE1DE),
            onErrorContainer = Color(0xFF5C0F0A),
        )
    }
}

/** Degradado de marca: del color principal a un tono vecino. Para tarjetas destacadas. */
@Composable
fun brandGradient(): Brush {
    val p = MaterialTheme.colorScheme.primary
    val dark = MaterialTheme.colorScheme.background.luminanceIsDark()
    val from = tone(p, if (dark) 0.40f else 0.50f, 1f)
    val to = tone(p, if (dark) 0.30f else 0.42f, 1f, 38f)
    return Brush.linearGradient(listOf(from, to))
}

private fun Color.luminanceIsDark(): Boolean = ColorUtils.calculateLuminance(toArgb()) < 0.5

@Composable
fun SudokuTheme(settings: Settings = Settings(), content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = appColorScheme(settings.accent.color, dark),
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
