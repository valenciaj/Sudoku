package com.valenciaj.sudoku.core

/** Estadísticas de un nivel. Los tiempos van en segundos. */
data class LevelStats(
    val played: Int = 0,
    val won: Int = 0,
    val perfect: Int = 0,
    val bestSeconds: Long? = null,
    val totalWonSeconds: Long = 0,
    /** Victorias sin pedir ninguna pista. */
    val noHints: Int = 0,
) {
    val averageSeconds: Long? get() = if (won == 0) null else totalWonSeconds / won
    val winRate: Int get() = if (played == 0) 0 else won * 100 / played
}

/** Sudokus diarios completados y racha de días seguidos. */
data class DailyStats(
    val completed: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val lastDay: Long? = null,
) {
    /** La racha sigue viva si se completó hoy o ayer. */
    fun currentStreak(today: Long) = if (lastDay != null && today - lastDay <= 1) streak else 0

    fun isDone(day: Long) = lastDay == day
}

/** Estadísticas de todos los niveles. Inmutable. */
data class Stats(
    val levels: Map<Difficulty, LevelStats> = emptyMap(),
    val daily: DailyStats = DailyStats(),
) {

    operator fun get(level: Difficulty) = levels[level] ?: LevelStats()

    val totalWon get() = levels.values.sumOf { it.won }

    fun recordStart(level: Difficulty) = update(level) { it.copy(played = it.played + 1) }

    fun recordWin(level: Difficulty, seconds: Long, mistakes: Int, hints: Int = 0) = update(level) {
        it.copy(
            won = it.won + 1,
            perfect = it.perfect + if (mistakes == 0) 1 else 0,
            bestSeconds = minOf(it.bestSeconds ?: seconds, seconds),
            totalWonSeconds = it.totalWonSeconds + seconds,
            noHints = it.noHints + if (hints == 0) 1 else 0,
        )
    }

    fun recordDailyWin(day: Long): Stats {
        if (daily.lastDay == day) return this
        val streak = if (daily.lastDay == day - 1) daily.streak + 1 else 1
        return copy(
            daily = DailyStats(
                completed = daily.completed + 1,
                streak = streak,
                bestStreak = maxOf(daily.bestStreak, streak),
                lastDay = day,
            ),
        )
    }

    /**
     * Suma las estadísticas de varios dispositivos (cada uno sube solo las suyas,
     * como un contador CRDT: así nunca se cuenta dos veces). Para el diario se queda
     * con la racha del dispositivo que jugó más recientemente.
     */
    operator fun plus(other: Stats): Stats {
        val merged = (levels.keys + other.levels.keys).associateWith { level ->
            val a = this[level]
            val b = other[level]
            LevelStats(
                played = a.played + b.played,
                won = a.won + b.won,
                perfect = a.perfect + b.perfect,
                bestSeconds = listOfNotNull(a.bestSeconds, b.bestSeconds).minOrNull(),
                totalWonSeconds = a.totalWonSeconds + b.totalWonSeconds,
                noHints = a.noHints + b.noHints,
            )
        }
        val latest = maxOf(daily, other.daily, compareBy { it.lastDay ?: Long.MIN_VALUE })
        val d = latest.copy(
            completed = daily.completed + other.daily.completed,
            bestStreak = maxOf(daily.bestStreak, other.daily.bestStreak),
        )
        return Stats(merged, d)
    }

    private fun update(level: Difficulty, f: (LevelStats) -> LevelStats) =
        copy(levels = levels + (level to f(this[level])))

    /**
     * Una línea por nivel: NIVEL;jugadas;ganadas;perfectas;mejor;total;sinPistas
     * y una línea DAILY;completados;racha;mejorRacha;último.
     */
    fun encode(): String {
        val lines = levels.entries.map { (level, s) ->
            listOf(level.name, s.played, s.won, s.perfect, s.bestSeconds ?: -1, s.totalWonSeconds, s.noHints)
                .joinToString(";")
        }
        val d = daily
        val dailyLine = listOf(DAILY, d.completed, d.streak, d.bestStreak, d.lastDay ?: -1).joinToString(";")
        return (lines + dailyLine).joinToString("\n")
    }

    companion object {
        private const val DAILY = "DAILY"

        fun decode(text: String?): Stats {
            if (text.isNullOrBlank()) return Stats()
            var daily = DailyStats()
            val levels = mutableMapOf<Difficulty, LevelStats>()
            for (line in text.lines()) {
                runCatching {
                    val p = line.split(";")
                    if (p[0] == DAILY) {
                        daily = DailyStats(p[1].toInt(), p[2].toInt(), p[3].toInt(), p[4].toLong().takeIf { it >= 0 })
                    } else {
                        levels[Difficulty.valueOf(p[0])] = LevelStats(
                            played = p[1].toInt(),
                            won = p[2].toInt(),
                            perfect = p[3].toInt(),
                            bestSeconds = p[4].toLong().takeIf { it >= 0 },
                            totalWonSeconds = p[5].toLong(),
                            noHints = p.getOrNull(6)?.toInt() ?: 0,
                        )
                    }
                }
            }
            return Stats(levels, daily)
        }
    }
}

/** Logros. Se calculan a partir de las estadísticas, así no hay que guardarlos aparte. */
enum class Achievement(val title: String, val description: String, val unlocked: (Stats) -> Boolean) {
    FIRST_WIN("Primera victoria", "Resuelve tu primer sudoku", { it.totalWon >= 1 }),
    PERFECT("Perfecto", "Resuelve un sudoku sin errores", { s -> s.levels.values.any { it.perfect > 0 } }),
    NO_HELP("Sin ayuda", "Resuelve un sudoku sin pistas", { s -> s.levels.values.any { it.noHints > 0 } }),
    FAST("Rayo", "Resuelve un Fácil en menos de 3 minutos", { (it[Difficulty.EASY].bestSeconds ?: Long.MAX_VALUE) < 180 }),
    ALL_LEVELS("Todoterreno", "Gana al menos una vez en cada nivel", { s -> Difficulty.entries.all { s[it].won > 0 } }),
    DEMENTIAL("Locura", "Supera un sudoku Demencial", { it[Difficulty.DEMENTIAL].won > 0 }),
    VETERAN("Veterano", "Gana 25 sudokus", { it.totalWon >= 25 }),
    DAILY_FIRST("Cita diaria", "Completa un sudoku diario", { it.daily.completed >= 1 }),
    STREAK_7("Constante", "Racha de 7 días con el sudoku diario", { it.daily.bestStreak >= 7 }),
    STREAK_30("Imparable", "Racha de 30 días con el sudoku diario", { it.daily.bestStreak >= 30 }),
    ;

    fun isUnlocked(stats: Stats) = unlocked(stats)
}
