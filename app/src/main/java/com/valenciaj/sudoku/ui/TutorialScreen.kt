package com.valenciaj.sudoku.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.valenciaj.sudoku.R
import com.valenciaj.sudoku.core.Example
import com.valenciaj.sudoku.core.Grid
import com.valenciaj.sudoku.core.Technique
import com.valenciaj.sudoku.core.Tutorial

/** Estado del tutorial: lección abierta y el ejemplo que se muestra. */
data class TutorialState(
    val technique: Technique? = null,
    val example: Example? = null,
    val loading: Boolean = false,
    /** null = sin responder; true = acertado; false = fallo. */
    val answered: Boolean? = null,
    /** Mostrar los candidatos descartados (técnicas de descarte). */
    val revealed: Boolean = false,
)

interface TutorialActions {
    fun openLesson(technique: Technique?)
    fun nextExample()
    fun answerTutorial(digit: Int)
    fun revealTutorial()
}

@Composable
fun Technique.title(): String = stringResource(
    when (this) {
        Technique.NAKED_SINGLE -> R.string.tech_naked_single
        Technique.HIDDEN_SINGLE -> R.string.tech_hidden_single
        Technique.POINTING -> R.string.tech_pointing
        Technique.NAKED_PAIR, Technique.NAKED_TRIPLE -> R.string.tech_naked_pair
        Technique.HIDDEN_PAIR, Technique.HIDDEN_TRIPLE -> R.string.tech_hidden_pair
        Technique.X_WING -> R.string.tech_x_wing
    },
)

@Composable
fun Technique.description(): String = stringResource(
    when (this) {
        Technique.NAKED_SINGLE -> R.string.tech_naked_single_desc
        Technique.HIDDEN_SINGLE -> R.string.tech_hidden_single_desc
        Technique.POINTING -> R.string.tech_pointing_desc
        Technique.NAKED_PAIR, Technique.NAKED_TRIPLE -> R.string.tech_naked_pair_desc
        Technique.HIDDEN_PAIR, Technique.HIDDEN_TRIPLE -> R.string.tech_hidden_pair_desc
        Technique.X_WING -> R.string.tech_x_wing_desc
    },
)

@Composable
fun TutorialScreen(state: TutorialState, actions: TutorialActions, onBack: () -> Unit) {
    val technique = state.technique
    Column(
        Modifier.widthIn(max = 480.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (technique == null) {
            ScreenHeader(stringResource(R.string.tutorial), onBack)
            Text(stringResource(R.string.tutorial_intro), style = MaterialTheme.typography.bodyMedium)
            Tutorial.lessons.forEach { t ->
                Card(onClick = { actions.openLesson(t) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(t.title(), Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            Text(t.level.text(), style = MaterialTheme.typography.labelMedium)
                        }
                        Text(t.description(), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { actions.openLesson(null) }) { Text(stringResource(R.string.back)) }
                Text(technique.title(), style = MaterialTheme.typography.titleLarge)
            }
            Text(technique.description(), style = MaterialTheme.typography.bodyMedium)
            val example = state.example
            when {
                state.loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.padding(end = 12.dp))
                    Text(stringResource(R.string.tutorial_loading))
                }
                example == null -> Text(stringResource(R.string.tutorial_none))
                else -> Lesson(example, state, actions)
            }
            if (!state.loading) {
                OutlinedButton(onClick = actions::nextExample, Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.tutorial_next))
                }
            }
        }
    }
}

@Composable
private fun Lesson(example: Example, state: TutorialState, actions: TutorialActions) {
    val placement = example.placement
    Text(stringResource(R.string.tutorial_pattern), style = MaterialTheme.typography.bodySmall)
    TutorialBoard(example, showEliminations = state.revealed, solved = state.answered == true)
    if (example.technique.places && placement != null) {
        Text(stringResource(R.string.tutorial_question), fontWeight = FontWeight.Bold)
        when (state.answered) {
            true -> Text(stringResource(R.string.tutorial_correct), color = MaterialTheme.colorScheme.primary)
            false -> Text(stringResource(R.string.tutorial_wrong), color = MaterialTheme.colorScheme.error)
            null -> Unit
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (d in 1..9) {
                FilledTonalButton(
                    onClick = { actions.answerTutorial(d) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    contentPadding = PaddingValues(0.dp),
                    enabled = state.answered != true,
                ) { Text(d.toString(), fontSize = 20.sp) }
            }
        }
    } else if (!state.revealed) {
        Button(onClick = actions::revealTutorial, Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.tutorial_show))
        }
    } else {
        Text(stringResource(R.string.tutorial_removed), fontWeight = FontWeight.Bold)
    }
}

/** Tablero del tutorial: números, candidatos, patrón resaltado y descartes en rojo. */
@Composable
private fun TutorialBoard(example: Example, showEliminations: Boolean, solved: Boolean) {
    val colors = MaterialTheme.colorScheme
    val target = example.placement?.first
    Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
        Column(Modifier.fillMaxSize()) {
            for (r in 0 until 9) {
                Row(Modifier.weight(1f)) {
                    for (c in 0 until 9) {
                        val cell = r * 9 + c
                        val value = example.values[cell]
                        val background = when {
                            cell == target -> colors.primaryContainer
                            cell in example.pattern -> colors.tertiaryContainer
                            else -> colors.surface
                        }
                        Box(
                            Modifier.weight(1f).fillMaxSize().background(background),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                value != 0 -> Text(value.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                cell == target && solved -> Text(
                                    example.placement!!.second.toString(),
                                    fontSize = 20.sp,
                                    color = colors.primary,
                                )
                                // Sin pistas antes de responder: la casilla objetivo muestra "?".
                                cell == target -> Text("?", fontSize = 20.sp, color = colors.primary)
                                else -> Candidates(
                                    mask = example.candidates[cell],
                                    patternDigits = if (cell in example.pattern && (solved || !example.technique.places)) {
                                        example.patternDigits
                                    } else {
                                        0
                                    },
                                    removed = if (showEliminations) example.eliminations[cell] ?: 0 else 0,
                                    color = colors.onSurfaceVariant,
                                    highlight = colors.primary,
                                    removedColor = colors.error,
                                )
                            }
                        }
                    }
                }
            }
        }
        GridLines(colors.outline, colors.onSurface)
    }
}

@Composable
private fun Candidates(mask: Int, patternDigits: Int, removed: Int, color: Color, highlight: Color, removedColor: Color) {
    Column(Modifier.fillMaxSize().padding(1.dp)) {
        for (r in 0 until 3) {
            Row(Modifier.weight(1f)) {
                for (c in 0 until 3) {
                    val d = r * 3 + c + 1
                    val b = Grid.bit(d)
                    Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (mask and b != 0) {
                            val isRemoved = removed and b != 0
                            val inPattern = patternDigits and b != 0
                            Text(
                                d.toString(),
                                fontSize = 9.sp,
                                lineHeight = 9.sp,
                                color = when {
                                    isRemoved -> removedColor
                                    inPattern -> highlight
                                    else -> color
                                },
                                fontWeight = if (isRemoved || inPattern) FontWeight.Bold else FontWeight.Normal,
                                textDecoration = if (isRemoved) TextDecoration.LineThrough else null,
                            )
                        }
                    }
                }
            }
        }
    }
}
