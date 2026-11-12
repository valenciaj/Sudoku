package com.valenciaj.sudoku.cloud

import com.valenciaj.sudoku.core.Stats

/**
 * Sincronización con un Gist privado del usuario.
 *
 * Archivos del Gist:
 * - `stats-<dispositivo>.txt`: las estadísticas de cada dispositivo. Cada uno solo
 *   escribe el suyo y la app suma todos (así nunca se cuenta una partida dos veces).
 * - `partida.txt`: la partida en curso. Gana la más reciente (marca de tiempo en la 1ª línea).
 */
class CloudSync(private val client: GitHubClient) {

    /** Partida guardada con su hora de modificación. [game] null = no hay partida en curso. */
    data class SavedGame(val updatedAt: Long, val game: String?) {
        fun serialize() = "$updatedAt\n${game.orEmpty()}"

        companion object {
            fun parse(text: String): SavedGame? {
                val first = text.substringBefore("\n")
                val at = first.toLongOrNull() ?: return null
                return SavedGame(at, text.substringAfter("\n", "").ifBlank { null })
            }
        }
    }

    data class Result(val gistId: String, val otherDevices: List<Stats>, val game: SavedGame)

    suspend fun sync(token: String, gistId: String?, deviceId: String, ownStats: Stats, local: SavedGame): Result {
        var id = gistId ?: client.findGist(token, GIST_DESCRIPTION)
        // Si el Gist se borró (404), se crea uno nuevo.
        val files = id?.let {
            try {
                client.readFiles(token, it)
            } catch (e: GitHubClient.GitHubException) {
                if (e.message != "HTTP 404") throw e
                null
            }
        }
        if (files == null) id = null
        val ownFile = "stats-$deviceId.txt"

        val others = files.orEmpty()
            .filterKeys { it.startsWith("stats-") && it != ownFile }
            .values.map(Stats::decode)
        val remote = files?.get(GAME_FILE)?.let(SavedGame::parse)
        val winner = if (remote != null && remote.updatedAt > local.updatedAt) remote else local

        val upload = buildMap {
            put(ownFile, ownStats.encode().ifEmpty { " " })
            if (winner === local) put(GAME_FILE, local.serialize())
            if (id == null) put(README_FILE, README)
        }
        val finalId = if (id == null) {
            client.createGist(token, GIST_DESCRIPTION, upload)
        } else {
            client.updateGist(token, id, upload)
            id
        }
        return Result(finalId, others, winner)
    }

    companion object {
        const val GIST_DESCRIPTION = "Sudoku · copia en la nube (no editar)"
        private const val GAME_FILE = "partida.txt"
        private const val README_FILE = "LEEME.md"
        private const val README =
            "Copia en la nube de la app Sudoku: estadísticas de cada dispositivo y la partida en curso. " +
                "Puedes borrar este Gist para empezar de cero."
    }
}
