package com.valenciaj.sudoku.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.valenciaj.sudoku.MainActivity
import com.valenciaj.sudoku.R
import com.valenciaj.sudoku.core.Daily
import com.valenciaj.sudoku.core.Difficulty
import com.valenciaj.sudoku.core.Stats
import java.time.LocalDate

/** Widget de la pantalla de inicio: el sudoku diario de hoy y la racha. */
class DailyWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, views(context)) }
    }

    companion object {
        /** Redibuja todos los widgets (al cambiar las estadísticas). */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, DailyWidget::class.java))
            if (ids.isNotEmpty()) ids.forEach { manager.updateAppWidget(it, views(context)) }
        }

        private fun views(context: Context): RemoteViews {
            val today = LocalDate.now().toEpochDay()
            val stats = totalStats(context)
            val level = context.getString(levelRes(Daily.difficultyFor(today)))
            val status = context.getString(if (stats.daily.isDone(today)) R.string.daily_done else R.string.daily_play)
            val streak = stats.daily.currentStreak(today)
            val open = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            return RemoteViews(context.packageName, R.layout.widget_daily).apply {
                setTextViewText(R.id.widget_level, level)
                setTextViewText(R.id.widget_status, status)
                setTextViewText(R.id.widget_streak, if (streak > 0) context.getString(R.string.widget_streak, streak) else "")
                setOnClickPendingIntent(R.id.widget_root, open)
            }
        }

        /** Estadísticas de este dispositivo más las de otros bajadas de la nube. */
        fun totalStats(context: Context): Stats {
            val prefs = context.getSharedPreferences("sudoku", Context.MODE_PRIVATE)
            val own = Stats.decode(prefs.getString("stats", null))
            val others = prefs.getString("other_stats", null)?.split("\n---\n")?.map(Stats::decode).orEmpty()
            return others.fold(own) { acc, s -> acc + s }
        }

        fun levelRes(level: Difficulty) = when (level) {
            Difficulty.EASY -> R.string.level_easy
            Difficulty.MEDIUM -> R.string.level_medium
            Difficulty.HARD -> R.string.level_hard
            Difficulty.DEMENTIAL -> R.string.level_dementia
        }
    }
}
