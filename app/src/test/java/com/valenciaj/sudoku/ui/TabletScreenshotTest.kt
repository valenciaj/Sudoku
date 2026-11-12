package com.valenciaj.sudoku.ui

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.compose.runtime.Composable
import com.valenciaj.sudoku.core.Hints
import com.valenciaj.sudoku.core.Mode
import org.junit.Rule
import org.junit.Test

/** Capturas para tablets de 10" (horizontal), que pide Google Play. */
class TabletScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.NEXUS_10.copy(locale = "es"), maxPercentDifference = 1.0)

    private fun shot(dark: Boolean = false, content: @Composable () -> Unit) = paparazzi.snapshot {
        SudokuTheme(Settings(themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT)) { AppSurface(content) }
    }

    @Test
    fun game() = shot {
        val game = Fixtures.game
        GameScreen(game, UiState(screen = Screen.GAME, game = game, selected = 40), Settings(), NoActions)
    }

    @Test
    fun gameHintDark() = shot(dark = true) {
        val game = Fixtures.game
        val hint = Hints.next(game)
        GameScreen(game, UiState(game = game, selected = hint?.cell, hint = hint), Settings(), NoActions)
    }

    @Test
    fun menu() = shot { MenuScreen(Fixtures.game, Fixtures.stats, Fixtures.today, {}, {}, Mode.CLASSIC, {}, {}, {}) }

    @Test
    fun menuDark() = shot(dark = true) { MenuScreen(Fixtures.game, Fixtures.stats, Fixtures.today, {}, {}, Mode.CLASSIC, {}, {}, {}) }

    @Test
    fun statistics() = shot { StatsScreen(Fixtures.stats, Fixtures.today) {} }
}

/** Capturas para tablets de 7" (vertical). */
class Tablet7ScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.NEXUS_7.copy(locale = "es"), maxPercentDifference = 1.0)

    @Test
    fun game() = paparazzi.snapshot {
        SudokuTheme(Settings(themeMode = ThemeMode.LIGHT)) {
            AppSurface {
                val game = Fixtures.game
                GameScreen(game, UiState(screen = Screen.GAME, game = game, selected = 40), Settings(), NoActions)
            }
        }
    }
}
