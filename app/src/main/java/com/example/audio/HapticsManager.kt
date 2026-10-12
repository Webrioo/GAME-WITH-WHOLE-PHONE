package com.example.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticsManager(context: Context) {
    private var isEnabled = true
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun isHapticsEnabled(): Boolean = isEnabled

    fun vibrateTap() {
        if (!isEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(15)
            }
        } catch (_: Exception) {}
    }

    fun vibrateJump() {
        if (!isEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(25, 120))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(25)
            }
        } catch (_: Exception) {}
    }

    fun vibrateSlide() {
        if (!isEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(35, 100))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(35)
            }
        } catch (_: Exception) {}
    }

    fun vibrateCoin() {
        if (!isEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(10, 80))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(10)
            }
        } catch (_: Exception) {}
    }

    fun vibrateCrash() {
        if (!isEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 50, 40, 90)
                val amplitudes = intArrayOf(0, 255, 0, 180)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(160)
            }
        } catch (_: Exception) {}
    }
}
