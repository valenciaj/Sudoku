package com.valenciaj.sudoku.ui

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.valenciaj.sudoku.core.Achievement
import com.valenciaj.sudoku.core.Daily
import com.valenciaj.sudoku.core.Difficulty
import com.valenciaj.sudoku.core.Game
import com.valenciaj.sudoku.core.Generator
import com.valenciaj.sudoku.core.Hint
import com.valenciaj.sudoku.core.Stats
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
import java.time.LocalDate

enum class Screen { MENU, GAME, STATS, ACHIEVEMENTS, SETTINGS }

/** Acciones de la pantalla de juego (separadas para poder hacer capturas sin ViewModel). */
interface GameActions {
    fun select(cell: Int)
    fun input(digit: Int)
    fun undo()
    fun hint()
    fun revealHint()
    fun dismissHint()
    fun togglePause()
    fun toggleNotesMode()
    fun newGame(difficulty: Difficulty)
    fun show(screen: Screen)
}

data class UiState(
    val screen: Screen = Screen.MENU,
    val game: Game? = null,
    val loading: Boolean = false,
    val selected: Int? = null,
    val notesMode: Boolean = false,
    val paused: Boolean = false,
    /** Pista que se está mostrando. */
    val hint: Hint? = null,
    /** Logros conseguidos al terminar la partida actual. */
    val newAchievements: List<Achievement> = emptyList(),
    /** Partida guardada sin terminar que se puede continuar desde el menú. */
    val saved: Game? = null,
)

class GameViewModel(app: Application) : AndroidViewModel(app), GameActions {
    private val prefs = app.getSharedPreferences("sudoku", Context.MODE_PRIVATE)
    private val generator = Generator()

    private val _state = MutableStateFlow(UiState(saved = loadSaved()))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _stats = MutableStateFlow(Stats.decode(prefs.getString(KEY_STATS, null)))
    val stats: StateFlow<Stats> = _stats.asStateFlow()

    private val _settings = MutableStateFlow(Settings.load(prefs))
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private var timer: Job? = null
    private var inForeground = false

    val today: Long get() = LocalDate.now().toEpochDay()

    // --- Empezar partidas ---

    override fun newGame(difficulty: Difficulty) = start(dailyDay = null) { generator.generate(difficulty) }

    fun playDaily() {
        val day = today
        val saved = loadSaved()
        if (saved != null && saved.dailyDay == day) return open(saved)
        start(dailyDay = day) { Daily.puzzle(day) }
    }

    fun continueGame() {
        loadSaved()?.let(::open)
    }

    private fun start(dailyDay: Long?, make: () -> com.valenciaj.sudoku.core.Puzzle) {
        _state.update { UiState(loading = true, screen = Screen.GAME) }
        viewModelScope.launch {
            val puzzle = withContext(Dispatchers.Default) { make() }
            updateStats { it.recordStart(puzzle.difficulty) }
            open(Game(puzzle, dailyDay = dailyDay))
            save()
        }
    }

    private fun open(game: Game) {
        _state.update { UiState(screen = Screen.GAME, game = game) }
        startTimer()
    }

    // --- Jugar ---

    override fun select(cell: Int) = _state.update { it.copy(selected = cell) }

    override fun toggleNotesMode() = _state.update { it.copy(notesMode = !it.notesMode) }

    override fun input(digit: Int) = play { s, game ->
        val cell = s.selected ?: return@play game
        if (s.notesMode && digit != 0) game.toggleNote(cell, digit) else game.input(cell, digit)
    }

    override fun undo() = play { _, game -> game.undo() }

    override fun hint() {
        val s = _state.value
        val game = s.game ?: return
        if (s.paused) return
        val (next, hint) = game.hint()
        _state.update { it.copy(game = next, hint = hint, selected = hint?.cell ?: it.selected) }
        save()
    }

    override fun revealHint() {
        val cell = _state.value.hint?.cell ?: return
        play { _, game -> game.reveal(cell) }
    }

    override fun dismissHint() = _state.update { it.copy(hint = null) }

    override fun togglePause() = _state.update { it.copy(paused = !it.paused) }

    /** Aplica un cambio a la partida, guarda y comprueba si se ha ganado. */
    private fun play(change: (UiState, Game) -> Game) {
        val s = _state.value
        val game = s.game ?: return
        if (s.paused || game.isSolved) return
        val next = change(s, game)
        var unlocked = emptyList<Achievement>()
        if (next.isSolved) {
            val before = _stats.value
            updateStats {
                val won = it.recordWin(next.puzzle.difficulty, next.elapsedSeconds, next.mistakes, next.hintsUsed)
                next.dailyDay?.let(won::recordDailyWin) ?: won
            }
            unlocked = Achievement.entries.filter { it.isUnlocked(_stats.value) && !it.isUnlocked(before) }
        }
        _state.update { it.copy(game = next, hint = null, newAchievements = unlocked) }
        save()
    }

    // --- Navegación ---

    override fun show(screen: Screen) {
        if (screen == Screen.MENU) {
            save()
            timer?.cancel()
            _state.update { UiState(saved = loadSaved()) }
        } else {
            _state.update { it.copy(screen = screen) }
        }
    }

    fun updateSettings(settings: Settings) {
        _settings.value = settings
        settings.save(prefs)
    }

    // --- Ciclo de vida y guardado ---

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
        if (!inForeground || _state.value.game == null) return
        timer = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _state.update { s ->
                    if (s.paused || s.game == null || s.screen != Screen.GAME) s else s.copy(game = s.game.tick())
                }
            }
        }
    }

    private fun updateStats(f: (Stats) -> Stats) {
        _stats.update(f)
        prefs.edit { putString(KEY_STATS, _stats.value.encode()) }
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
        const val KEY_STATS = "stats"
    }
}
