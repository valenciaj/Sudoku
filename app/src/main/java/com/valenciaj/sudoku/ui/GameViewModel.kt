package com.valenciaj.sudoku.ui

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.valenciaj.sudoku.BuildConfig
import com.valenciaj.sudoku.cloud.CloudSync
import com.valenciaj.sudoku.cloud.GitHubClient
import com.valenciaj.sudoku.core.Achievement
import com.valenciaj.sudoku.core.Daily
import com.valenciaj.sudoku.core.Difficulty
import com.valenciaj.sudoku.core.Game
import com.valenciaj.sudoku.core.Generator
import com.valenciaj.sudoku.core.Hint
import com.valenciaj.sudoku.core.Killer
import com.valenciaj.sudoku.core.Mode
import com.valenciaj.sudoku.core.TimeAttack
import com.valenciaj.sudoku.core.Stats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID

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
    /** Modo elegido en el menú para las partidas nuevas. */
    val menuMode: Mode = Mode.CLASSIC,
)

/** Estado de la copia en la nube. */
data class CloudState(
    val configured: Boolean = false,
    val login: String? = null,
    /** Código a escribir en GitHub mientras se conecta. */
    val userCode: String? = null,
    val verificationUri: String? = null,
    val busy: Boolean = false,
    val lastSync: Long? = null,
    val error: String? = null,
)

class GameViewModel(app: Application) : AndroidViewModel(app), GameActions, CloudActions {
    private val prefs = app.getSharedPreferences("sudoku", Context.MODE_PRIVATE)
    private val generator = Generator()

    private val _state = MutableStateFlow(UiState(saved = loadSaved()))
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Estadísticas de este dispositivo (lo que se sube a la nube). */
    private var ownStats = Stats.decode(prefs.getString(KEY_STATS, null))

    /** Estadísticas de otros dispositivos, bajadas de la nube. */
    private var otherStats: List<Stats> =
        prefs.getString(KEY_OTHER_STATS, null)?.split(SEPARATOR)?.map(Stats::decode).orEmpty()

    /** Lo que se muestra: la suma de todos los dispositivos. */
    private val _stats = MutableStateFlow(total())
    val stats: StateFlow<Stats> = _stats.asStateFlow()

    private val _feedback = MutableSharedFlow<Feedback>(extraBufferCapacity = 8)
    val feedback: SharedFlow<Feedback> = _feedback

    private val deviceId: String = prefs.getString(KEY_DEVICE, null)
        ?: UUID.randomUUID().toString().take(8).also { id -> prefs.edit { putString(KEY_DEVICE, id) } }
    private val github = GitHubClient(BuildConfig.GITHUB_CLIENT_ID)
    private val cloudSync = CloudSync(github)
    private var connectJob: Job? = null

    private val _cloud = MutableStateFlow(
        CloudState(
            configured = BuildConfig.GITHUB_CLIENT_ID.isNotBlank(),
            login = prefs.getString(KEY_LOGIN, null),
            lastSync = prefs.getLong(KEY_LAST_SYNC, 0).takeIf { it > 0 },
        ),
    )
    val cloud: StateFlow<CloudState> = _cloud.asStateFlow()

    private val _settings = MutableStateFlow(Settings.load(prefs))
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private var timer: Job? = null
    private var inForeground = false

    val today: Long get() = LocalDate.now().toEpochDay()

    // --- Empezar partidas ---

    override fun newGame(difficulty: Difficulty) = newGame(difficulty, _state.value.game?.mode ?: _state.value.menuMode)

    fun newGame(difficulty: Difficulty, mode: Mode) {
        val limit = if (mode == Mode.TIME_ATTACK) TimeAttack.limitSeconds(difficulty) else null
        start(dailyDay = null, mode = mode, timeLimit = limit) {
            if (mode == Mode.KILLER) Killer.generate(difficulty) else generator.generate(difficulty)
        }
    }

    fun selectMenuMode(mode: Mode) = _state.update { it.copy(menuMode = mode) }

    fun playDaily() {
        val day = today
        val saved = loadSaved()
        if (saved != null && saved.dailyDay == day) return open(saved)
        start(dailyDay = day) { Daily.puzzle(day) }
    }

    fun continueGame() {
        loadSaved()?.let(::open)
    }

    private fun start(
        dailyDay: Long?,
        mode: Mode = Mode.CLASSIC,
        timeLimit: Long? = null,
        make: () -> com.valenciaj.sudoku.core.Puzzle,
    ) {
        val menuMode = _state.value.menuMode
        _state.update { UiState(loading = true, screen = Screen.GAME, menuMode = menuMode) }
        viewModelScope.launch {
            val puzzle = withContext(Dispatchers.Default) { make() }
            updateStats { it.recordStart(puzzle.difficulty, mode) }
            open(Game(puzzle, dailyDay = dailyDay, mode = mode, timeLimitSeconds = timeLimit))
            save()
        }
    }

    private fun open(game: Game) {
        _state.update { UiState(screen = Screen.GAME, game = game, menuMode = it.menuMode) }
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
        if (s.paused || game.isOver) return
        val next = change(s, game)
        var unlocked = emptyList<Achievement>()
        if (next.isSolved) {
            val before = _stats.value
            updateStats {
                val won = it.recordWin(
                    next.puzzle.difficulty, next.elapsedSeconds, next.mistakes, next.hintsUsed, next.mode,
                )
                next.dailyDay?.let(won::recordDailyWin) ?: won
            }
            unlocked = Achievement.entries.filter { it.isUnlocked(_stats.value) && !it.isUnlocked(before) }
        }
        _state.update { it.copy(game = next, hint = null, newAchievements = unlocked) }
        save()
        when {
            next.isSolved -> _feedback.tryEmit(Feedback.WIN)
            next.mistakes > game.mistakes -> _feedback.tryEmit(Feedback.ERROR)
            next.values != game.values && next.values.count { it != 0 } > game.values.count { it != 0 } ->
                _feedback.tryEmit(Feedback.PLACE)
        }
        if (next.isSolved) syncNow()
    }

    // --- Navegación ---

    override fun show(screen: Screen) {
        if (screen == Screen.MENU) {
            save()
            timer?.cancel()
            _state.update { UiState(saved = loadSaved(), menuMode = it.menuMode) }
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
        if (_state.value.screen == Screen.MENU) syncNow()
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
                val before = _state.value.game
                _state.update { s ->
                    if (s.paused || s.game == null || s.screen != Screen.GAME) s else s.copy(game = s.game.tick())
                }
                val after = _state.value.game
                if (before != null && after != null && !before.isLost && after.isLost) {
                    _feedback.tryEmit(Feedback.ERROR)
                    save()
                }
            }
        }
    }

    private fun total() = otherStats.fold(ownStats) { acc, s -> acc + s }

    private fun updateStats(f: (Stats) -> Stats) {
        ownStats = f(ownStats)
        prefs.edit { putString(KEY_STATS, ownStats.encode()) }
        _stats.value = total()
    }

    private fun save() {
        val game = _state.value.game ?: return
        prefs.edit {
            if (game.isOver) remove(KEY_SAVED) else putString(KEY_SAVED, game.encode())
            putLong(KEY_SAVED_AT, System.currentTimeMillis())
        }
    }

    // --- Copia en la nube (GitHub) ---

    /** Empieza el inicio de sesión: muestra un código para escribir en GitHub. */
    override fun connectGitHub() {
        if (!_cloud.value.configured || connectJob?.isActive == true) return
        connectJob = viewModelScope.launch {
            _cloud.update { it.copy(busy = true, error = null) }
            try {
                val code = github.requestDeviceCode()
                _cloud.update { it.copy(userCode = code.userCode, verificationUri = code.verificationUri) }
                val token = github.waitForToken(code)
                val login = github.login(token)
                prefs.edit {
                    putString(KEY_TOKEN, token)
                    putString(KEY_LOGIN, login)
                }
                _cloud.update { it.copy(login = login, userCode = null, verificationUri = null, busy = false) }
                syncNow()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _cloud.update { it.copy(busy = false, userCode = null, verificationUri = null, error = e.message) }
            }
        }
    }

    override fun cancelConnect() {
        connectJob?.cancel()
        _cloud.update { it.copy(busy = false, userCode = null, verificationUri = null) }
    }

    override fun disconnectGitHub() {
        prefs.edit {
            remove(KEY_TOKEN); remove(KEY_LOGIN); remove(KEY_GIST); remove(KEY_LAST_SYNC); remove(KEY_OTHER_STATS)
        }
        otherStats = emptyList()
        _stats.value = total()
        _cloud.update { CloudState(configured = it.configured) }
    }

    override fun syncNow() {
        val token = prefs.getString(KEY_TOKEN, null) ?: return
        if (_cloud.value.busy) return
        viewModelScope.launch {
            _cloud.update { it.copy(busy = true, error = null) }
            try {
                val local = CloudSync.SavedGame(prefs.getLong(KEY_SAVED_AT, 0), prefs.getString(KEY_SAVED, null))
                val result = cloudSync.sync(token, prefs.getString(KEY_GIST, null), deviceId, ownStats, local)
                otherStats = result.otherDevices
                val now = System.currentTimeMillis()
                prefs.edit {
                    putString(KEY_GIST, result.gistId)
                    putString(KEY_OTHER_STATS, otherStats.joinToString(SEPARATOR) { it.encode() })
                    putLong(KEY_LAST_SYNC, now)
                    // Partida más reciente de otro dispositivo: se guarda, salvo si se está jugando ahora.
                    if (result.game !== local && _state.value.screen != Screen.GAME) {
                        val text = result.game.game
                        if (text != null && Game.decode(text) != null) putString(KEY_SAVED, text) else remove(KEY_SAVED)
                        putLong(KEY_SAVED_AT, result.game.updatedAt)
                    }
                }
                _stats.value = total()
                if (_state.value.screen != Screen.GAME) _state.update { it.copy(saved = loadSaved()) }
                _cloud.update { it.copy(busy = false, lastSync = now) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (e.message == "HTTP 401") disconnectGitHub()
                _cloud.update { it.copy(busy = false, error = e.message ?: e.javaClass.simpleName) }
            }
        }
    }

    private fun loadSaved(): Game? = prefs.getString(KEY_SAVED, null)?.let(Game::decode)

    private companion object {
        const val KEY_SAVED = "saved_game"
        const val KEY_STATS = "stats"
        const val KEY_SAVED_AT = "saved_game_at"
        const val KEY_OTHER_STATS = "other_stats"
        const val KEY_DEVICE = "device_id"
        const val KEY_TOKEN = "github_token"
        const val KEY_LOGIN = "github_login"
        const val KEY_GIST = "github_gist"
        const val KEY_LAST_SYNC = "last_sync"
        const val SEPARATOR = "\n---\n"
    }
}
