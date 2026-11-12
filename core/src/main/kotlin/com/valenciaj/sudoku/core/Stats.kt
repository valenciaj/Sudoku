package com.valenciaj.sudoku.core

/** Estadísticas de un nivel. Los tiempos van en segundos. */
data class LevelStats(
    val played: Int = 0,
    val won: Int = 0,
    val perfect: Int = 0,
    val bestSeconds: Long? = null,
    val totalWonSeconds: Long = 0,
) {
    val averageSeconds: Long? get() = if (won == 0) null else totalWonSeconds / won
    val winRate: Int get() = if (played == 0) 0 else won * 100 / played
}

/** Estadísticas de todos los niveles. Inmutable. */
data class Stats(val levels: Map<Difficulty, LevelStats> = emptyMap()) {

    operator fun get(level: Difficulty) = levels[level] ?: LevelStats()

    fun recordStart(level: Difficulty) = update(level) { it.copy(played = it.played + 1) }

    fun recordWin(level: Difficulty, seconds: Long, mistakes: Int) = update(level) {
        it.copy(
            won = it.won + 1,
            perfect = it.perfect + if (mistakes == 0) 1 else 0,
            bestSeconds = minOf(it.bestSeconds ?: seconds, seconds),
            totalWonSeconds = it.totalWonSeconds + seconds,
        )
    }

    private fun update(level: Difficulty, f: (LevelStats) -> LevelStats) =
        copy(levels = levels + (level to f(this[level])))

    /** Una línea por nivel: NIVEL;jugadas;ganadas;perfectas;mejor;total. */
    fun encode(): String = levels.entries.joinToString("\n") { (level, s) ->
        listOf(level.name, s.played, s.won, s.perfect, s.bestSeconds ?: -1, s.totalWonSeconds).joinToString(";")
    }

    companion object {
        fun decode(text: String?): Stats {
            if (text.isNullOrBlank()) return Stats()
            val levels = text.lines().mapNotNull { line ->
                runCatching {
                    val p = line.split(";")
                    Difficulty.valueOf(p[0]) to LevelStats(
                        played = p[1].toInt(),
                        won = p[2].toInt(),
                        perfect = p[3].toInt(),
                        bestSeconds = p[4].toLong().takeIf { it >= 0 },
                        totalWonSeconds = p[5].toLong(),
                    )
                }.getOrNull()
            }.toMap()
            return Stats(levels)
        }
    }
}
