package com.valenciaj.sudoku.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.valenciaj.sudoku.core.Achievement
import com.valenciaj.sudoku.core.Stats

@Composable
fun AchievementsScreen(stats: Stats, onBack: () -> Unit) {
    val unlocked = Achievement.entries.count { it.isUnlocked(stats) }
    Column(
        Modifier.widthIn(max = 480.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ScreenHeader("Logros", onBack)
        Text("$unlocked de ${Achievement.entries.size} conseguidos", style = MaterialTheme.typography.titleMedium)
        Achievement.entries.forEach { a ->
            val done = a.isUnlocked(stats)
            Card(
                Modifier.fillMaxWidth().alpha(if (done) 1f else 0.5f),
                colors = CardDefaults.cardColors(
                    containerColor = if (done) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (done) "🏆" else "🔒", fontSize = 28.sp)
                    Column(Modifier.padding(start = 16.dp)) {
                        Text(a.title, fontWeight = FontWeight.Bold)
                        Text(a.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(settings: Settings, onChange: (Settings) -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.widthIn(max = 480.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader("Ajustes", onBack)

        Text("Tema", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = settings.themeMode == mode,
                    onClick = { onChange(settings.copy(themeMode = mode)) },
                    label = { Text(mode.label) },
                )
            }
        }

        Text("Color", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Accent.entries.forEach { accent ->
                val selected = settings.accent == accent
                Box(
                    Modifier
                        .size(44.dp)
                        .background(accent.color, CircleShape)
                        .border(
                            width = if (selected) 3.dp else 0.dp,
                            color = MaterialTheme.colorScheme.onBackground,
                            shape = CircleShape,
                        )
                        .clickable { onChange(settings.copy(accent = accent)) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) Text("✓", color = androidx.compose.ui.graphics.Color.White, fontSize = 20.sp)
                }
            }
        }

        Text("Resaltado", style = MaterialTheme.typography.titleMedium)
        SwitchRow("Fila, columna y caja", settings.highlightRelated) {
            onChange(settings.copy(highlightRelated = it))
        }
        SwitchRow("Números y notas iguales", settings.highlightSame) {
            onChange(settings.copy(highlightSame = it))
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
