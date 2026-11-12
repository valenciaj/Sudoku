package com.valenciaj.sudoku.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valenciaj.sudoku.core.Difficulty
import com.valenciaj.sudoku.core.Game
import com.valenciaj.sudoku.core.Grid

@Composable
fun SudokuApp(vm: GameViewModel = viewModel()) {
    val state by vm.state.collectAsState()

    // Para el cronómetro y guarda la partida cuando la app se va a segundo plano.
    LifecycleResumeEffect(vm) {
        vm.onResume()
        onPauseOrDispose { vm.onPause() }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.safeDrawingPadding().padding(16.dp), contentAlignment = Alignment.Center) {
            val game = state.game
            when {
                state.loading -> CircularProgressIndicator()
                game == null -> MenuScreen(
                    canContinue = state.canContinue,
                    onContinue = vm::continueGame,
                    onSelect = vm::newGame,
                )
                else -> {
                    BackHandler(onBack = vm::backToMenu)
                    GameScreen(game, state, vm)
                }
            }
        }
    }
}

@Composable
private fun MenuScreen(canContinue: Boolean, onContinue: () -> Unit, onSelect: (Difficulty) -> Unit) {
    Column(
        Modifier.widthIn(max = 360.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Sudoku", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        if (canContinue) {
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = onContinue, Modifier.fillMaxWidth().height(56.dp)) {
                Text("Continuar partida", fontSize = 18.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Nueva partida", style = MaterialTheme.typography.titleMedium)
        Difficulty.entries.forEach { level ->
            Button(onClick = { onSelect(level) }, Modifier.fillMaxWidth().height(56.dp)) {
                Text(level.label, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun GameScreen(game: Game, state: UiState, vm: GameViewModel) {
    Column(
        Modifier.widthIn(max = 480.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = vm::backToMenu) { Text("← Menú") }
            Text(
                game.puzzle.difficulty.label,
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
            )
            Text("Errores: ${game.mistakes}", style = MaterialTheme.typography.titleMedium)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatTime(game.elapsedSeconds),
                Modifier.weight(1f).padding(start = 12.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            TextButton(onClick = vm::togglePause) { Text(if (state.paused) "▶ Seguir" else "⏸ Pausa") }
        }
        Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            if (state.paused) {
                Text("En pausa", style = MaterialTheme.typography.headlineMedium)
            } else {
                Board(game, state.selected, vm::select)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = vm::undo, Modifier.weight(1f), enabled = game.canUndo) { Text("Deshacer") }
            OutlinedButton(onClick = { vm.input(0) }, Modifier.weight(1f)) { Text("Borrar") }
            val notesLabel = if (state.notesMode) "Notas: SÍ" else "Notas: NO"
            if (state.notesMode) {
                Button(onClick = vm::toggleNotesMode, Modifier.weight(1f)) { Text(notesLabel) }
            } else {
                OutlinedButton(onClick = vm::toggleNotesMode, Modifier.weight(1f)) { Text(notesLabel) }
            }
        }
        NumberPad(game, vm::input)
    }

    if (game.isSolved) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("¡Resuelto!") },
            text = {
                Text(
                    "Sudoku ${game.puzzle.difficulty.label} en ${formatTime(game.elapsedSeconds)} " +
                        "con ${game.mistakes} errores.",
                )
            },
            confirmButton = { Button(onClick = { vm.newGame(game.puzzle.difficulty) }) { Text("Otro igual") } },
            dismissButton = { TextButton(onClick = vm::backToMenu) { Text("Menú") } },
        )
    }
}

private fun formatTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@Composable
private fun Board(game: Game, selected: Int?, onSelect: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val selectedValue = selected?.let { game.values[it] } ?: 0
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            for (r in 0 until 9) {
                Row(Modifier.weight(1f)) {
                    for (c in 0 until 9) {
                        val cell = r * 9 + c
                        val value = game.values[cell]
                        val related = selected != null && (
                            Grid.row(selected) == r || Grid.col(selected) == c || Grid.box(selected) == Grid.box(cell)
                            )
                        val background = when {
                            cell == selected -> colors.primaryContainer
                            value != 0 && value == selectedValue -> colors.secondaryContainer
                            related -> colors.surfaceVariant
                            else -> colors.surface
                        }
                        val textColor = when {
                            game.isWrong(cell) -> colors.error
                            game.isGiven(cell) -> colors.onSurface
                            else -> colors.primary
                        }
                        Box(
                            Modifier.weight(1f).fillMaxSize().background(background).clickable { onSelect(cell) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (value != 0) {
                                Text(
                                    value.toString(),
                                    color = textColor,
                                    fontSize = 22.sp,
                                    fontWeight = if (game.isGiven(cell)) FontWeight.Bold else FontWeight.Normal,
                                )
                            } else if (game.notes[cell] != 0) {
                                Notes(game, cell, colors.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        GridLines(colors.outline, colors.onSurface)
    }
}

/** Notas a lápiz: mini cuadrícula 3x3 dentro de la celda. */
@Composable
private fun Notes(game: Game, cell: Int, color: Color) {
    Column(Modifier.fillMaxSize().padding(1.dp)) {
        for (r in 0 until 3) {
            Row(Modifier.weight(1f)) {
                for (c in 0 until 3) {
                    val d = r * 3 + c + 1
                    Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (game.hasNote(cell, d)) Text(d.toString(), color = color, fontSize = 9.sp, lineHeight = 9.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun GridLines(thin: Color, thick: Color) {
    Canvas(Modifier.fillMaxSize()) {
        val step = size.width / 9
        for (i in 0..9) {
            val major = i % 3 == 0
            val color = if (major) thick else thin
            val width = if (major) 2.dp.toPx() else 0.5.dp.toPx()
            val p = i * step
            drawLine(color, Offset(p, 0f), Offset(p, size.height), width)
            drawLine(color, Offset(0f, p), Offset(size.width, p), width)
        }
    }
}

@Composable
private fun NumberPad(game: Game, onInput: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (d in 1..9) {
            // Un número colocado 9 veces ya no hace falta: se atenúa.
            val done = game.placedCount(d) >= 9
            FilledTonalButton(
                onClick = { onInput(d) },
                modifier = Modifier.weight(1f).height(56.dp),
                enabled = !done,
                contentPadding = PaddingValues(0.dp),
            ) { Text(d.toString(), fontSize = 22.sp) }
        }
    }
}
