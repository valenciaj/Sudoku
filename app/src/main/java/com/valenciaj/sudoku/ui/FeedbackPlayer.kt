package com.valenciaj.sudoku.ui

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Eventos de juego que suenan o vibran. */
enum class Feedback { PLACE, ERROR, WIN }

/** Sonidos (tonos del sistema, sin archivos de audio) y vibración. */
class FeedbackPlayer(context: Context) {
    private val tone: ToneGenerator? = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 50) }.getOrNull()

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    fun play(feedback: Feedback, settings: Settings) {
        if (settings.sound) {
            when (feedback) {
                Feedback.PLACE -> tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 50)
                Feedback.ERROR -> tone?.startTone(ToneGenerator.TONE_PROP_NACK, 200)
                Feedback.WIN -> tone?.startTone(ToneGenerator.TONE_CDMA_CONFIRM, 600)
            }
        }
        if (settings.vibration && vibrator?.hasVibrator() == true) {
            val effect = when (feedback) {
                Feedback.PLACE -> VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE)
                Feedback.ERROR -> VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
                Feedback.WIN -> VibrationEffect.createWaveform(longArrayOf(0, 80, 60, 80, 60, 200), -1)
            }
            vibrator.vibrate(effect)
        }
    }

    fun release() {
        tone?.release()
    }
}
