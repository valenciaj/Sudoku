package com.valenciaj.sudoku.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valenciaj.sudoku.core.Difficulty
import com.valenciaj.sudoku.core.Stats

@Composable
fun StatsScreen(stats: Stats, today: Long, onBack: () -> Unit) {
    Column(
        Modifier.widthIn(max = 480.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader("Estadísticas", onBack)
        StatCard("Sudoku diario") {
            StatRow("Completados", stats.daily.completed.toString())
            StatRow("Racha actual", "${stats.daily.currentStreak(today)} días")
            StatRow("Mejor racha", "${stats.daily.bestStreak} días")
        }
        Difficulty.entries.forEach { level ->
            val s = stats[level]
            StatCard(level.label) {
                StatRow("Jugadas", s.played.toString())
                StatRow("Ganadas", "${s.won} (${s.winRate}%)")
                StatRow("Sin errores", s.perfect.toString())
                StatRow("Sin pistas", s.noHints.toString())
                StatRow("Mejor tiempo", s.bestSeconds?.let(::formatTime) ?: "—")
                StatRow("Tiempo medio", s.averageSeconds?.let(::formatTime) ?: "—")
            }
        }
    }
}

@Composable
private fun StatCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
