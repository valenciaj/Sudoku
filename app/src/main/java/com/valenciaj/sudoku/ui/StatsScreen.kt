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
import com.valenciaj.sudoku.R
import androidx.compose.ui.res.stringResource
import com.valenciaj.sudoku.core.Difficulty
import com.valenciaj.sudoku.core.Stats

@Composable
fun StatsScreen(stats: Stats, today: Long, onBack: () -> Unit) {
    Column(
        Modifier.widthIn(max = 480.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(stringResource(R.string.stats), onBack)
        StatCard(stringResource(R.string.daily)) {
            StatRow(stringResource(R.string.completed), stats.daily.completed.toString())
            StatRow(stringResource(R.string.current_streak), stringResource(R.string.days, stats.daily.currentStreak(today)))
            StatRow(stringResource(R.string.best_streak), stringResource(R.string.days, stats.daily.bestStreak))
        }
        Difficulty.entries.forEach { level ->
            val s = stats[level]
            StatCard(level.text()) {
                StatRow(stringResource(R.string.played), s.played.toString())
                StatRow(stringResource(R.string.won), stringResource(R.string.won_value, s.won, s.winRate))
                StatRow(stringResource(R.string.perfect), s.perfect.toString())
                StatRow(stringResource(R.string.no_hints), s.noHints.toString())
                StatRow(stringResource(R.string.best_time), s.bestSeconds?.let(::formatTime) ?: "—")
                StatRow(stringResource(R.string.avg_time), s.averageSeconds?.let(::formatTime) ?: "—")
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
