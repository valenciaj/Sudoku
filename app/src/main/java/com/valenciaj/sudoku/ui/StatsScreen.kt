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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valenciaj.sudoku.core.Difficulty
import com.valenciaj.sudoku.core.LevelStats
import com.valenciaj.sudoku.core.Stats

@Composable
fun StatsScreen(stats: Stats, onBack: () -> Unit) {
    Column(
        Modifier.widthIn(max = 480.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Menú") }
            Text("Estadísticas", style = MaterialTheme.typography.titleLarge)
        }
        Difficulty.entries.forEach { level -> LevelCard(level, stats[level]) }
    }
}

@Composable
private fun LevelCard(level: Difficulty, s: LevelStats) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(level.label, style = MaterialTheme.typography.titleMedium)
            StatRow("Jugadas", s.played.toString())
            StatRow("Ganadas", "${s.won} (${s.winRate}%)")
            StatRow("Sin errores", s.perfect.toString())
            StatRow("Mejor tiempo", s.bestSeconds?.let(::formatTime) ?: "—")
            StatRow("Tiempo medio", s.averageSeconds?.let(::formatTime) ?: "—")
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
