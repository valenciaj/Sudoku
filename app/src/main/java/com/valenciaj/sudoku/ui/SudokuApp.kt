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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.material3.FilterChip
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.valenciaj.sudoku.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.valenciaj.sudoku.core.Daily
import com.valenciaj.sudoku.core.Difficulty
import com.valenciaj.sudoku.core.Game
import com.valenciaj.sudoku.core.Grid
import com.valenciaj.sudoku.core.Hint
import com.valenciaj.sudoku.core.Mode
import com.valenciaj.sudoku.core.TimeAttack
import com.valenciaj.sudoku.core.Stats

@Composable
fun SudokuApp(vm: GameViewModel) {
    val state by vm.state.collectAsState()
    val stats by vm.stats.collectAsState()
    val settings by vm.settings.collectAsState()
    val cloud by vm.cloud.collectAsState()

    // Para el cronómetro y guarda la partida cuando la app se va a segundo plano.
    LifecycleResumeEffect(vm) {
        vm.onResume()
        onPauseOrDispose { vm.onPause() }
    }

    AppSurface {
        val game = state.game
        if (state.screen != Screen.MENU) BackHandler { vm.show(Screen.MENU) }
        when {
            state.loading -> CircularProgressIndicator()
            state.screen == Screen.STATS -> StatsScreen(stats, vm.today, onBack = { vm.show(Screen.MENU) })
            state.screen == Screen.ACHIEVEMENTS -> AchievementsScreen(stats, onBack = { vm.show(Screen.MENU) })
            state.screen == Screen.SETTINGS ->
                SettingsScreen(settings, vm::updateSettings, cloud, vm, onBack = { vm.show(Screen.MENU) })
            state.screen == Screen.GAME && game != null -> GameScreen(game, state, settings, vm)
            else -> MenuScreen(
                saved = state.saved,
                stats = stats,
                today = vm.today,
                onDaily = vm::playDaily,
                onContinue = vm::continueGame,
                mode = state.menuMode,
                onMode = vm::selectMenuMode,
                onSelect = { vm.newGame(it, state.menuMode) },
                onShow = vm::show,
            )
        }
    }
}

@Composable
fun AppSurface(content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.safeDrawingPadding().padding(16.dp), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
fun MenuScreen(
    saved: Game?,
    stats: Stats,
    today: Long,
    onDaily: () -> Unit,
    onContinue: () -> Unit,
    mode: Mode,
    onMode: (Mode) -> Unit,
    onSelect: (Difficulty) -> Unit,
    onShow: (Screen) -> Unit,
) {
    Column(
        Modifier.widthIn(max = 360.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)

        // Sudoku diario
        val done = stats.daily.isDone(today)
        val streak = stats.daily.currentStreak(today)
        Card(
            onClick = onDaily,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.daily), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${Daily.difficultyFor(today).text()}  ·  " +
                        stringResource(if (done) R.string.daily_done else R.string.daily_play) +
                        (if (streak > 0) "  ·  🔥 $streak" else ""),
                )
            }
        }

        if (saved != null && saved.dailyDay == null) {
            FilledTonalButton(onClick = onContinue, Modifier.fillMaxWidth().height(56.dp)) {
                val label = saved.puzzle.difficulty.text().let {
                    if (saved.mode == Mode.CLASSIC) it else "${saved.mode.text()} · $it"
                }
                Text(stringResource(R.string.continue_game, label), fontSize = 18.sp)
            }
        }
        Text(stringResource(R.string.new_game), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Mode.entries.forEach { m ->
                FilterChip(selected = m == mode, onClick = { onMode(m) }, label = { Text(m.text()) })
            }
        }
        when (mode) {
            Mode.TIME_ATTACK -> Text(
                stringResource(R.string.mode_time_attack_info, TimeAttack.PENALTY_SECONDS.toInt()),
                style = MaterialTheme.typography.bodySmall,
            )
            Mode.KILLER -> Text(stringResource(R.string.mode_killer_info), style = MaterialTheme.typography.bodySmall)
            Mode.CLASSIC -> Unit
        }
        Difficulty.entries.forEach { level ->
            Button(onClick = { onSelect(level) }, Modifier.fillMaxWidth().height(56.dp)) {
                Text(level.text(), fontSize = 18.sp)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onClick = { onShow(Screen.STATS) }) { Text(stringResource(R.string.stats)) }
            TextButton(onClick = { onShow(Screen.ACHIEVEMENTS) }) { Text(stringResource(R.string.achievements)) }
            TextButton(onClick = { onShow(Screen.SETTINGS) }) { Text(stringResource(R.string.settings)) }
        }
    }
}

@Composable
fun GameScreen(game: Game, state: UiState, settings: Settings, actions: GameActions) {
    Column(
        Modifier.widthIn(max = 480.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { actions.show(Screen.MENU) }) { Text(stringResource(R.string.back_menu)) }
            Text(
                game.puzzle.difficulty.text().let {
                    when {
                        game.dailyDay != null -> stringResource(R.string.daily_prefix, it)
                        game.mode != Mode.CLASSIC -> "${game.mode.text()} · $it"
                        else -> it
                    }
                },
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(stringResource(R.string.mistakes, game.mistakes), style = MaterialTheme.typography.titleMedium)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // En contrarreloj se muestra el tiempo que queda (en rojo el último minuto).
            val remaining = game.remainingSeconds
            Text(
                if (remaining != null) "⏳ ${formatTime(remaining)}" else formatTime(game.elapsedSeconds),
                Modifier.weight(1f).padding(start = 12.dp),
                style = MaterialTheme.typography.titleLarge,
                color = if (remaining != null && remaining < 60) MaterialTheme.colorScheme.error else Color.Unspecified,
            )
            TextButton(onClick = actions::togglePause) { Text(stringResource(if (state.paused) R.string.resume else R.string.pause)) }
        }
        Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            if (state.paused) {
                Text(stringResource(R.string.paused), style = MaterialTheme.typography.headlineMedium)
            } else {
                Board(game, state.selected, state.hint?.cell, settings, actions::select)
            }
        }
        val hint = state.hint
        if (hint != null) {
            HintCard(hint, actions)
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val mod = Modifier.weight(1f)
                val pad = PaddingValues(horizontal = 4.dp)
                OutlinedButton(actions::undo, mod, enabled = game.canUndo, contentPadding = pad) { Text(stringResource(R.string.undo)) }
                OutlinedButton({ actions.input(0) }, mod, contentPadding = pad) { Text(stringResource(R.string.erase)) }
                if (state.notesMode) {
                    Button(actions::toggleNotesMode, mod, contentPadding = pad) { Text(stringResource(R.string.notes_on)) }
                } else {
                    OutlinedButton(actions::toggleNotesMode, mod, contentPadding = pad) { Text(stringResource(R.string.notes)) }
                }
                OutlinedButton(actions::hint, mod, contentPadding = pad) { Text(stringResource(R.string.hint)) }
            }
        }
        NumberPad(game, actions::input)
    }

    if (game.isLost) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.time_up_title)) },
            text = { Text(stringResource(R.string.time_up_text, game.puzzle.difficulty.text())) },
            confirmButton = {
                Button(onClick = { actions.newGame(game.puzzle.difficulty) }) { Text(stringResource(R.string.retry)) }
            },
            dismissButton = { TextButton(onClick = { actions.show(Screen.MENU) }) { Text(stringResource(R.string.menu)) } },
        )
    }

    if (game.isSolved) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.solved_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(
                            R.string.solved_text,
                            game.puzzle.difficulty.text(),
                            formatTime(game.elapsedSeconds),
                            game.mistakes,
                            game.hintsUsed,
                        ),
                    )
                    state.newAchievements.forEach {
                        Text(stringResource(R.string.achievement_unlocked, it.title()), fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                if (game.dailyDay == null) {
                    Button(onClick = { actions.newGame(game.puzzle.difficulty) }) { Text(stringResource(R.string.same_again)) }
                }
            },
            dismissButton = { TextButton(onClick = { actions.show(Screen.MENU) }) { Text(stringResource(R.string.menu)) } },
        )
    }
}

@Composable
private fun HintCard(hint: Hint, actions: GameActions) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("💡 ${hint.text()}")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = actions::dismissHint) { Text(stringResource(R.string.close)) }
                Button(onClick = actions::revealHint) {
                    Text(if (hint.kind == Hint.Kind.WRONG) stringResource(R.string.fix) else stringResource(R.string.put_digit, hint.digit))
                }
            }
        }
    }
}

internal fun formatTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@Composable
private fun Board(game: Game, selected: Int?, hintCell: Int?, settings: Settings, onSelect: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val selectedValue = if (settings.highlightSame) selected?.let { game.values[it] } ?: 0 else 0
    val cages = game.puzzle.cages
    // Killer: a qué jaula pertenece cada casilla y en qué casilla se escribe la suma.
    val cageOf = remember(cages) {
        IntArray(Grid.CELLS) { -1 }.also { owner -> cages.forEachIndexed { i, c -> c.cells.forEach { owner[it] = i } } }
    }
    val sumCells = remember(cages) { cages.associate { it.cells.min() to it.sum } }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            for (r in 0 until 9) {
                Row(Modifier.weight(1f)) {
                    for (c in 0 until 9) {
                        val cell = r * 9 + c
                        val value = game.values[cell]
                        val related = settings.highlightRelated && selected != null && (
                            Grid.row(selected) == r || Grid.col(selected) == c || Grid.box(selected) == Grid.box(cell)
                            )
                        val background = when {
                            cell == hintCell -> colors.tertiaryContainer
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
                                Notes(game, cell, selectedValue, colors.onSurfaceVariant, colors.primary)
                            }
                            sumCells[cell]?.let { sum ->
                                Text(
                                    sum.toString(),
                                    Modifier.align(Alignment.TopStart).padding(start = 3.dp, top = 1.dp),
                                    fontSize = 8.sp,
                                    lineHeight = 8.sp,
                                    color = colors.onSurface,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
        GridLines(colors.outline, colors.onSurface)
        if (cages.isNotEmpty()) CageLines(cageOf, colors.onSurfaceVariant)
    }
}

/** Bordes punteados de las jaulas del modo Killer, un poco hacia dentro de cada casilla. */
@Composable
private fun CageLines(cageOf: IntArray, color: Color) {
    Canvas(Modifier.fillMaxSize()) {
        val step = size.width / 9
        val inset = 3.dp.toPx()
        val effect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
        val w = 1.dp.toPx()
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(color, Offset(x1, y1), Offset(x2, y2), w, pathEffect = effect)
        for (cell in 0 until Grid.CELLS) {
            val r = Grid.row(cell)
            val c = Grid.col(cell)
            val x0 = c * step
            val y0 = r * step
            val same = { rr: Int, cc: Int -> rr in 0..8 && cc in 0..8 && cageOf[rr * 9 + cc] == cageOf[cell] }
            val top = !same(r - 1, c)
            val bottom = !same(r + 1, c)
            val left = !same(r, c - 1)
            val right = !same(r, c + 1)
            val l = x0 + if (left) inset else 0f
            val rt = x0 + step - if (right) inset else 0f
            val t = y0 + if (top) inset else 0f
            val b = y0 + step - if (bottom) inset else 0f
            if (top) line(l, t, rt, t)
            if (bottom) line(l, b, rt, b)
            if (left) line(l, t, l, b)
            if (right) line(rt, t, rt, b)
        }
    }
}

/** Notas a lápiz: mini cuadrícula 3x3. La nota igual al número elegido se resalta. */
@Composable
private fun Notes(game: Game, cell: Int, highlight: Int, color: Color, highlightColor: Color) {
    Column(Modifier.fillMaxSize().padding(1.dp)) {
        for (r in 0 until 3) {
            Row(Modifier.weight(1f)) {
                for (c in 0 until 3) {
                    val d = r * 3 + c + 1
                    Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (game.hasNote(cell, d)) {
                            val hl = d == highlight
                            Text(
                                d.toString(),
                                color = if (hl) highlightColor else color,
                                fontWeight = if (hl) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 9.sp,
                                lineHeight = 9.sp,
                            )
                        }
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
            // Un número colocado 9 veces ya no hace falta: se desactiva.
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

@Composable
internal fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.back_menu)) }
        Spacer(Modifier.weight(0.01f))
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}
