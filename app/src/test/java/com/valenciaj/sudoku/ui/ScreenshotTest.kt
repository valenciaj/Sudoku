package com.valenciaj.sudoku.ui

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.compose.runtime.Composable
import com.valenciaj.sudoku.core.Difficulty
import com.valenciaj.sudoku.core.Game
import com.valenciaj.sudoku.core.Generator
import com.valenciaj.sudoku.core.Hints
import com.valenciaj.sudoku.core.Killer
import com.valenciaj.sudoku.core.Mode
import com.valenciaj.sudoku.core.Stats
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

/**
 * Capturas de pantalla para Google Play, generadas en la JVM con Paparazzi.
 * Generar: ./gradlew :app:recordPaparazziDebug  → app/src/test/snapshots/images/
 */
class ScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "es"), maxPercentDifference = 1.0)

    private val today = 20_400L
    private val puzzle = Generator(Random(11)).generate(Difficulty.HARD)

    /** Una partida a medias: unas casillas resueltas y unas notas. */
    private val game: Game = run {
        var g = Game(puzzle).tick(312)
        val empty = puzzle.givens.indices.filter { puzzle.givens[it] == 0 }
        empty.take(14).forEach { g = g.input(it, puzzle.solution[it]) }
        empty.drop(14).take(6).forEach { cell ->
            g = g.toggleNote(cell, puzzle.solution[cell]).toggleNote(cell, puzzle.solution[cell] % 9 + 1)
        }
        g
    }

    private val stats: Stats = run {
        var s = Stats()
        for (day in today - 8..today) s = s.recordDailyWin(day)
        listOf(Difficulty.EASY to 150L, Difficulty.MEDIUM to 420L, Difficulty.HARD to 780L).forEach { (d, t) ->
            repeat(4) { i -> s = s.recordStart(d).recordWin(d, t + i * 37, mistakes = i % 2, hints = i % 3) }
        }
        s.recordStart(Difficulty.DEMENTIAL)
    }

    private fun shot(dark: Boolean = false, accent: Accent = Accent.BLUE, content: @Composable () -> Unit) =
        paparazzi.snapshot {
            SudokuTheme(Settings(themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, accent = accent)) {
                AppSurface(content)
            }
        }

    @Test
    fun menu() = shot {
        MenuScreen(game, stats, today, {}, {}, Mode.KILLER, {}, {}, {})
    }

    @Test
    fun game() = shot {
        GameScreen(game, UiState(screen = Screen.GAME, game = game, selected = selectedCell()), Settings(), NoActions)
    }

    @Test
    fun gameHintDark() = shot(dark = true, accent = Accent.GREEN) {
        val hint = Hints.next(game)
        GameScreen(game, UiState(game = game, selected = hint?.cell, hint = hint), Settings(), NoActions)
    }

    @Test
    fun killer() = shot(accent = Accent.ORANGE) {
        val p = Killer.generate(Difficulty.MEDIUM, Random(4))
        var g = Game(p, mode = Mode.KILLER, timeLimitSeconds = null).tick(95)
        p.givens.indices.filter { p.givens[it] == 0 }.take(8).forEach { g = g.input(it, p.solution[it]) }
        GameScreen(g, UiState(game = g, selected = 40), Settings(), NoActions)
    }

    @Test
    fun timeAttack() = shot(dark = true, accent = Accent.PINK) {
        val g = Game(puzzle, mode = Mode.TIME_ATTACK, timeLimitSeconds = 900).tick(860)
        GameScreen(g, UiState(game = g), Settings(), NoActions)
    }

    @Test
    fun achievements() = shot(accent = Accent.PURPLE) { AchievementsScreen(stats) {} }

    @Test
    fun statistics() = shot(dark = true) { StatsScreen(stats, today) {} }

    @Test
    fun settings() = shot {
        SettingsScreen(Settings(), {}, CloudState(configured = true, login = "jugador", lastSync = 0L), NoCloud) {}
    }

    private object NoCloud : CloudActions {
        override fun connectGitHub() {}
        override fun cancelConnect() {}
        override fun disconnectGitHub() {}
        override fun syncNow() {}
    }

    private fun selectedCell() = game.values.indices.first { game.values[it] == 5 }

    private object NoActions : GameActions {
        override fun select(cell: Int) {}
        override fun input(digit: Int) {}
        override fun undo() {}
        override fun hint() {}
        override fun revealHint() {}
        override fun dismissHint() {}
        override fun togglePause() {}
        override fun toggleNotesMode() {}
        override fun newGame(difficulty: Difficulty) {}
        override fun show(screen: Screen) {}
    }
}
