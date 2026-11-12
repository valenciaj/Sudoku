package com.valenciaj.sudoku.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valenciaj.sudoku.core.Difficulty
import com.valenciaj.sudoku.core.Game
import com.valenciaj.sudoku.core.Generator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val game: Game? = null,
    val loading: Boolean = false,
    val selected: Int? = null,
)

class GameViewModel : ViewModel() {
    private val generator = Generator()
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun newGame(difficulty: Difficulty) {
        _state.value = UiState(loading = true)
        viewModelScope.launch {
            val puzzle = withContext(Dispatchers.Default) { generator.generate(difficulty) }
            _state.value = UiState(game = Game(puzzle))
        }
    }

    fun select(cell: Int) = _state.update { it.copy(selected = cell) }

    fun input(digit: Int) = _state.update { s ->
        val game = s.game ?: return@update s
        val cell = s.selected ?: return@update s
        s.copy(game = game.input(cell, digit))
    }

    fun backToMenu() {
        _state.value = UiState()
    }
}
