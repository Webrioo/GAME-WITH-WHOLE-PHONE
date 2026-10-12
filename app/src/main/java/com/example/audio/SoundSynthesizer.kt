package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class SoundSynthesizer {
    private val sampleRate = 22050
    private var isSfxEnabled = true
    private var isMusicEnabled = true
    private var musicJob: Job? = null
    private val audioScope = CoroutineScope(Dispatchers.Default)

    fun setSfxEnabled(enabled: Boolean) {
        isSfxEnabled = enabled
    }

    fun setMusicEnabled(enabled: Boolean) {
        isMusicEnabled = enabled
        if (!enabled) {
            stopMusic()
        } else if (musicJob == null || musicJob?.isActive == false) {
            startMusic()
        }
    }

    fun playCoinSound(combo: Int = 1) {
        if (!isSfxEnabled) return
        audioScope.launch {
            // Pentatonic scale note based on combo: C5, D5, E5, G5, A5, C6
            val baseFreqs = floatArrayOf(523.25f, 587.33f, 659.25f, 783.99f, 880.00f, 1046.50f)
            val index = (combo - 1).coerceIn(0, baseFreqs.size - 1)
            val freq = baseFreqs[index]
            val durationMs = 80
            val numSamples = (sampleRate * durationMs / 1000)
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val envelope = 1.0 - (i.toDouble() / numSamples) // decay
                val wave = sin(2.0 * PI * freq * t) + 0.3 * sin(2.0 * PI * (freq * 2) * t)
                buffer[i] = (wave * envelope * Short.MAX_VALUE * 0.45).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            playShortBuffer(buffer)
        }
    }

    fun playJumpSound() {
        if (!isSfxEnabled) return
        audioScope.launch {
            val durationMs = 120
            val numSamples = (sampleRate * durationMs / 1000)
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                val currentFreq = 220.0 + progress * 350.0 // upward pitch sweep
                val wave = sin(2.0 * PI * currentFreq * (i.toDouble() / sampleRate))
                val env = sin(progress * PI) // bell curve
                buffer[i] = (wave * env * Short.MAX_VALUE * 0.4).toInt().toShort()
            }
            playShortBuffer(buffer)
        }
    }

    fun playSlideSound() {
        if (!isSfxEnabled) return
        audioScope.launch {
            val durationMs = 140
            val numSamples = (sampleRate * durationMs / 1000)
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                val currentFreq = 480.0 - progress * 260.0 // downward pitch whoosh
                val noise = (Math.random() * 2.0 - 1.0) * 0.25
                val wave = sin(2.0 * PI * currentFreq * (i.toDouble() / sampleRate)) + noise
                val env = 1.0 - progress
                buffer[i] = (wave * env * Short.MAX_VALUE * 0.35).toInt().toShort()
            }
            playShortBuffer(buffer)
        }
    }

    fun playPowerupSound() {
        if (!isSfxEnabled) return
        audioScope.launch {
            val freqs = doubleArrayOf(440.0, 554.37, 659.25, 880.0) // A major chord fanfare
            for (freq in freqs) {
                val durationMs = 60
                val numSamples = (sampleRate * durationMs / 1000)
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val env = 1.0 - (i.toDouble() / numSamples)
                    val wave = sin(2.0 * PI * freq * t)
                    buffer[i] = (wave * env * Short.MAX_VALUE * 0.4).toInt().toShort()
                }
                playShortBuffer(buffer)
                delay(40)
            }
        }
    }

    fun playShieldDeflectSound() {
        if (!isSfxEnabled) return
        audioScope.launch {
            val durationMs = 200
            val numSamples = (sampleRate * durationMs / 1000)
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                val wave = sin(2.0 * PI * 1200.0 * (i.toDouble() / sampleRate)) +
                        sin(2.0 * PI * 850.0 * (i.toDouble() / sampleRate)) * 0.5
                val env = (1.0 - progress) * (1.0 - progress)
                buffer[i] = (wave * env * Short.MAX_VALUE * 0.5).toInt().toShort()
            }
            playShortBuffer(buffer)
        }
    }

    fun playCrashSound() {
        if (!isSfxEnabled) return
        audioScope.launch {
            val durationMs = 280
            val numSamples = (sampleRate * durationMs / 1000)
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                val noise = (Math.random() * 2.0 - 1.0)
                val lowRumble = sin(2.0 * PI * (90.0 - progress * 40.0) * (i.toDouble() / sampleRate))
                val wave = (noise * 0.7 + lowRumble * 0.3)
                val env = (1.0 - progress)
                buffer[i] = (wave * env * Short.MAX_VALUE * 0.6).toInt().toShort()
            }
            playShortBuffer(buffer)
        }
    }

    fun startMusic() {
        if (!isMusicEnabled || musicJob?.isActive == true) return
        musicJob = audioScope.launch {
            // Retro sci-fi synth arpeggio progression: Am -> F -> C -> G
            val chordProgressions = arrayOf(
                doubleArrayOf(220.0, 261.63, 329.63, 440.0), // Am
                doubleArrayOf(174.61, 220.0, 261.63, 349.23), // F
                doubleArrayOf(130.81, 164.81, 196.00, 261.63), // C
                doubleArrayOf(196.00, 246.94, 293.66, 392.00)  // G
            )

            while (isActive && isMusicEnabled) {
                for (chord in chordProgressions) {
                    if (!isActive || !isMusicEnabled) break
                    for (step in 0 until 4) {
                        if (!isActive || !isMusicEnabled) break
                        val freq = chord[step]
                        val durationMs = 120
                        val numSamples = (sampleRate * durationMs / 1000)
                        val buffer = ShortArray(numSamples)

                        for (i in 0 until numSamples) {
                            val t = i.toDouble() / sampleRate
                            val env = 1.0 - (i.toDouble() / numSamples) * 0.7
                            val bass = sin(2.0 * PI * (freq / 2.0) * t) * 0.6
                            val lead = sin(2.0 * PI * freq * t) * 0.4
                            val wave = bass + lead
                            buffer[i] = (wave * env * Short.MAX_VALUE * 0.12).toInt().toShort()
                        }
                        playShortBuffer(buffer)
                        delay(130)
                    }
                }
            }
        }
    }

    fun stopMusic() {
        musicJob?.cancel()
        musicJob = null
    }

    private fun playShortBuffer(buffer: ShortArray) {
        var track: AudioTrack? = null
        try {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufSize = maxOf(buffer.size * 2, minBuf)
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            Thread.sleep((buffer.size * 1000L / sampleRate) + 20)
        } catch (_: Exception) {
        } finally {
            try {
                track?.stop()
                track?.release()
            } catch (_: Exception) {}
        }
    }
}
