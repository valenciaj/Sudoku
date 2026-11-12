package com.valenciaj.sudoku.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.valenciaj.sudoku.R

/** Icono de Material Symbols (redondeado) guardado como vector en res/drawable. */
@Composable
fun AppIcon(@DrawableRes id: Int, description: String?, modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
    Icon(
        painterResource(id),
        description,
        modifier.size(24.dp),
        tint = if (tint == Color.Unspecified) androidx.compose.material3.LocalContentColor.current else tint,
    )
}

/** Botón redondo con icono, con un leve "hundido" al pulsarlo. */
@Composable
fun RoundIconButton(
    @DrawableRes icon: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    enabled: Boolean = true,
    size: Dp = 44.dp,
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, spring(stiffness = 600f), label = "press")
    val bg by animateColorAsState(if (active) colors.primary else colors.surfaceContainerHigh, label = "bg")
    val fg by animateColorAsState(if (active) colors.onPrimary else colors.onSurface, label = "fg")
    Surface(
        onClick = onClick,
        modifier = modifier.size(size).graphicsLayer { scaleX = scale; scaleY = scale },
        enabled = enabled,
        shape = CircleShape,
        color = bg,
        contentColor = fg.copy(alpha = if (enabled) 1f else 0.38f),
        interactionSource = interaction,
    ) {
        Box(contentAlignment = Alignment.Center) { AppIcon(icon, description) }
    }
}

/** Pastilla pequeña con icono y texto (tiempo, errores…). */
@Composable
fun Pill(@DrawableRes icon: Int, text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified, description: String? = null) {
    val colors = MaterialTheme.colorScheme
    val tint = if (color == Color.Unspecified) colors.onSurfaceVariant else color
    Surface(modifier, shape = CircleShape, color = colors.surfaceContainerHigh) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            AppIcon(icon, description, Modifier.size(16.dp), tint = tint)
            Text(text, style = MaterialTheme.typography.labelLarge, color = if (color == Color.Unspecified) colors.onSurface else color)
        }
    }
}

/** Marca de la app: una mini cuadrícula 3x3 con casillas redondeadas. */
@Composable
fun SudokuLogo(modifier: Modifier = Modifier.size(40.dp)) {
    val colors = MaterialTheme.colorScheme
    val strong = colors.primary
    val soft = colors.primary.copy(alpha = 0.28f)
    Canvas(modifier) {
        val gap = size.width * 0.08f
        val cell = (size.width - gap * 2) / 3
        val filled = setOf(0, 4, 8, 5)
        for (i in 0 until 9) {
            val x = (i % 3) * (cell + gap)
            val y = (i / 3) * (cell + gap)
            drawRoundRect(
                if (i in filled) strong else soft,
                Offset(x, y),
                Size(cell, cell),
                CornerRadius(cell * 0.32f),
            )
        }
    }
}

/** Puntos que indican la dificultad (1 a 4). */
@Composable
fun LevelDots(level: Int, total: Int = 4, color: Color = MaterialTheme.colorScheme.primary) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until total) {
            Box(
                Modifier.size(width = if (i < level) 14.dp else 6.dp, height = 6.dp)
                    .background(if (i < level) color else color.copy(alpha = 0.22f), CircleShape),
            )
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier.padding(start = 4.dp, top = 4.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = MaterialTheme.typography.labelLarge.letterSpacing * 1.6f,
    )
}

/** Lista de tarjetas: una columna en móvil, dos en pantallas anchas (tablets). */
@Composable
internal fun <T> AdaptiveList(items: List<T>, item: @Composable (T) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 640.dp) 2 else 1
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { Box(Modifier.weight(1f)) { item(it) } }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** Cabecera de las pantallas secundarias: botón de volver y título grande. */
@Composable
internal fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RoundIconButton(R.drawable.ic_arrow_back, stringResource(R.string.back_cd), onBack)
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Selector de opciones tipo "pastilla": la elegida se rellena con el color principal. */
@Composable
fun SegmentedPills(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = CircleShape, color = colors.surfaceContainerHigh, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(4.dp)) {
            options.forEachIndexed { i, label ->
                val on = i == selected
                val bg by animateColorAsState(if (on) colors.primary else Color.Transparent, tween(220), label = "pill")
                val fg by animateColorAsState(if (on) colors.onPrimary else colors.onSurfaceVariant, tween(220), label = "pillText")
                Box(
                    Modifier.weight(1f).height(40.dp).clip(CircleShape).background(bg)
                        .selectable(selected = on, role = Role.Tab) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** Bloque de ajustes: título en mayúsculas y contenido dentro de una tarjeta suave. */
@Composable
fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(title)
        Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content()
            }
        }
    }
}
