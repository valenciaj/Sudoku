package com.valenciaj.sudoku.ui

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.valenciaj.sudoku.core.Difficulty
import com.valenciaj.sudoku.core.Game
import com.valenciaj.sudoku.core.Generator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val game: Game? = null,
    val loading: Boolean = false,
    val selected: Int? = null,
    val notesMode: Boolean = false,
    val paused: Boolean = false,
    /** Hay una partida guardada sin terminar que se puede continuar desde el menú. */
    val canContinue: Boolean = false,
)

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("sudoku", Context.MODE_PRIVATE)
    private val generator = Generator()
    private val _state = MutableStateFlow(UiState(canContinue = loadSaved() != null))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var timer: Job? = null
    private var inForeground = false

    fun newGame(difficulty: Difficulty) {
        _state.update { UiState(loading = true) }
        viewModelScope.launch {
            val puzzle = withContext(Dispatchers.Default) { generator.generate(difficulty) }
            _state.update { UiState(game = Game(puzzle)) }
            save()
            startTimer()
        }
    }

    fun continueGame() {
        val game = loadSaved() ?: return
        _state.update { UiState(game = game) }
        startTimer()
    }

    fun select(cell: Int) = _state.update { it.copy(selected = cell) }

    fun toggleNotesMode() = _state.update { it.copy(notesMode = !it.notesMode) }

    fun input(digit: Int) {
        _state.update { s ->
            val game = s.game ?: return@update s
            val cell = s.selected ?: return@update s
            if (s.paused) return@update s
            val next = if (s.notesMode && digit != 0) game.toggleNote(cell, digit) else game.input(cell, digit)
            s.copy(game = next)
        }
        save()
    }

    fun undo() {
        _state.update { s -> s.copy(game = s.game?.undo()) }
        save()
    }

    fun togglePause() = _state.update { it.copy(paused = !it.paused) }

    fun backToMenu() {
        save()
        timer?.cancel()
        _state.update { UiState(canContinue = loadSaved() != null) }
    }

    /** La app pasa a primer plano: el cronómetro sigue corriendo. */
    fun onResume() {
        inForeground = true
        startTimer()
    }

    /** La app pasa a segundo plano: se para el cronómetro y se guarda. */
    fun onPause() {
        inForeground = false
        timer?.cancel()
        save()
    }

    private fun startTimer() {
        timer?.cancel()
        if (!inForeground) return
        timer = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _state.update { s ->
                    if (s.paused || s.game == null) s else s.copy(game = s.game.tick())
                }
            }
        }
    }

    private fun save() {
        val game = _state.value.game ?: return
        prefs.edit {
            if (game.isSolved) remove(KEY_SAVED) else putString(KEY_SAVED, game.encode())
        }
    }

    private fun loadSaved(): Game? = prefs.getString(KEY_SAVED, null)?.let(Game::decode)

    private companion object {
        const val KEY_SAVED = "saved_game"
    }
}
