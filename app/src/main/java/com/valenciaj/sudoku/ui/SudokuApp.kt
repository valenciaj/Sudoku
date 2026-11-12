package com.valenciaj.sudoku.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.valenciaj.sudoku.R
import com.valenciaj.sudoku.core.Daily
import com.valenciaj.sudoku.core.Difficulty
import com.valenciaj.sudoku.core.Game
import com.valenciaj.sudoku.core.Grid
import com.valenciaj.sudoku.core.Hint
import com.valenciaj.sudoku.core.Mode
import com.valenciaj.sudoku.core.Stats
import com.valenciaj.sudoku.core.TimeAttack

@Composable
fun SudokuApp(vm: GameViewModel) {
    val state by vm.state.collectAsState()
    val stats by vm.stats.collectAsState()
    val settings by vm.settings.collectAsState()
    val cloud by vm.cloud.collectAsState()
    val tutorial by vm.tutorial.collectAsState()

    // Para el cronómetro y guarda la partida cuando la app se va a segundo plano.
    LifecycleResumeEffect(vm) {
        vm.onResume()
        onPauseOrDispose { vm.onPause() }
    }

    AppSurface {
        val game = state.game
        if (state.screen != Screen.MENU) {
            BackHandler {
                if (state.screen == Screen.TUTORIAL && tutorial.technique != null) vm.openLesson(null) else vm.show(Screen.MENU)
            }
        }
        // Qué pantalla toca. Al cambiar, la nueva entra con un fundido suave.
        val target: Screen? = when {
            state.loading -> null
            state.screen == Screen.GAME && game == null -> Screen.MENU
            else -> state.screen
        }
        AnimatedContent(
            targetState = target,
            transitionSpec = {
                (fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.97f)) togetherWith fadeOut(tween(140))
            },
            contentAlignment = Alignment.Center,
            label = "screen",
        ) { screen ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (screen) {
                    null -> CircularProgressIndicator(strokeCap = StrokeCap.Round)
                    Screen.STATS -> StatsScreen(stats, vm.today, onBack = { vm.show(Screen.MENU) })
                    Screen.ACHIEVEMENTS -> AchievementsScreen(stats, onBack = { vm.show(Screen.MENU) })
                    Screen.SETTINGS ->
                        SettingsScreen(settings, vm::updateSettings, cloud, vm, onBack = { vm.show(Screen.MENU) })
                    Screen.TUTORIAL -> TutorialScreen(tutorial, vm, onBack = { vm.show(Screen.MENU) })
                    Screen.GAME -> state.game?.let { GameScreen(it, state, settings, vm) }
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
    }
}

/** Fondo de la app: color base con un halo suave del color de acento arriba. */
@Composable
fun AppSurface(content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxSize(), color = colors.background) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    listOf(colors.primary.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(0f, 0f),
                    radius = 1400f,
                ),
            ),
        ) {
            Box(Modifier.safeDrawingPadding().padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                content()
            }
        }
    }
}

// ───────────────────────────── Menú ─────────────────────────────

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
    val continuable = saved?.takeIf { it.dailyDay == null }
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (maxWidth >= 840.dp) {
            // Tablet en horizontal: dos columnas.
            Column(Modifier.widthIn(max = 980.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                MenuHeader(onShow)
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        DailyCard(stats, today, onDaily)
                        continuable?.let { ContinueCard(it, onContinue) }
                        MenuShortcuts(onShow)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        NewGameSection(stats, mode, onMode, onSelect)
                    }
                }
            }
        } else {
            Column(
                Modifier.widthIn(max = 520.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MenuHeader(onShow)
                DailyCard(stats, today, onDaily)
                continuable?.let { ContinueCard(it, onContinue) }
                NewGameSection(stats, mode, onMode, onSelect)
                MenuShortcuts(onShow)
            }
        }
    }
}

@Composable
private fun MenuHeader(onShow: (Screen) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        SudokuLogo(Modifier.size(34.dp))
        Spacer(Modifier.width(12.dp))
        Text(stringResource(R.string.app_name), Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge)
        RoundIconButton(R.drawable.ic_settings, stringResource(R.string.settings), { onShow(Screen.SETTINGS) })
    }
}

/** Tarjeta destacada del sudoku diario, con degradado y la racha. */
@Composable
private fun DailyCard(stats: Stats, today: Long, onDaily: () -> Unit) {
    val done = stats.daily.isDone(today)
    val streak = stats.daily.currentStreak(today)
    Surface(onClick = onDaily, shape = MaterialTheme.shapes.extraLarge, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.background(brandGradient()).padding(22.dp)) {
            // Cuadrícula decorativa al fondo.
            Canvas(Modifier.align(Alignment.CenterEnd).size(120.dp).graphicsLayer { alpha = 0.16f }) {
                val step = size.width / 3
                for (i in 0..3) {
                    drawLine(Color.White, Offset(i * step, 0f), Offset(i * step, size.height), 3f)
                    drawLine(Color.White, Offset(0f, i * step), Offset(size.width, i * step), 3f)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppIcon(R.drawable.ic_calendar_today, null, Modifier.size(16.dp), tint = Color.White.copy(alpha = 0.85f))
                    Text(
                        stringResource(R.string.daily).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.85f),
                        letterSpacing = 1.4.sp,
                    )
                }
                Text(Daily.difficultyFor(today).text(), style = MaterialTheme.typography.headlineLarge, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = CircleShape, color = Color.White) {
                        Text(
                            stringResource(if (done) R.string.daily_done else R.string.play),
                            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.Black.copy(alpha = 0.85f),
                        )
                    }
                    if (streak > 0) {
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.18f)) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                AppIcon(R.drawable.ic_local_fire_department, null, Modifier.size(18.dp), tint = Color(0xFFFFC46B))
                                Text(
                                    "$streak",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    modifier = Modifier.clearAndSetSemantics {},
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Partida a medias: nivel, tiempo y barra de progreso. */
@Composable
private fun ContinueCard(saved: Game, onContinue: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val label = saved.puzzle.difficulty.text().let { if (saved.mode == Mode.CLASSIC) it else "${saved.mode.text()} · $it" }
    val givens = saved.puzzle.givens.count { it != 0 }
    val filled = saved.values.count { it != 0 }
    val progress = ((filled - givens).toFloat() / (81 - givens).coerceAtLeast(1)).coerceIn(0f, 1f)
    Surface(onClick = onContinue, shape = MaterialTheme.shapes.large, color = colors.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(colors.primary, CircleShape), contentAlignment = Alignment.Center) {
                AppIcon(R.drawable.ic_play_arrow, null, tint = colors.onPrimary)
            }
            Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.continue_title), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Text(formatTime(saved.elapsedSeconds), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                }
                Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    strokeCap = StrokeCap.Round,
                    trackColor = colors.surfaceContainerHighest,
                    drawStopIndicator = {},
                )
            }
        }
    }
}

@Composable
private fun NewGameSection(stats: Stats, mode: Mode, onMode: (Mode) -> Unit, onSelect: (Difficulty) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(stringResource(R.string.new_game))
        ModeSelector(mode, onMode)
        val info = when (mode) {
            Mode.TIME_ATTACK -> stringResource(R.string.mode_time_attack_info, TimeAttack.PENALTY_SECONDS.toInt())
            Mode.KILLER -> stringResource(R.string.mode_killer_info)
            Mode.CLASSIC -> null
        }
        info?.let {
            Text(it, Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Difficulty.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { level -> LevelCard(level, stats, Modifier.weight(1f)) { onSelect(level) } }
            }
        }
    }
}

@Composable
private fun ModeSelector(mode: Mode, onMode: (Mode) -> Unit) {
    SegmentedPills(Mode.entries.map { it.text() }, mode.ordinal, { onMode(Mode.entries[it]) })
}

@Composable
private fun LevelCard(level: Difficulty, stats: Stats, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(stiffness = 500f), label = "press")
    val best = stats[level].bestSeconds
    Surface(
        onClick = onClick,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant),
        interactionSource = interaction,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LevelDots(level.ordinal + 1)
            Text(level.text(), style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AppIcon(R.drawable.ic_timer, null, Modifier.size(14.dp), tint = colors.onSurfaceVariant)
                Text(best?.let(::formatTime) ?: "—", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            }
        }
    }
}

/** Accesos a tutorial, estadísticas y logros. */
@Composable
private fun MenuShortcuts(onShow: (Screen) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Shortcut(R.drawable.ic_school, stringResource(R.string.tutorial), Modifier.weight(1f)) { onShow(Screen.TUTORIAL) }
        Shortcut(R.drawable.ic_bar_chart, stringResource(R.string.stats), Modifier.weight(1f)) { onShow(Screen.STATS) }
        Shortcut(R.drawable.ic_emoji_events, stringResource(R.string.achievements), Modifier.weight(1f)) { onShow(Screen.ACHIEVEMENTS) }
    }
}

@Composable
private fun Shortcut(icon: Int, label: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.large, color = colors.surfaceContainerLow) {
        Column(
            Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(40.dp).background(colors.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                AppIcon(icon, null, Modifier.size(22.dp), tint = colors.onPrimaryContainer)
            }
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ───────────────────────────── Partida ─────────────────────────────

@Composable
fun GameScreen(game: Game, state: UiState, settings: Settings, actions: GameActions) {
    // Tablet o móvil en horizontal: tablero a la izquierda y controles a la derecha.
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (maxWidth > maxHeight && maxWidth >= 560.dp) {
            val panel = (maxWidth * 0.4f).coerceIn(260.dp, 400.dp)
            val gap = 32.dp
            val board = minOf(maxHeight, maxWidth - panel - gap)
            Row(
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(board)) { BoardArea(game, state, settings, actions) }
                Column(
                    Modifier.width(panel).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    GameHeader(game, state, actions)
                    GameControls(game, state, actions)
                    NumberGrid(game, actions::input)
                }
            }
        } else {
            Column(
                // En tablets en vertical el tablero crece, pero sin pasarse del alto disponible.
                Modifier.widthIn(max = minOf(720.dp, maxHeight * 0.62f).coerceAtLeast(320.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                GameHeader(game, state, actions)
                Box(Modifier.fillMaxWidth().aspectRatio(1f)) { BoardArea(game, state, settings, actions) }
                GameControls(game, state, actions)
                NumberPad(game, actions::input)
            }
        }
    }
    GameDialogs(game, state, actions)
}

@Composable
private fun GameHeader(game: Game, state: UiState, actions: GameActions) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RoundIconButton(R.drawable.ic_arrow_back, stringResource(R.string.menu), { actions.show(Screen.MENU) })
        Column(Modifier.weight(1f).padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                game.puzzle.difficulty.text().let {
                    when {
                        game.dailyDay != null -> stringResource(R.string.daily_prefix, it)
                        game.mode != Mode.CLASSIC -> "${game.mode.text()} · $it"
                        else -> it
                    }
                },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // En contrarreloj se muestra el tiempo que queda (en rojo el último minuto).
                val remaining = game.remainingSeconds
                if (remaining != null) {
                    Pill(R.drawable.ic_hourglass_top, formatTime(remaining), color = if (remaining < 60) colors.error else Color.Unspecified)
                } else {
                    Pill(R.drawable.ic_timer, formatTime(game.elapsedSeconds))
                }
                Pill(
                    R.drawable.ic_cancel,
                    game.mistakes.toString(),
                    Modifier.clearAndSetSemantics { },
                    color = if (game.mistakes > 0) colors.error else Color.Unspecified,
                )
            }
        }
        RoundIconButton(
            if (state.paused) R.drawable.ic_play_arrow else R.drawable.ic_pause,
            stringResource(if (state.paused) R.string.resume_cd else R.string.pause_cd),
            actions::togglePause,
            active = state.paused,
        )
    }
}

@Composable
private fun BoardArea(game: Game, state: UiState, settings: Settings, actions: GameActions) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier.fillMaxSize()
            .clip(shape)
            .background(colors.surfaceContainerLowest)
            .border(2.dp, colors.onSurface.copy(alpha = 0.85f), shape),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(state.paused, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) }, label = "pause") { paused ->
            if (paused) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(Modifier.size(72.dp).background(colors.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                        AppIcon(R.drawable.ic_pause, null, Modifier.size(36.dp), tint = colors.onPrimaryContainer)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(stringResource(R.string.paused), style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = actions::togglePause) { Text(stringResource(R.string.resume_cd)) }
                }
            } else {
                Board(game, state.selected, state.hint?.cell, settings, actions::select)
            }
        }
    }
}

@Composable
private fun GameControls(game: Game, state: UiState, actions: GameActions) {
    AnimatedContent(
        state.hint,
        transitionSpec = { (fadeIn(tween(220)) + slideInVertically { it / 4 }) togetherWith fadeOut(tween(120)) },
        contentKey = { it != null },
        label = "controls",
    ) { hint ->
        if (hint != null) {
            HintCard(hint, actions)
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ToolButton(R.drawable.ic_undo, stringResource(R.string.undo), actions::undo, enabled = game.canUndo)
                ToolButton(R.drawable.ic_ink_eraser, stringResource(R.string.erase), { actions.input(0) })
                ToolButton(R.drawable.ic_edit, stringResource(R.string.notes), actions::toggleNotesMode, active = state.notesMode)
                ToolButton(R.drawable.ic_lightbulb, stringResource(R.string.hint), actions::hint)
            }
        }
    }
}

@Composable
private fun ToolButton(icon: Int, label: String, onClick: () -> Unit, active: Boolean = false, enabled: Boolean = true) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RoundIconButton(icon, label, onClick, active = active, enabled = enabled, size = 52.dp)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

@Composable
private fun GameDialogs(game: Game, state: UiState, actions: GameActions) {
    if (game.isLost) {
        AlertDialog(
            onDismissRequest = {},
            icon = { AppIcon(R.drawable.ic_hourglass_top, null, Modifier.size(32.dp)) },
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
            icon = {
                Box(Modifier.size(64.dp).background(brandGradient(), CircleShape), contentAlignment = Alignment.Center) {
                    AppIcon(R.drawable.ic_emoji_events, null, Modifier.size(34.dp), tint = Color.White)
                }
            },
            title = { Text(stringResource(R.string.solved_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(
                            R.string.solved_text,
                            game.puzzle.difficulty.text(),
                            formatTime(game.elapsedSeconds),
                            game.mistakes,
                            game.hintsUsed,
                        ),
                        textAlign = TextAlign.Center,
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
    val colors = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = colors.tertiaryContainer) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(36.dp).background(colors.tertiary, CircleShape), contentAlignment = Alignment.Center) {
                    AppIcon(R.drawable.ic_lightbulb, null, Modifier.size(20.dp), tint = colors.onTertiary)
                }
                Text(hint.text(), Modifier.weight(1f), color = colors.onTertiaryContainer, style = MaterialTheme.typography.bodyMedium)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = actions::dismissHint) { Text(stringResource(R.string.close), color = colors.onTertiaryContainer) }
                Button(
                    onClick = actions::revealHint,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = colors.tertiary, contentColor = colors.onTertiary),
                ) {
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
    val scale = settings.digitSize.scale
    val dark = colors.background.luminance() < 0.5f
    // Daltonismo (paleta Okabe-Ito): azul para lo escrito, bermellón tachado para los errores.
    val wrongColor = if (settings.colorBlind) Color(0xFFD55E00) else colors.error
    val userColor = when {
        !settings.colorBlind -> colors.primary
        dark -> Color(0xFF56B4E9)
        else -> Color(0xFF0072B2)
    }
    val relatedColor = colors.primary.copy(alpha = if (dark) 0.10f else 0.07f).compositeOver(colors.surfaceContainerLowest)
    val context = LocalContext.current
    val selectLabel = stringResource(R.string.cell_select)
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
                        val wrong = game.isWrong(cell)
                        val isSelected = cell == selected
                        val target = when {
                            cell == hintCell -> colors.tertiaryContainer
                            isSelected -> colors.primary
                            value != 0 && value == selectedValue -> colors.primaryContainer
                            wrong -> colors.errorContainer.copy(alpha = 0.6f).compositeOver(colors.surfaceContainerLowest)
                            related -> relatedColor
                            else -> colors.surfaceContainerLowest
                        }
                        val background by animateColorAsState(target, tween(180), label = "cell")
                        val textColor = when {
                            isSelected && cell != hintCell -> colors.onPrimary
                            wrong -> wrongColor
                            game.isGiven(cell) -> colors.onSurface
                            else -> userColor
                        }
                        // Pequeño "pop" al escribir un número (no en la primera pintura).
                        val pop = remember { Animatable(1f) }
                        var last by remember { mutableIntStateOf(value) }
                        LaunchedEffect(value) {
                            if (value != last) {
                                last = value
                                if (value != 0) {
                                    pop.snapTo(0.5f)
                                    pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 500f))
                                }
                            }
                        }
                        val description = cellDescription(context, game, cell, cageOf, cages)
                        Box(
                            Modifier.weight(1f).fillMaxSize().background(background).clickable { onSelect(cell) }
                                .clearAndSetSemantics {
                                    contentDescription = description
                                    onClick(label = selectLabel) { onSelect(cell); true }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (value != 0) {
                                Text(
                                    value.toString(),
                                    Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value },
                                    color = textColor,
                                    fontFamily = Outfit,
                                    fontSize = 23.sp * scale,
                                    fontWeight = if (game.isGiven(cell)) FontWeight.SemiBold else FontWeight.Medium,
                                    textDecoration = if (wrong && settings.colorBlind) TextDecoration.LineThrough else null,
                                )
                            } else if (game.notes[cell] != 0) {
                                Notes(
                                    game, cell, selectedValue,
                                    if (isSelected) colors.onPrimary.copy(alpha = 0.8f) else colors.onSurfaceVariant,
                                    if (isSelected) colors.onPrimary else colors.primary,
                                    scale,
                                )
                            }
                            sumCells[cell]?.let { sum ->
                                Text(
                                    sum.toString(),
                                    Modifier.align(Alignment.TopStart).padding(start = 3.dp, top = 1.dp),
                                    fontSize = 8.sp,
                                    lineHeight = 8.sp,
                                    color = if (isSelected) colors.onPrimary else colors.onSurface,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
        GridLines(colors.outlineVariant, colors.onSurface.copy(alpha = 0.85f), outer = false)
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
private fun Notes(game: Game, cell: Int, highlight: Int, color: Color, highlightColor: Color, scale: Float) {
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
                                fontSize = 9.sp * minOf(scale, 1.2f),
                                lineHeight = 9.sp * minOf(scale, 1.2f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Texto que lee TalkBack para una casilla: fila, columna, contenido y jaula. */
private fun cellDescription(
    context: android.content.Context,
    game: Game,
    cell: Int,
    cageOf: IntArray,
    cages: List<com.valenciaj.sudoku.core.Cage>,
): String {
    val value = game.values[cell]
    var content = when {
        value != 0 && game.isGiven(cell) -> context.getString(R.string.cell_given, value)
        value != 0 && game.isWrong(cell) -> context.getString(R.string.cell_wrong, value)
        value != 0 -> value.toString()
        game.notes[cell] != 0 -> context.getString(
            R.string.cell_notes,
            (1..9).filter { game.hasNote(cell, it) }.joinToString(" "),
        )
        else -> context.getString(R.string.cell_empty)
    }
    val cage = cageOf[cell]
    if (cage >= 0) content = context.getString(R.string.cell_cage, content, cages[cage].sum)
    return context.getString(R.string.cell_desc, Grid.row(cell) + 1, Grid.col(cell) + 1, content)
}

/** Líneas del tablero: finas entre casillas, gruesas entre cajas 3x3. */
@Composable
internal fun GridLines(thin: Color, thick: Color, outer: Boolean = true) {
    Canvas(Modifier.fillMaxSize()) {
        val step = size.width / 9
        val range = if (outer) 0..9 else 1..8
        for (i in range) {
            val major = i % 3 == 0
            val color = if (major) thick else thin
            val width = if (major) 2.dp.toPx() else 1.dp.toPx()
            val p = i * step
            drawLine(color, Offset(p, 0f), Offset(p, size.height), width)
            drawLine(color, Offset(0f, p), Offset(size.width, p), width)
        }
    }
}

/** Tecla de número: cifra grande y, debajo, cuántas quedan por poner. */
@Composable
private fun DigitKey(digit: Int, game: Game, onInput: (Int) -> Unit, modifier: Modifier, big: Boolean) {
    val colors = MaterialTheme.colorScheme
    val left = 9 - game.placedCount(digit)
    // Un número colocado 9 veces ya no hace falta: se desactiva.
    val done = left <= 0
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, spring(stiffness = 700f), label = "key")
    val alpha by animateFloatAsState(if (done) 0.35f else 1f, label = "keyAlpha")
    Surface(
        onClick = { onInput(digit) },
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha },
        enabled = !done,
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceContainerHigh,
        interactionSource = interaction,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                digit.toString(),
                fontFamily = Outfit,
                fontWeight = FontWeight.Medium,
                fontSize = if (big) 30.sp else 26.sp,
                lineHeight = if (big) 32.sp else 28.sp,
                color = colors.primary,
            )
            Text(
                if (done) "✓" else left.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}

/** Teclado 3x3 para el panel lateral en horizontal. */
@Composable
private fun NumberGrid(game: Game, onInput: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (r in 0 until 3) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (c in 0 until 3) {
                    DigitKey(r * 3 + c + 1, game, onInput, Modifier.weight(1f).height(76.dp), big = true)
                }
            }
        }
    }
}

@Composable
private fun NumberPad(game: Game, onInput: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        for (d in 1..9) DigitKey(d, game, onInput, Modifier.weight(1f).height(64.dp), big = false)
    }
}
