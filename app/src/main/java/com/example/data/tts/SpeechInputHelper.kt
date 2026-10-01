package com.example.data.tts

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SpeechInputHelper(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _audioLevel = MutableStateFlow(0.0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private var onResultCallback: ((String) -> Unit)? = null
    private var onStartListeningCallback: (() -> Unit)? = null
    private var lastRecognizedText = ""
    private var lastSpeechTimestamp = 0L
    private var silenceCheckRunnable: Runnable? = null

    fun setOnStartListeningCallback(callback: () -> Unit) {
        this.onStartListeningCallback = callback
    }

    init {
        mainHandler.post {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    createRecognizer()
                } else {
                    Log.w("SpeechInputHelper", "Speech recognition not available on device")
                }
            } catch (e: Exception) {
                Log.e("SpeechInputHelper", "Initialization error: ${e.message}")
            }
        }
    }

    private fun createRecognizer() {
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                    onStartListeningCallback?.invoke()
                    lastSpeechTimestamp = System.currentTimeMillis()
                }

                override fun onBeginningOfSpeech() {
                    _isListening.value = true
                    onStartListeningCallback?.invoke()
                    lastSpeechTimestamp = System.currentTimeMillis()
                    scheduleSilenceCheck()
                }

                override fun onRmsChanged(rmsdB: Float) {
                    if (rmsdB > 2.0f) {
                        lastSpeechTimestamp = System.currentTimeMillis()
                    }
                    val normalized = ((rmsdB + 2.0f) / 12.0f).coerceIn(0.0f, 1.0f)
                    _audioLevel.value = normalized
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    cancelSilenceCheck()
                    _audioLevel.value = 0.0f
                    mainHandler.postDelayed({
                        if (_isListening.value && lastRecognizedText.isNotBlank()) {
                            finishAndEmitResult(lastRecognizedText)
                        } else {
                            _isListening.value = false
                        }
                    }, 500)
                }

                override fun onError(error: Int) {
                    cancelSilenceCheck()
                    _audioLevel.value = 0.0f
                    Log.e("SpeechInputHelper", "Speech error: $error")
                    if (lastRecognizedText.isNotBlank()) {
                        finishAndEmitResult(lastRecognizedText)
                    } else {
                        _isListening.value = false
                    }
                }

                override fun onResults(results: Bundle?) {
                    cancelSilenceCheck()
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: lastRecognizedText
                    finishAndEmitResult(text)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        lastRecognizedText = matches[0]
                        lastSpeechTimestamp = System.currentTimeMillis()
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    fun startListening(onResult: (String) -> Unit) {
        onResultCallback = onResult
        onStartListeningCallback?.invoke()
        lastRecognizedText = ""
        lastSpeechTimestamp = System.currentTimeMillis()

        mainHandler.post {
            try {
                if (speechRecognizer == null && SpeechRecognizer.isRecognitionAvailable(context)) {
                    createRecognizer()
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ru-RU")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите ваш вопрос ИИ-Автомеханику...")
                }

                _isListening.value = true
                speechRecognizer?.startListening(intent)
                scheduleSilenceCheck()
            } catch (e: Exception) {
                _isListening.value = false
                Log.e("SpeechInputHelper", "Failed to start listening: ${e.message}")
            }
        }
    }

    private fun scheduleSilenceCheck() {
        cancelSilenceCheck()
        silenceCheckRunnable = object : Runnable {
            override fun run() {
                val silentMillis = System.currentTimeMillis() - lastSpeechTimestamp
                if (_isListening.value && silentMillis >= 3000L) {
                    if (lastRecognizedText.isNotBlank()) {
                        finishAndEmitResult(lastRecognizedText)
                    } else {
                        stopListening()
                    }
                } else if (_isListening.value) {
                    mainHandler.postDelayed(this, 500)
                }
            }
        }
        mainHandler.postDelayed(silenceCheckRunnable!!, 500)
    }

    private fun cancelSilenceCheck() {
        silenceCheckRunnable?.let { mainHandler.removeCallbacks(it) }
        silenceCheckRunnable = null
    }

    private fun finishAndEmitResult(text: String) {
        cancelSilenceCheck()
        _isListening.value = false
        _audioLevel.value = 0.0f
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            // Ignore
        }
        val cleanText = text.trim()
        if (cleanText.isNotBlank()) {
            onResultCallback?.invoke(cleanText)
        }
    }

    fun stopListening() {
        mainHandler.post {
            cancelSilenceCheck()
            _audioLevel.value = 0.0f
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                // Ignore
            }
            _isListening.value = false
        }
    }

    fun destroy() {
        mainHandler.post {
            cancelSilenceCheck()
            _audioLevel.value = 0.0f
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                // Ignore
            }
            speechRecognizer = null
            _isListening.value = false
        }
    }
}

