package com.example.data.sound

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/**
 * Tiny built-in chime synthesizer: generates short PCM melodies (sine waves with
 * exponential decay envelopes and light harmonics) and plays them through
 * AudioTrack on STREAM_MUSIC. No external audio assets are required.
 */
object MelodyPlayer {

    private const val SAMPLE_RATE = 44100

    /**
     * Note: frequency in Hz, duration in milliseconds, gap after note in ms.
     */
    data class Note(
        val freq: Double,
        val durationMs: Int,
        val gapMs: Int = 30
    )

    @Volatile
    private var isPlaying = false

    /**
     * Synthesizes and plays the given melody synchronously (call from a background thread).
     */
    fun play(melody: List<Note>) {
        if (isPlaying) return
        isPlaying = true
        try {
            val totalMs = melody.sumOf { it.durationMs + it.gapMs }
            val sampleCount = (SAMPLE_RATE * totalMs / 1000L).toInt().coerceAtLeast(1)
            val buffer = ShortArray(sampleCount)

            var pos = 0
            for (note in melody) {
                val noteSamples = SAMPLE_RATE * note.durationMs / 1000
                if (noteSamples > 0) {
                    val attackLen = (0.08 * noteSamples).toInt().coerceAtLeast(1)
                    for (i in 0 until noteSamples) {
                        val t = i.toDouble() / SAMPLE_RATE
                        // Fast attack, smooth release envelope + exponential decay
                        val envLen = minOf(i, noteSamples - 1 - i, attackLen - 1).coerceAtLeast(0)
                        val envelope = sin(PI * 0.5 * envLen / attackLen)
                        val decay = Math.exp(-2.2 * t / (note.durationMs / 1000.0))
                        // Fundamental + soft harmonics for a pleasant bell-like timbre
                        val wave = sin(2 * PI * note.freq * t) +
                                0.35 * sin(2 * PI * note.freq * 2 * t) +
                                0.12 * sin(2 * PI * note.freq * 3 * t)
                        val sample = wave * envelope * decay * 0.55
                        val idx = pos + i
                        if (idx < buffer.size) {
                            buffer[idx] = sample.shortOrClamp()
                        }
                    }
                }
                pos += noteSamples + SAMPLE_RATE * note.gapMs / 1000
            }

            val minBuf = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(buffer.size * 2)

            val track = AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf,
                AudioTrack.MODE_STATIC
            )
            try {
                track.write(buffer, 0, buffer.size)
                track.setNotificationMarkerPosition(buffer.size)
                track.play()
                // Safety wait: static mode tracks play fully from memory.
                val maxWaitMs = (totalMs + 400L).coerceAtMost(6000L)
                val start = System.currentTimeMillis()
                while (track.playState != AudioTrack.PLAYSTATE_STOPPED &&
                    System.currentTimeMillis() - start < maxWaitMs
                ) {
                    Thread.sleep(40)
                }
            } finally {
                try {
                    track.stop()
                } catch (_: IllegalStateException) {
                }
                track.release()
            }
        } catch (_: Exception) {
            // Never crash sound feedback of the diagnostic session
        } finally {
            isPlaying = false
        }
    }

    private fun Double.shortOrClamp(): Short {
        val v = (this * Short.MAX_VALUE).toInt()
        return v.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }

    // Equal-tempered note frequencies (Hz), octave 4/5 — used by connect/disconnect chimes
    object Notes {
        val C4 = 261.63
        val D4 = 293.66
        val E4 = 329.63
        val F4 = 349.23
        val G4 = 392.00
        val A4 = 440.00
        val B4 = 493.88
        val C5 = 523.25
        val D5 = 587.33
        val E5 = 659.25
        val G5 = 783.99
        val Ab4 = 415.30
        val Eb4 = 311.13
        val Db5 = 554.37
    }
}
