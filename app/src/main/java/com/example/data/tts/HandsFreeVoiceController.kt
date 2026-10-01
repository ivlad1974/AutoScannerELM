package com.example.data.tts

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Intelligent In-Car Voice Assistant Controller.
 * Implements a true conversational lifecycle:
 * 1. Waiting for wake word («Автоскан»): Microphone stays continuously active without periodic cycling or beeps.
 *    Background talk and words like "температура" are ignored until the wake word is spoken.
 * 2. Wake phrase detected: Assistant acknowledges with code response («Слушаю вас») and enters dialog mode.
 * 3. Analysis & Voice Response: Assistant analyzes query and speaks the result.
 * 4. Open Dialog (5-second follow-up window): After responding, assistant waits up to 5 seconds for
 *    follow-up questions without requiring the wake word. If silence exceeds 5 seconds, dialog ends
 *    and system returns to waiting for the wake word.
 */
class HandsFreeVoiceController(
    private val context: Context,
    private val ttsHelper: TextToSpeechHelper
) {
    enum class AssistantWorkMode(val label: String, val description: String) {
        CONTINUOUS_DRIVE_SESSION(
            "Голосовой эфир за рулем (Hands-Free)",
            "Микрофон непрерывно слушает кодовое слово «Автоскан». После ответа открывается диалог на 5 сек для уточнений без кодового слова."
        ),
        OFF(
            "Выключен",
            "Фоновый голосовой ассистент отключен."
        )
    }

    enum class HandsFreeMode {
        IDLE,                   // Assistant dormant
        LISTENING_CONTINUOUS,   // Actively listening for wake word (always on, ignores other words)
        AWAITING_COMMAND,       // Acknowledged or follow-up dialog window (active for 5 seconds)
        PROCESSING,             // Parsing, executing command and speaking answer
        TESTING_WAKE_WORD       // Interactive test in settings dialog
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _workMode = MutableStateFlow(AssistantWorkMode.OFF)
    val workMode: StateFlow<AssistantWorkMode> = _workMode.asStateFlow()

    private val _mode = MutableStateFlow(HandsFreeMode.IDLE)
    val mode: StateFlow<HandsFreeMode> = _mode.asStateFlow()

    private val _isHandsFreeActive = MutableStateFlow(false)
    val isHandsFreeActive: StateFlow<Boolean> = _isHandsFreeActive.asStateFlow()

    private val _wakePhrase = MutableStateFlow("Автоскан")
    val wakePhrase: StateFlow<String> = _wakePhrase.asStateFlow()

    private val _readyResponse = MutableStateFlow("Слушаю вас")
    val readyResponse: StateFlow<String> = _readyResponse.asStateFlow()

    private val _lastDetectedPhrase = MutableStateFlow("")
    val lastDetectedPhrase: StateFlow<String> = _lastDetectedPhrase.asStateFlow()

    private val _audioLevel = MutableStateFlow(0.0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var isMutedByController = false

    private var onCommandDispatched: ((String) -> Unit)? = null
    private var onTestResultCallback: ((Boolean, String) -> Unit)? = null
    private var testTargetWakePhrase: String = ""
    private var testTargetResponsePhrase: String = ""
    private var isDestroyed = false

    private var restartRunnable: Runnable? = null
    private var testTimeoutRunnable: Runnable? = null
    private var dialogInactivityRunnable: Runnable? = null

    init {
        scope.launch {
            ttsHelper.isSpeaking.collect { isSpeaking ->
                if (isSpeaking) {
                    // TTS is currently outputting sound through speaker.
                    // Stop microphone immediately to prevent acoustic feedback/loopback!
                    cancelRestart()
                    cancelDialogInactivityTimer()
                    stopSpeechRecognizerSafely()
                    _audioLevel.value = 0.0f
                } else {
                    // TTS has finished speaking!
                    // Wait a 250ms decay margin so audio echo dissipates before opening microphone
                    if (!isDestroyed && _isHandsFreeActive.value) {
                        mainHandler.postDelayed({
                            if (!isDestroyed && _isHandsFreeActive.value && !ttsHelper.isSpeaking.value) {
                                when (_mode.value) {
                                    HandsFreeMode.AWAITING_COMMAND -> {
                                        // The prompt ("Слушаю вас") finished speaking: open mic and wait 5 sec for user question!
                                        _lastDetectedPhrase.value = "Слушаю команду (5 сек)..."
                                        launchRecognizerInternal(prompt = null)
                                        startDialogInactivityTimer(5000L)
                                    }
                                    HandsFreeMode.PROCESSING -> {
                                        // AI just finished speaking the answer!
                                        // Start follow-up open dialog for 5 seconds without requiring wake word:
                                        _mode.value = HandsFreeMode.AWAITING_COMMAND
                                        _lastDetectedPhrase.value = "Диалог открыт: жду вопрос (5 сек)..."
                                        launchRecognizerInternal(prompt = null)
                                        startDialogInactivityTimer(5000L)
                                    }
                                    HandsFreeMode.LISTENING_CONTINUOUS -> {
                                        _lastDetectedPhrase.value = "Ожидание кодового слова: «${_wakePhrase.value}»"
                                        launchRecognizerInternal(prompt = null)
                                    }
                                    else -> {}
                                }
                            }
                        }, 250L)
                    }
                }
            }
        }
    }

    fun setCommandDispatcher(dispatcher: (String) -> Unit) {
        this.onCommandDispatched = dispatcher
    }

    fun setWorkMode(newMode: AssistantWorkMode) {
        _workMode.value = newMode
        if (newMode == AssistantWorkMode.CONTINUOUS_DRIVE_SESSION) {
            _isHandsFreeActive.value = true
            startContinuousListeningSession()
        } else {
            _isHandsFreeActive.value = false
            stopHandsFree()
        }
    }

    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun updateSettings(
        wakePhrase: String,
        readyResponse: String,
        isEnabled: Boolean,
        workMode: AssistantWorkMode = _workMode.value
    ) {
        val cleanWake = wakePhrase.trim().ifBlank { "Автоскан" }
        val cleanResp = readyResponse.trim().ifBlank { "Слушаю вас" }
        _wakePhrase.value = cleanWake
        _readyResponse.value = cleanResp
        _workMode.value = workMode
        val wasActive = _isHandsFreeActive.value
        _isHandsFreeActive.value = isEnabled

        if (isEnabled && (!wasActive || speechRecognizer == null)) {
            startContinuousListeningSession()
        } else if (!isEnabled && wasActive) {
            stopHandsFree()
        }
    }

    /**
     * Parses comma-separated ready response phrases.
     */
    fun getReadyResponseVariants(raw: String = _readyResponse.value): List<String> {
        val list = raw.split(",", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        return if (list.isNotEmpty()) list else listOf("Слушаю вас")
    }

    /**
     * Selects one phrase at random from the comma-separated options.
     */
    fun getRandomReadyPhrase(raw: String = _readyResponse.value): String {
        val variants = getReadyResponseVariants(raw)
        return variants.random()
    }

    /**
     * Starts continuous listening session for wake word.
     * The microphone stays constantly active without multi-second pauses.
     */
    fun startContinuousListeningSession() {
        if (isDestroyed) return
        if (!hasAudioPermission()) {
            _lastDetectedPhrase.value = "Требуется разрешение на микрофон (RECORD_AUDIO)"
            return
        }

        cancelAllTimeouts()
        _isHandsFreeActive.value = true
        _mode.value = HandsFreeMode.LISTENING_CONTINUOUS
        _lastDetectedPhrase.value = "Ожидание кодового слова: «${_wakePhrase.value}»"

        silenceSystemStreams()
        launchRecognizerInternal(prompt = null)
    }

    /**
     * One-shot trigger for Push-To-Talk button in UI.
     */
    fun startOneShotAsk(onResult: (String) -> Unit) {
        if (isDestroyed) return
        if (!hasAudioPermission()) return

        cancelAllTimeouts()
        stopSpeechRecognizerSafely()

        _mode.value = HandsFreeMode.AWAITING_COMMAND
        _lastDetectedPhrase.value = "Слушаю вопрос..."

        mainHandler.post {
            try {
                createSpeechRecognizer(oneShotCallback = onResult)
                val intent = createSpeechIntent(prompt = "Задайте вопрос автомеханику...")
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e("HandsFreeController", "Error starting one-shot ask: ${e.message}")
                _mode.value = HandsFreeMode.IDLE
            }
        }
    }

    private fun launchRecognizerInternal(prompt: String?) {
        if (isDestroyed || !_isHandsFreeActive.value) return
        if (!hasAudioPermission()) return
        if (ttsHelper.isSpeaking.value) return

        cancelRestart()

        mainHandler.post {
            if (isDestroyed || !_isHandsFreeActive.value || ttsHelper.isSpeaking.value) return@post
            try {
                if (speechRecognizer == null) {
                    createSpeechRecognizer()
                } else {
                    try { speechRecognizer?.cancel() } catch (e: Exception) {}
                }

                val intent = createSpeechIntent(prompt = prompt)
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e("HandsFreeController", "Recognizer launch error: ${e.message}")
                if (_isHandsFreeActive.value && !isDestroyed) {
                    scheduleDelayedRestart(250L)
                }
            }
        }
    }

    /**
     * Wake phrase was detected. Acknowledges with code response and opens 5-second command window.
     */
    fun triggerActiveCommandListening() {
        if (isDestroyed || !_isHandsFreeActive.value) return
        cancelAllTimeouts()
        stopSpeechRecognizerSafely()

        _mode.value = HandsFreeMode.AWAITING_COMMAND
        val phraseToSpeak = getRandomReadyPhrase()
        _lastDetectedPhrase.value = "⚡ «$phraseToSpeak»"

        // Speak ready response. When TTS finishes speaking, isSpeaking collector opens mic and starts 5s timer.
        ttsHelper.speak(phraseToSpeak, overrideToggle = true)

        // Safety fallback in case TTS is not initialized or silent
        mainHandler.postDelayed({
            if (!isDestroyed && _isHandsFreeActive.value && _mode.value == HandsFreeMode.AWAITING_COMMAND && !ttsHelper.isSpeaking.value && speechRecognizer == null) {
                launchRecognizerInternal(prompt = null)
                startDialogInactivityTimer(5000L)
            }
        }, 2500L)
    }

    /**
     * Dedicated interactive test for the settings dialog.
     */
    fun testWakeWordRecognition(
        wakePhraseToTest: String,
        readyResponseToTest: String,
        onResult: (matched: Boolean, recognizedText: String) -> Unit
    ) {
        if (isDestroyed) return
        if (!hasAudioPermission()) {
            onResult(false, "Ошибка: нет разрешения на микрофон")
            return
        }

        cancelAllTimeouts()
        stopSpeechRecognizerSafely()

        testTargetWakePhrase = wakePhraseToTest.trim().ifBlank { _wakePhrase.value }
        testTargetResponsePhrase = readyResponseToTest.trim().ifBlank { _readyResponse.value }
        onTestResultCallback = onResult
        _mode.value = HandsFreeMode.TESTING_WAKE_WORD
        _lastDetectedPhrase.value = "Говорите: «$testTargetWakePhrase»..."

        testTimeoutRunnable = Runnable {
            if (_mode.value == HandsFreeMode.TESTING_WAKE_WORD) {
                _lastDetectedPhrase.value = "Время ожидания истекло"
                onTestResultCallback?.invoke(false, "Время ожидания истекло")
                onTestResultCallback = null
                if (_isHandsFreeActive.value) {
                    startContinuousListeningSession()
                } else {
                    _mode.value = HandsFreeMode.IDLE
                }
            }
        }
        mainHandler.postDelayed(testTimeoutRunnable!!, 8000)

        mainHandler.post {
            try {
                createSpeechRecognizer()
                val intent = createSpeechIntent(prompt = null)
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                onTestResultCallback?.invoke(false, "Ошибка запуска: ${e.message}")
                onTestResultCallback = null
            }
        }
    }

    fun testResponseVoice(text: String): String {
        val phrase = getRandomReadyPhrase(text.trim().ifBlank { _readyResponse.value })
        ttsHelper.speak(phrase, overrideToggle = true)
        return phrase
    }

    fun stopHandsFree() {
        _isHandsFreeActive.value = false
        _workMode.value = AssistantWorkMode.OFF
        _mode.value = HandsFreeMode.IDLE
        _lastDetectedPhrase.value = "Помощник отключен"
        _audioLevel.value = 0.0f
        cancelAllTimeouts()
        restoreSystemStreams()
        stopSpeechRecognizerSafely()
    }

    private fun createSpeechRecognizer(oneShotCallback: ((String) -> Unit)? = null) {
        try {
            speechRecognizer?.setRecognitionListener(null)
            speechRecognizer?.destroy()
        } catch (e: Exception) {}

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w("HandsFreeController", "Speech recognition unavailable on device")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}

                override fun onBeginningOfSpeech() {
                    if (!_isHandsFreeActive.value && _mode.value != HandsFreeMode.TESTING_WAKE_WORD && oneShotCallback == null) return
                    // If user started speaking while in follow-up dialog, postpone/pause dialog timer so they aren't cut off
                    if (_mode.value == HandsFreeMode.AWAITING_COMMAND) {
                        cancelDialogInactivityTimer()
                    }
                }

                override fun onRmsChanged(rmsdB: Float) {
                    if (!_isHandsFreeActive.value && _mode.value != HandsFreeMode.TESTING_WAKE_WORD && oneShotCallback == null) {
                        _audioLevel.value = 0.0f
                        return
                    }
                    val normalized = ((rmsdB + 2.0f) / 12.0f).coerceIn(0.0f, 1.0f)
                    _audioLevel.value = normalized
                }

                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    if (oneShotCallback != null) {
                        _mode.value = HandsFreeMode.IDLE
                        return
                    }

                    if (_mode.value == HandsFreeMode.TESTING_WAKE_WORD) {
                        cancelTestTimeout()
                        val errMsg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "Слово не распознано (тишина)"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Таймаут речи"
                            else -> "Ошибка распознавания ($error)"
                        }
                        _lastDetectedPhrase.value = errMsg
                        onTestResultCallback?.invoke(false, errMsg)
                        onTestResultCallback = null
                        if (_isHandsFreeActive.value && !isDestroyed) {
                            scheduleDelayedRestart(500L)
                        } else {
                            _mode.value = HandsFreeMode.IDLE
                        }
                        return
                    }

                    if (!_isHandsFreeActive.value || isDestroyed) {
                        _mode.value = HandsFreeMode.IDLE
                        return
                    }

                    when (_mode.value) {
                        HandsFreeMode.LISTENING_CONTINUOUS -> {
                            // Silence or speech timeout waiting for wake word:
                            // RESTART IMMEDIATELY with minimal delay (60ms) so the microphone is always on!
                            val restartDelay = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH,
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 60L
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 200L
                                else -> 350L
                            }
                            scheduleDelayedRestart(restartDelay)
                        }

                        HandsFreeMode.AWAITING_COMMAND -> {
                            // In open dialog (within 5 seconds):
                            if (dialogInactivityRunnable == null) {
                                // 5 seconds expired -> close dialog, return to waiting for wake word
                                _mode.value = HandsFreeMode.LISTENING_CONTINUOUS
                                _lastDetectedPhrase.value = "Ожидание кодового слова: «${_wakePhrase.value}»"
                                scheduleDelayedRestart(60L)
                            } else {
                                // Still within 5-second window, keep listening for user input
                                val restartDelay = when (error) {
                                    SpeechRecognizer.ERROR_NO_MATCH,
                                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 60L
                                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 200L
                                    else -> 350L
                                }
                                scheduleDelayedRestart(restartDelay)
                            }
                        }

                        else -> {
                            scheduleDelayedRestart(400L)
                        }
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: emptyList()
                    val best = matches.firstOrNull() ?: ""

                    if (oneShotCallback != null) {
                        _mode.value = HandsFreeMode.IDLE
                        if (best.isNotBlank()) {
                            oneShotCallback.invoke(best)
                        }
                        return
                    }

                    if (!_isHandsFreeActive.value && _mode.value != HandsFreeMode.TESTING_WAKE_WORD) {
                        _mode.value = HandsFreeMode.IDLE
                        return
                    }

                    handleRecognizedResults(matches, best)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    if (!_isHandsFreeActive.value && _mode.value != HandsFreeMode.TESTING_WAKE_WORD) {
                        return
                    }
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: emptyList()
                    val candidate = matches.firstOrNull() ?: return

                    if (_mode.value == HandsFreeMode.LISTENING_CONTINUOUS || _mode.value == HandsFreeMode.AWAITING_COMMAND) {
                        _lastDetectedPhrase.value = "Слышу: «$candidate»..."
                    } else if (_mode.value == HandsFreeMode.TESTING_WAKE_WORD) {
                        for (item in matches) {
                            if (containsWakePhrase(item, testTargetWakePhrase)) {
                                try { speechRecognizer?.stopListening() } catch (e: Exception) {}
                                handleRecognizedResults(listOf(item), item)
                                break
                            }
                        }
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun handleRecognizedResults(candidates: List<String>, bestSpoken: String) {
        if (!_isHandsFreeActive.value && _mode.value != HandsFreeMode.TESTING_WAKE_WORD) {
            _mode.value = HandsFreeMode.IDLE
            return
        }

        when (_mode.value) {
            HandsFreeMode.TESTING_WAKE_WORD -> {
                cancelTestTimeout()
                val targetWake = testTargetWakePhrase.ifBlank { _wakePhrase.value }
                val matchedCandidate = candidates.firstOrNull { containsWakePhrase(it, targetWake) }

                if (matchedCandidate != null || containsWakePhrase(bestSpoken, targetWake)) {
                    val recognized = matchedCandidate ?: bestSpoken
                    val chosenResponse = getRandomReadyPhrase(testTargetResponsePhrase)
                    _lastDetectedPhrase.value = "✅ Распознано: «$recognized»"
                    ttsHelper.speak(chosenResponse, overrideToggle = true)
                    onTestResultCallback?.invoke(true, "$recognized (ответ: «$chosenResponse»)")
                } else {
                    _lastDetectedPhrase.value = "⚠️ Услышано: «$bestSpoken»"
                    onTestResultCallback?.invoke(false, bestSpoken)
                }
                onTestResultCallback = null
                if (_isHandsFreeActive.value && !isDestroyed) {
                    scheduleDelayedRestart(1000L)
                } else {
                    _mode.value = HandsFreeMode.IDLE
                }
            }

            HandsFreeMode.LISTENING_CONTINUOUS -> {
                val targetWake = _wakePhrase.value
                val matchedCandidate = candidates.firstOrNull { containsWakePhrase(it, targetWake) }

                if (matchedCandidate != null) {
                    // WAKE WORD DETECTED!
                    val cleanCommand = stripWakePhrase(matchedCandidate, targetWake)
                    if (cleanCommand.isNotBlank()) {
                        // User said wake word and command together (e.g. "Автоскан, какая температура?")
                        cancelAllTimeouts()
                        stopSpeechRecognizerSafely()
                        _mode.value = HandsFreeMode.PROCESSING
                        _lastDetectedPhrase.value = "🧠 «$cleanCommand»"
                        onCommandDispatched?.invoke(cleanCommand)
                    } else {
                        // User said only the wake word (e.g. "Автоскан")
                        triggerActiveCommandListening()
                    }
                } else {
                    // NO WAKE WORD DETECTED!
                    // E.g. user or passenger said "температура", "погода", or normal car conversation:
                    // STRICTLY DO NOT ACTIVATE! Ignore speech and immediately continue listening for wake word!
                    _lastDetectedPhrase.value = "Ожидание кодового слова: «${_wakePhrase.value}»"
                    scheduleDelayedRestart(60L)
                }
            }

            HandsFreeMode.AWAITING_COMMAND -> {
                // Open dialog window (assistant previously responded and is actively waiting for user question/command)
                if (bestSpoken.isNotBlank()) {
                    if (isStopCommand(bestSpoken)) {
                        cancelDialogInactivityTimer()
                        _mode.value = HandsFreeMode.LISTENING_CONTINUOUS
                        _lastDetectedPhrase.value = "Диалог завершен. Ожидание «${_wakePhrase.value}»"
                        scheduleDelayedRestart(60L)
                        return
                    }

                    // Any spoken question or command in this state is dispatched directly to the AI
                    cancelDialogInactivityTimer()
                    _mode.value = HandsFreeMode.PROCESSING
                    val cleanCommand = stripWakePhrase(bestSpoken, _wakePhrase.value).ifBlank { bestSpoken }
                    _lastDetectedPhrase.value = "🧠 «$cleanCommand»"
                    stopSpeechRecognizerSafely()
                    onCommandDispatched?.invoke(cleanCommand)
                } else {
                    // Empty speech result
                    if (dialogInactivityRunnable == null) {
                        _mode.value = HandsFreeMode.LISTENING_CONTINUOUS
                        _lastDetectedPhrase.value = "Диалог завершен. Ожидание «${_wakePhrase.value}»"
                        scheduleDelayedRestart(60L)
                    } else {
                        scheduleDelayedRestart(60L)
                    }
                }
            }

            HandsFreeMode.PROCESSING -> {
                // Processing in progress; recognizer will restart after TTS finishes speaking
            }

            else -> {
                if (_isHandsFreeActive.value && !isDestroyed) {
                    scheduleDelayedRestart(500L)
                } else {
                    _mode.value = HandsFreeMode.IDLE
                }
            }
        }
    }

    private fun isStopCommand(text: String): Boolean {
        val lower = text.lowercase().trim()
        return lower == "стоп" || lower == "стоп игра" || lower == "хватит" ||
                lower == "замолчи" || lower == "молчи" || lower == "тишина" ||
                lower == "отмена" || lower == "перестань" ||
                lower.startsWith("стоп ") || lower.startsWith("хватит ") ||
                lower.startsWith("замолчи ")
    }

    private fun startDialogInactivityTimer(timeoutMs: Long = 5000L) {
        cancelDialogInactivityTimer()
        dialogInactivityRunnable = Runnable {
            if (_mode.value == HandsFreeMode.AWAITING_COMMAND && _isHandsFreeActive.value && !isDestroyed) {
                _mode.value = HandsFreeMode.LISTENING_CONTINUOUS
                _lastDetectedPhrase.value = "Ожидание кодового слова: «${_wakePhrase.value}»"
                launchRecognizerInternal(prompt = null)
            }
        }
        mainHandler.postDelayed(dialogInactivityRunnable!!, timeoutMs)
    }

    private fun cancelDialogInactivityTimer() {
        dialogInactivityRunnable?.let { mainHandler.removeCallbacks(it) }
        dialogInactivityRunnable = null
    }

    private fun scheduleDelayedRestart(delayMs: Long) {
        cancelRestart()
        if (!_isHandsFreeActive.value || isDestroyed) return
        if (ttsHelper.isSpeaking.value) return

        restartRunnable = Runnable {
            if (_isHandsFreeActive.value && !isDestroyed && _mode.value != HandsFreeMode.TESTING_WAKE_WORD && !ttsHelper.isSpeaking.value) {
                launchRecognizerInternal(prompt = null)
            }
        }
        mainHandler.postDelayed(restartRunnable!!, delayMs)
    }

    private fun cancelRestart() {
        restartRunnable?.let { mainHandler.removeCallbacks(it) }
        restartRunnable = null
    }

    private fun silenceSystemStreams() {
        try {
            if (audioManager != null && !isMutedByController) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE)
                audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE)
                isMutedByController = true
            }
        } catch (e: Exception) {
            Log.w("HandsFreeController", "Could not adjust stream: ${e.message}")
        }
    }

    private fun restoreSystemStreams() {
        try {
            if (audioManager != null && isMutedByController) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE)
                audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE)
                isMutedByController = false
            }
        } catch (e: Exception) {
            Log.w("HandsFreeController", "Could not restore stream: ${e.message}")
        }
    }

    private fun createSpeechIntent(prompt: String?): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            if (prompt != null) {
                putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
            }
        }
    }

    fun containsWakePhrase(spokenText: String, targetPhrase: String): Boolean {
        val targets = targetPhrase.split(",", ";").map { it.trim().lowercase() }.filter { it.isNotBlank() }
        if (targets.isEmpty() || spokenText.isBlank()) return false
        return targets.any { checkSingleWakePhrase(spokenText, it) }
    }

    private fun checkSingleWakePhrase(spokenText: String, targetPhrase: String): Boolean {
        val target = targetPhrase.lowercase().trim()
        if (target.isBlank() || spokenText.isBlank()) return false
        val cleanSpoken = spokenText.lowercase().replace(Regex("[^a-zA-Zа-яА-Я0-9\\s]"), " ").replace(Regex("\\s+"), " ").trim()
        if (cleanSpoken.isBlank()) return false

        val cleanTarget = target.replace(Regex("[^a-zA-Zа-яА-Я0-9\\s]"), " ").replace(Regex("\\s+"), " ").trim()
        val targetWords = cleanTarget.split(" ").filter { it.isNotBlank() }
        if (targetWords.isEmpty()) return false

        // Exact matching with word boundaries strictly for the words entered by the user
        val wordBoundaryPattern = "\\b" + targetWords.joinToString("\\s+") { Regex.escape(it) } + "\\b"
        return Regex(wordBoundaryPattern, RegexOption.IGNORE_CASE).containsMatchIn(cleanSpoken)
    }

    private fun isAddressingAssistant(spokenText: String, targetWake: String): Boolean {
        val lower = spokenText.lowercase().trim()
        if (lower.isBlank()) return false

        // Spoken contains strictly the wake word(s) entered by the user
        if (containsWakePhrase(lower, targetWake)) return true

        // Stop commands
        if (isStopCommand(lower)) return true

        return false
    }

    private fun stripWakePhrase(text: String, targetPhrase: String): String {
        var clean = text
        val targets = targetPhrase.split(",", ";").map { it.trim() }.filter { it.isNotBlank() }
        for (target in targets) {
            if (target.isNotBlank()) {
                clean = clean.replace(Regex("\\b" + Regex.escape(target) + "\\b", RegexOption.IGNORE_CASE), "")
            }
        }
        return clean.trim().removePrefix(",").removePrefix(".").removePrefix("!").removePrefix("?").trim()
    }

    private fun stopSpeechRecognizerSafely() {
        cancelRestart()
        val recognizerToDestroy = speechRecognizer
        speechRecognizer = null
        val cleanupAction = {
            try {
                recognizerToDestroy?.setRecognitionListener(null)
                recognizerToDestroy?.stopListening()
                recognizerToDestroy?.cancel()
                recognizerToDestroy?.destroy()
            } catch (e: Exception) {}
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            cleanupAction()
        } else {
            mainHandler.post(Runnable { cleanupAction() })
        }
    }

    private fun cancelTestTimeout() {
        testTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        testTimeoutRunnable = null
    }

    private fun cancelAllTimeouts() {
        cancelRestart()
        cancelTestTimeout()
        cancelDialogInactivityTimer()
        mainHandler.removeCallbacksAndMessages(null)
    }

    fun destroy() {
        isDestroyed = true
        cancelAllTimeouts()
        restoreSystemStreams()
        stopSpeechRecognizerSafely()
    }
}
