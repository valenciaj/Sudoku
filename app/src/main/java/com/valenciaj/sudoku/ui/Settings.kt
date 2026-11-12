package com.valenciaj.sudoku.ui

import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.core.content.edit

enum class ThemeMode(val label: String) { SYSTEM("Sistema"), LIGHT("Claro"), DARK("Oscuro") }

enum class Accent(val label: String, val color: Color) {
    BLUE("Azul", Color(0xFF3F51B5)),
    GREEN("Verde", Color(0xFF2E7D32)),
    PURPLE("Morado", Color(0xFF7B1FA2)),
    ORANGE("Naranja", Color(0xFFE65100)),
    PINK("Rosa", Color(0xFFC2185B)),
}

data class Settings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: Accent = Accent.BLUE,
    /** Resaltar fila, columna y caja de la casilla elegida. */
    val highlightRelated: Boolean = true,
    /** Resaltar los números (y notas) iguales al elegido. */
    val highlightSame: Boolean = true,
    val sound: Boolean = true,
    val vibration: Boolean = true,
) {
    fun save(prefs: SharedPreferences) = prefs.edit {
        putString("theme", themeMode.name)
        putString("accent", accent.name)
        putBoolean("highlight_related", highlightRelated)
        putBoolean("highlight_same", highlightSame)
        putBoolean("sound", sound)
        putBoolean("vibration", vibration)
    }

    companion object {
        fun load(prefs: SharedPreferences) = Settings(
            themeMode = enumOr(prefs.getString("theme", null), ThemeMode.SYSTEM),
            accent = enumOr(prefs.getString("accent", null), Accent.BLUE),
            highlightRelated = prefs.getBoolean("highlight_related", true),
            highlightSame = prefs.getBoolean("highlight_same", true),
            sound = prefs.getBoolean("sound", true),
            vibration = prefs.getBoolean("vibration", true),
        )

        private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
            enumValues<T>().firstOrNull { it.name == name } ?: default
    }
}
