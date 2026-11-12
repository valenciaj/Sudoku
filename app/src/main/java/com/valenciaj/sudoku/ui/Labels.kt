package com.valenciaj.sudoku.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.valenciaj.sudoku.R
import com.valenciaj.sudoku.core.Achievement
import com.valenciaj.sudoku.core.Difficulty
import com.valenciaj.sudoku.core.Hint
import com.valenciaj.sudoku.core.Mode

/** Textos traducidos de los conceptos del motor (que no conoce los idiomas). */

@Composable
fun Difficulty.text(): String = stringResource(
    when (this) {
        Difficulty.EASY -> R.string.level_easy
        Difficulty.MEDIUM -> R.string.level_medium
        Difficulty.HARD -> R.string.level_hard
        Difficulty.DEMENTIAL -> R.string.level_dementia
    },
)

@Composable
fun ThemeMode.text(): String = stringResource(
    when (this) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    },
)

@Composable
fun Hint.text(): String = when (kind) {
    Hint.Kind.WRONG -> stringResource(R.string.hint_wrong)
    Hint.Kind.NAKED_SINGLE -> stringResource(R.string.hint_naked, digit)
    Hint.Kind.REVEAL -> stringResource(R.string.hint_reveal, digit)
    Hint.Kind.HIDDEN_SINGLE -> {
        val unitName = stringResource(
            when (unit) {
                Hint.UnitKind.ROW -> R.string.unit_row
                Hint.UnitKind.COLUMN -> R.string.unit_column
                else -> R.string.unit_box
            },
        )
        stringResource(R.string.hint_hidden, unitName, digit)
    }
}

private val achievementRes = mapOf(
    Achievement.FIRST_WIN to (R.string.ach_first_win_title to R.string.ach_first_win_desc),
    Achievement.PERFECT to (R.string.ach_perfect_title to R.string.ach_perfect_desc),
    Achievement.NO_HELP to (R.string.ach_no_help_title to R.string.ach_no_help_desc),
    Achievement.FAST to (R.string.ach_fast_title to R.string.ach_fast_desc),
    Achievement.ALL_LEVELS to (R.string.ach_all_levels_title to R.string.ach_all_levels_desc),
    Achievement.DEMENTIAL to (R.string.ach_demential_title to R.string.ach_demential_desc),
    Achievement.VETERAN to (R.string.ach_veteran_title to R.string.ach_veteran_desc),
    Achievement.DAILY_FIRST to (R.string.ach_daily_first_title to R.string.ach_daily_first_desc),
    Achievement.STREAK_7 to (R.string.ach_streak_7_title to R.string.ach_streak_7_desc),
    Achievement.STREAK_30 to (R.string.ach_streak_30_title to R.string.ach_streak_30_desc),
    Achievement.TIME_ATTACK to (R.string.ach_time_attack_title to R.string.ach_time_attack_desc),
    Achievement.KILLER to (R.string.ach_killer_title to R.string.ach_killer_desc),
)

@Composable
fun Achievement.title(): String = stringResource(achievementRes.getValue(this).first)

@Composable
fun Achievement.description(): String = stringResource(achievementRes.getValue(this).second)

@Composable
fun Mode.text(): String = stringResource(
    when (this) {
        Mode.CLASSIC -> R.string.mode_classic
        Mode.TIME_ATTACK -> R.string.mode_time_attack
        Mode.KILLER -> R.string.mode_killer
    },
)
