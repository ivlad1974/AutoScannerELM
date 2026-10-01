package com.example.data.sound

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.data.sound.MelodyPlayer.Note
import com.example.data.sound.MelodyPlayer.Notes

/**
 * Provides melodic audio feedback for Bluetooth adapter connection events:
 *  • Connect    — bright, cheerful ascending major chime (C–E–G–C up the scale).
 *  • Disconnect — slow, descending minor "sigh" chime (sad farewell melody).
 *
 * Melodies are synthesized on the fly by [MelodyPlayer] (no audio assets needed),
 * so they sound like a friendly musical notification rather than modem beeps.
 */
class SoundEffectsHelper(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var lastConnectToneTime = 0L
    private var lastDisconnectToneTime = 0L

    /**
     * Cheerful "everything is connected!" chime: fast ascending major arpeggio
     * ending on a high sparkle note (C5 → E5 → G5 → C6 style run).
     */
    fun playConnectSound() {
        val now = System.currentTimeMillis()
        if (now - lastConnectToneTime < 800L) return
        lastConnectToneTime = now

        scope.launch {
            try {
                MelodyPlayer.play(
                    listOf(
                        Note(Notes.C5, 110, 20),   // до — задорный старт
                        Note(Notes.E5, 110, 20),   // ми — вверх по мажору
                        Note(Notes.G5, 110, 20),   // соль — развитие
                        Note(Notes.C5, 90, 15),    // октавный подскок
                        Note(Notes.E5, 90, 15),
                        Note(Notes.G5, 130, 10),
                        Note(Notes.C5 * 2, 260, 0) // финальная «искорка» на две октавы выше
                    )
                )
            } catch (e: Exception) {
                Log.e("SoundEffectsHelper", "Error playing connect melody: ${e.message}")
            }
        }
    }

    /**
     * Sad "goodbye, adapter…" chime: slow descending minor phrases with a final
     * low sighing note (A4 → Ab4 → G4 → F4 → E4 → A4-down low long fade).
     */
    fun playDisconnectSound() {
        val now = System.currentTimeMillis()
        if (now - lastDisconnectToneTime < 800L) return
        lastDisconnectToneTime = now

        scope.launch {
            try {
                MelodyPlayer.play(
                    listOf(
                        Note(Notes.A4, 200, 40),     // ля — грустная нота
                        Note(Notes.Ab4, 200, 40),    // понижение полтона — «вздох»
                        Note(Notes.G4, 220, 40),
                        Note(Notes.F4, 220, 40),
                        Note(Notes.E4, 260, 50),
                        Note(Notes.A4 / 2, 520, 0)   // низкий долгий затухающий финал
                    )
                )
            } catch (e: Exception) {
                Log.e("SoundEffectsHelper", "Error playing disconnect melody: ${e.message}")
            }
        }
    }

    fun release() {
        // MelodyPlayer owns short-lived AudioTrack instances; nothing persistent to release.
    }
}
