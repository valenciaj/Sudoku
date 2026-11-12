package com.valenciaj.sudoku.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.valenciaj.sudoku.R
import com.valenciaj.sudoku.core.Difficulty
import com.valenciaj.sudoku.core.Mode
import com.valenciaj.sudoku.core.Stats

@Composable
fun StatsScreen(stats: Stats, today: Long, onBack: () -> Unit) {
    Column(
        Modifier.widthIn(max = 960.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(stringResource(R.string.stats), onBack)

        // Sudoku diario: tarjeta destacada con tres cifras grandes.
        Surface(shape = MaterialTheme.shapes.extraLarge, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.background(brandGradient()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppIcon(R.drawable.ic_local_fire_department, null, Modifier.size(20.dp), tint = Color(0xFFFFC46B))
                    Text(stringResource(R.string.daily), style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
                Row {
                    BigNumber(stats.daily.completed.toString(), stringResource(R.string.completed), Modifier.weight(1f))
                    BigNumber(stats.daily.currentStreak(today).toString(), stringResource(R.string.current_streak), Modifier.weight(1f))
                    BigNumber(stats.daily.bestStreak.toString(), stringResource(R.string.best_streak), Modifier.weight(1f))
                }
            }
        }

        val cards = Difficulty.entries.map { Triple(it.text(), it.ordinal + 1, stats[it]) } +
            listOf(Mode.TIME_ATTACK, Mode.KILLER).map { Triple(it.text(), 0, stats.mode(it)) }
        AdaptiveList(cards) { (title, level, s) ->
            Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                        if (level > 0) LevelDots(level)
                    }
                    Row {
                        Metric(s.played.toString(), stringResource(R.string.played), Modifier.weight(1f))
                        Metric("${s.winRate}%", stringResource(R.string.won), Modifier.weight(1f))
                        Metric(s.perfect.toString(), stringResource(R.string.perfect), Modifier.weight(1f))
                    }
                    Row {
                        Metric(s.noHints.toString(), stringResource(R.string.no_hints), Modifier.weight(1f))
                        Metric(s.bestSeconds?.let(::formatTime) ?: "—", stringResource(R.string.best_time), Modifier.weight(1f))
                        Metric(s.averageSeconds?.let(::formatTime) ?: "—", stringResource(R.string.avg_time), Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BigNumber(value: String, label: String, modifier: Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.displaySmall, color = Color.White)
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Metric(value: String, label: String, modifier: Modifier) {
    Box(modifier) {
        Column {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
