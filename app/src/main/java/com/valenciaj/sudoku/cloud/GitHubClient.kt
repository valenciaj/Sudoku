package com.valenciaj.sudoku.cloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Cliente mínimo de GitHub para la copia en la nube.
 *
 * - Inicio de sesión con OAuth "device flow": la app muestra un código, el usuario lo
 *   escribe en github.com/login/device y la app recibe un token. No hace falta ningún
 *   secreto dentro de la app, solo el client ID (que es público).
 * - Solo pide el permiso `gist`: puede crear y editar Gists, nada más.
 */
class GitHubClient(private val clientId: String) {

    data class DeviceCode(
        val deviceCode: String,
        val userCode: String,
        val verificationUri: String,
        val intervalSeconds: Int,
        val expiresInSeconds: Int,
    )

    class GitHubException(message: String) : IOException(message)

    suspend fun requestDeviceCode(): DeviceCode {
        val json = postForm("https://github.com/login/device/code", mapOf("client_id" to clientId, "scope" to "gist"))
        json.optString("error").takeIf { it.isNotEmpty() }?.let { throw GitHubException(it) }
        return DeviceCode(
            deviceCode = json.getString("device_code"),
            userCode = json.getString("user_code"),
            verificationUri = json.getString("verification_uri"),
            intervalSeconds = json.optInt("interval", 5),
            expiresInSeconds = json.optInt("expires_in", 900),
        )
    }

    /** Espera a que el usuario autorice la app y devuelve el token. */
    suspend fun waitForToken(code: DeviceCode): String {
        var interval = code.intervalSeconds
        val deadline = System.currentTimeMillis() + code.expiresInSeconds * 1000L
        while (System.currentTimeMillis() < deadline) {
            delay(interval * 1000L)
            val json = postForm(
                "https://github.com/login/oauth/access_token",
                mapOf(
                    "client_id" to clientId,
                    "device_code" to code.deviceCode,
                    "grant_type" to "urn:ietf:params:oauth:grant-type:device_code",
                ),
            )
            json.optString("access_token").takeIf { it.isNotEmpty() }?.let { return it }
            when (val error = json.optString("error")) {
                "authorization_pending" -> Unit
                "slow_down" -> interval = json.optInt("interval", interval + 5)
                else -> throw GitHubException(error.ifEmpty { "unknown" })
            }
        }
        throw GitHubException("expired_token")
    }

    suspend fun login(token: String): String = JSONObject(api("GET", "/user", token)).getString("login")

    /** Busca el Gist de la app por su descripción. */
    suspend fun findGist(token: String, description: String): String? {
        for (page in 1..10) {
            val list = org.json.JSONArray(api("GET", "/gists?per_page=100&page=$page", token))
            if (list.length() == 0) return null
            for (i in 0 until list.length()) {
                val gist = list.getJSONObject(i)
                if (gist.optString("description") == description) return gist.getString("id")
            }
        }
        return null
    }

    suspend fun readFiles(token: String, gistId: String): Map<String, String> {
        val files = JSONObject(api("GET", "/gists/$gistId", token)).getJSONObject("files")
        return files.keys().asSequence().associateWith { files.getJSONObject(it).optString("content") }
    }

    suspend fun createGist(token: String, description: String, files: Map<String, String>): String {
        val body = JSONObject()
            .put("description", description)
            .put("public", false)
            .put("files", filesJson(files))
        return JSONObject(api("POST", "/gists", token, body)).getString("id")
    }

    suspend fun updateGist(token: String, gistId: String, files: Map<String, String>) {
        api("PATCH", "/gists/$gistId", token, JSONObject().put("files", filesJson(files)))
    }

    private fun filesJson(files: Map<String, String>) = JSONObject().apply {
        files.forEach { (name, content) -> put(name, JSONObject().put("content", content)) }
    }

    private suspend fun postForm(url: String, form: Map<String, String>): JSONObject = withContext(Dispatchers.IO) {
        val body = form.entries.joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, "UTF-8")}" }
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }
        conn.outputStream.use { it.write(body.toByteArray()) }
        JSONObject(read(conn))
    }

    private suspend fun api(method: String, path: String, token: String, body: JSONObject? = null): String =
        withContext(Dispatchers.IO) {
            val conn = (URL("https://api.github.com$path").openConnection() as HttpURLConnection).apply {
                // HttpURLConnection no admite PATCH; la API de GitHub acepta POST en su lugar.
                requestMethod = if (method == "PATCH") "POST" else method
                if (method == "PATCH") setRequestProperty("X-HTTP-Method-Override", "PATCH")
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
            }
            if (body != null) conn.outputStream.use { it.write(body.toString().toByteArray()) }
            read(conn)
        }

    private fun read(conn: HttpURLConnection): String {
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw GitHubException("HTTP $code")
            return text
        } finally {
            conn.disconnect()
        }
    }
}
