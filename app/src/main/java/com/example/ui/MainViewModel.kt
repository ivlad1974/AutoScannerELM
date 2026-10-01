package com.example.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.AlertCondition
import com.example.data.AppProfileData
import com.example.data.ChatMessage
import com.example.data.ChatSender
import com.example.data.ConnectionState
import com.example.data.DashboardTileConfig
import com.example.data.DtcError
import com.example.data.ObdSensor
import com.example.data.ProfileManager
import com.example.data.SensorAlert
import com.example.data.SensorLogSession
import com.example.data.TileDisplayStyle
import com.example.data.VehicleInfo
import com.example.data.ai.GeminiAiService
import com.example.data.db.AppDatabase
import com.example.data.db.ChatMessageEntity
import com.example.data.db.ScanHistoryEntity
import com.example.data.db.SensorLogSessionEntity
import com.example.data.elm327.Elm327Manager
import com.example.data.logging.LogDataParser
import com.example.data.logging.ParsedLogSession
import com.example.data.logging.SensorFileLogger
import com.example.data.service.ObdForegroundService
import com.example.data.tts.SpeechInputHelper
import com.example.data.tts.TextToSpeechHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val elm327Manager = Elm327Manager(application)
    val ttsHelper = TextToSpeechHelper(application)
    val soundEffectsHelper = com.example.data.sound.SoundEffectsHelper(application)
    val speechHelper = SpeechInputHelper(application)
    val handsFreeController = com.example.data.tts.HandsFreeVoiceController(application, ttsHelper)
    val logger = SensorFileLogger(application)
    private val aiService = GeminiAiService(application)
    private val liveInternetService = com.example.data.ai.LiveInternetService(application)

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "autoscan_database.db"
    ).build()
    private val dao = db.appDao()

    val connectionState = elm327Manager.connectionState
    val vehicleInfo = elm327Manager.vehicleInfo
    val sensors = elm327Manager.sensors
    val dtcErrors = elm327Manager.dtcErrors
    val ecuBlocks = elm327Manager.ecuBlocks
    val supportedPidsResult = elm327Manager.supportedPidsResult
    val isScanning = elm327Manager.isScanning
    val scanningProgressText = elm327Manager.scanningProgressText
    val scanningProgressFraction = elm327Manager.scanningProgressFraction
    val activeManufacturerProfile = elm327Manager.activeManufacturerProfile
    val availableManufacturerProfiles = com.example.data.elm327.ManufacturerSensorRepository.getAllProfiles()

    val isVoiceEnabled = ttsHelper.isVoiceEnabled
    val isSpeaking = ttsHelper.isSpeaking
    val isListening = speechHelper.isListening

    val handsFreeMode = handsFreeController.mode
    val isHandsFreeActive = handsFreeController.isHandsFreeActive
    val handsFreeWorkMode = handsFreeController.workMode
    val wakePhrase = handsFreeController.wakePhrase
    val readyResponse = handsFreeController.readyResponse
    val lastDetectedPhrase = handsFreeController.lastDetectedPhrase
    val handsFreeAudioLevel = handsFreeController.audioLevel
    val micAudioLevel: StateFlow<Float> = kotlinx.coroutines.flow.combine(
        handsFreeController.audioLevel,
        speechHelper.audioLevel
    ) { hf, sp -> maxOf(hf, sp) }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), 0f)

    fun setHandsFreeWorkMode(workMode: com.example.data.tts.HandsFreeVoiceController.AssistantWorkMode) {
        handsFreeController.setWorkMode(workMode)
        persistProfile()
    }

    fun updateHandsFreeSettings(wakePhrase: String, readyResponse: String, isEnabled: Boolean) {
        handsFreeController.updateSettings(wakePhrase, readyResponse, isEnabled)
        persistProfile()
    }

    fun toggleHandsFree() {
        val newState = !isHandsFreeActive.value
        handsFreeController.updateSettings(wakePhrase.value, readyResponse.value, newState)
        persistProfile()
    }

    fun testWakeWord(
        wakePhraseToTest: String,
        readyResponseToTest: String,
        onResult: (matched: Boolean, recognizedText: String) -> Unit
    ) {
        handsFreeController.testWakeWordRecognition(wakePhraseToTest, readyResponseToTest, onResult)
    }

    fun testResponseVoice(text: String): String {
        return handsFreeController.testResponseVoice(text)
    }

    private val _isLogging = MutableStateFlow(false)
    val isLogging: StateFlow<Boolean> = _isLogging.asStateFlow()

    private val _selectedLogSession = MutableStateFlow<ParsedLogSession?>(null)
    val selectedLogSession: StateFlow<ParsedLogSession?> = _selectedLogSession.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _aiAlert = MutableStateFlow<String?>(null)
    val aiAlert: StateFlow<String?> = _aiAlert.asStateFlow()

    // Real-time AI Sensor Monitoring State
    private val _aiMonitoringState = MutableStateFlow(AiMonitoringState())
    val aiMonitoringState: StateFlow<AiMonitoringState> = _aiMonitoringState.asStateFlow()
    private var monitoringJob: kotlinx.coroutines.Job? = null
    private var activeAiJob: kotlinx.coroutines.Job? = null

    private var activeMonitoringFileWriter: java.io.FileWriter? = null
    private var activeMonitoringLogFile: File? = null

    private val _lastMonitoringStats = MutableStateFlow<com.example.data.ai.MonitoringStats?>(null)
    val lastMonitoringStats: StateFlow<com.example.data.ai.MonitoringStats?> = _lastMonitoringStats.asStateFlow()

    private val _lastMonitoringFile = MutableStateFlow<File?>(null)
    val lastMonitoringFile: StateFlow<File?> = _lastMonitoringFile.asStateFlow()

    private val _navigationEvent = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 5)
    val navigationEvent: kotlinx.coroutines.flow.SharedFlow<String> = _navigationEvent

    fun navigateTo(route: String) {
        _navigationEvent.tryEmit(route)
    }

    fun dismissLastMonitoringCard() {
        _lastMonitoringStats.value = null
    }

    private val _statusMessage = MutableStateFlow<String>("Готов к подключению ELM327 v1.5")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _aiReferencedLogs = MutableStateFlow<List<java.io.File>>(emptyList())
    val aiReferencedLogs: StateFlow<List<java.io.File>> = _aiReferencedLogs.asStateFlow()

    // Configurable connection & ignition check intervals (seconds)
    val disconnectedReconnectIntervalSec: StateFlow<Int> = elm327Manager.disconnectedReconnectIntervalSec
    val ignitionOffPollIntervalSec: StateFlow<Int> = elm327Manager.ignitionOffPollIntervalSec
    val ignitionOnCheckIntervalSec: StateFlow<Int> = elm327Manager.ignitionOnCheckIntervalSec

    fun setDisconnectedReconnectIntervalSec(seconds: Int) {
        elm327Manager.setDisconnectedReconnectIntervalSec(seconds)
    }

    fun setIgnitionOffPollIntervalSec(seconds: Int) {
        elm327Manager.setIgnitionOffPollIntervalSec(seconds)
    }

    fun setIgnitionOnCheckIntervalSec(seconds: Int) {
        elm327Manager.setIgnitionOnCheckIntervalSec(seconds)
    }

    private val _appTheme = MutableStateFlow<com.example.ui.theme.AppThemeMode>(com.example.ui.theme.AppThemeMode.DARK_SPORT)
    val appTheme: StateFlow<com.example.ui.theme.AppThemeMode> = _appTheme.asStateFlow()

    private val _sensorAlerts = MutableStateFlow<List<SensorAlert>>(emptyList())
    val sensorAlerts: StateFlow<List<SensorAlert>> = _sensorAlerts.asStateFlow()

    private val defaultDashboardTiles = listOf(
        DashboardTileConfig(0, "010D", TileDisplayStyle.GRAPH_WAVE),     // Скорость
        DashboardTileConfig(1, "010C", TileDisplayStyle.GRAPH_WAVE),     // Обороты RPM
        DashboardTileConfig(2, "0105", TileDisplayStyle.GRAPH_WAVE),     // Температура ОЖ
        DashboardTileConfig(3, "0142", TileDisplayStyle.GRAPH_WAVE)      // Напряжение АКБ
    )

    private val _dashboardTileConfigs = MutableStateFlow<List<DashboardTileConfig>>(defaultDashboardTiles)
    val dashboardTileConfigs: StateFlow<List<DashboardTileConfig>> = _dashboardTileConfigs.asStateFlow()

    fun updateDashboardTileConfig(slotIndex: Int, newPid: String, newStyle: TileDisplayStyle, newGraphTimeRangeMinutes: Int = 5) {
        val current = _dashboardTileConfigs.value.toMutableList()
        val existingIdx = current.indexOfFirst { it.slotIndex == slotIndex }
        val newConfig = DashboardTileConfig(slotIndex, newPid, newStyle, newGraphTimeRangeMinutes)
        if (existingIdx >= 0) {
            current[existingIdx] = newConfig
        } else {
            current.add(newConfig)
        }
        _dashboardTileConfigs.value = current.sortedBy { it.slotIndex }
        persistProfile()
    }

    fun setAppTheme(mode: com.example.ui.theme.AppThemeMode) {
        _appTheme.value = mode
        persistProfile()
    }

    fun setSensorGraphPeriod(pid: String, periodSeconds: Int) {
        elm327Manager.setSensorGraphPeriod(pid, periodSeconds)
        persistProfile()
    }

    fun persistProfile() {
        val data = currentProfileData()
        ProfileManager.saveToPrefs(getApplication(), data)
        ProfileManager.autoExportProfileToFile(getApplication(), data)
    }

    private fun currentProfileData(): AppProfileData {
        val periods = sensors.value.associate { it.pid to it.graphPeriodSec }
        val cutoffWeekMs = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000L
        val recentChat = _chatMessages.value.filter { it.timestamp >= cutoffWeekMs }
        return AppProfileData(
            isVoiceEnabled = isVoiceEnabled.value,
            appTheme = _appTheme.value.name,
            savedDeviceAddress = getSavedDeviceAddress(),
            savedDeviceName = getSavedDeviceName(),
            alerts = _sensorAlerts.value,
            chatMessages = recentChat,
            vehicleInfo = vehicleInfo.value,
            tileConfigs = _dashboardTileConfigs.value,
            isHandsFreeEnabled = isHandsFreeActive.value,
            wakePhrase = wakePhrase.value,
            readyResponse = readyResponse.value,
            sensorGraphPeriods = periods
        )
    }

    fun addSensorAlert(alert: SensorAlert) {
        _sensorAlerts.value = _sensorAlerts.value + alert
        persistProfile()
    }

    fun updateSensorAlert(updatedAlert: SensorAlert) {
        _sensorAlerts.value = _sensorAlerts.value.map {
            if (it.id == updatedAlert.id) updatedAlert else it
        }
        persistProfile()
    }

    fun removeSensorAlert(alertId: String) {
        _sensorAlerts.value = _sensorAlerts.value.filter { it.id != alertId }
        persistProfile()
    }

    fun toggleSensorAlert(alertId: String, enabled: Boolean) {
        _sensorAlerts.value = _sensorAlerts.value.map {
            if (it.id == alertId) it.copy(isEnabled = enabled) else it
        }
        persistProfile()
    }

    val scanHistory = dao.getAllScanHistory()
    val logSessions = dao.getAllLogSessions()

    init {
        // Start Foreground Service to keep running in background
        try {
            ObdForegroundService.startService(application)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Restore Profile (alerts, settings, dialogs)
        val restoredProfile = ProfileManager.loadFromPrefs(application)
        if (restoredProfile != null) {
            _sensorAlerts.value = restoredProfile.alerts
            if (restoredProfile.tileConfigs.isNotEmpty()) {
                _dashboardTileConfigs.value = restoredProfile.tileConfigs
            }
            ttsHelper.setVoiceEnabled(restoredProfile.isVoiceEnabled)
            try {
                _appTheme.value = com.example.ui.theme.AppThemeMode.valueOf(restoredProfile.appTheme)
            } catch (e: Exception) {
                _appTheme.value = com.example.ui.theme.AppThemeMode.DARK_SPORT
            }
            if (restoredProfile.chatMessages.isNotEmpty()) {
                _chatMessages.value = restoredProfile.chatMessages
            }
            if (restoredProfile.sensorGraphPeriods.isNotEmpty()) {
                restoredProfile.sensorGraphPeriods.forEach { (pid, sec) ->
                    elm327Manager.setSensorGraphPeriod(pid, sec)
                }
            }
        }

        // Initialize historical sample trip logs in Room DB and filesystem for yesterday's logs
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sampleFiles = logger.ensureSampleTripLogsExist()
                sampleFiles.forEach { file ->
                    val cal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
                    val ts = when {
                        file.name.contains("_1552") -> {
                            cal.set(java.util.Calendar.HOUR_OF_DAY, 15)
                            cal.set(java.util.Calendar.MINUTE, 52)
                            cal.timeInMillis
                        }
                        file.name.contains("_1605") -> {
                            cal.set(java.util.Calendar.HOUR_OF_DAY, 16)
                            cal.set(java.util.Calendar.MINUTE, 5)
                            cal.timeInMillis
                        }
                        file.name.contains("_1618") -> {
                            cal.set(java.util.Calendar.HOUR_OF_DAY, 16)
                            cal.set(java.util.Calendar.MINUTE, 18)
                            cal.timeInMillis
                        }
                        else -> file.lastModified()
                    }
                    val linesCount = try { file.readLines().size.coerceAtLeast(1) - 1 } catch (e: Exception) { 600 }
                    dao.insertLogSession(
                        SensorLogSessionEntity(
                            fileName = file.name,
                            timestamp = ts,
                            recordCount = linesCount,
                            durationSeconds = linesCount.toLong(),
                            filePath = file.absolutePath
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Stop TTS immediately whenever speech recognition starts
        speechHelper.setOnStartListeningCallback {
            ttsHelper.stop()
        }

        // Setup hands-free controller dispatcher
        handsFreeController.setCommandDispatcher { spokenCmd ->
            askAiCustomQuery(spokenCmd)
        }

        if (restoredProfile != null) {
            handsFreeController.updateSettings(
                wakePhrase = restoredProfile.wakePhrase,
                readyResponse = restoredProfile.readyResponse,
                isEnabled = restoredProfile.isHandsFreeEnabled
            )
        }

        // Collect DB chat history & maintain 7-day retention
        viewModelScope.launch {
            val cutoffWeekMs = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000L
            try {
                dao.deleteChatMessagesOlderThan(cutoffWeekMs)
            } catch (e: Exception) {}

            dao.getAllChatMessages().collectLatest { entities ->
                val recentEntities = entities.filter { it.timestamp >= (System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000L) }
                if (recentEntities.isEmpty() && _chatMessages.value.isEmpty()) {
                    val defaultGreeting = ChatMessage(
                        sender = ChatSender.AI,
                        text = "Привет! Я твоя универсальная нейросеть и умный бортовой ассистент AutoScan. Отвечу абсолютно на любые вопросы — от науки, кода, кулинарии и жизненных тем до автодиагностики, телеметрии датчиков и погоды. Спрашивай обо всем!"
                    )
                    _chatMessages.value = listOf(defaultGreeting)
                } else if (recentEntities.isNotEmpty()) {
                    _chatMessages.value = recentEntities.map {
                        ChatMessage(
                            id = it.id,
                            sender = if (it.sender == "USER") ChatSender.USER else ChatSender.AI,
                            text = it.text,
                            timestamp = it.timestamp
                        )
                    }
                }
            }
        }

        // Collect Elm327 events
        viewModelScope.launch {
            elm327Manager.eventFlow.collectLatest { event ->
                when {
                    event == "CONNECTED_BLUETOOTH_READY" -> {
                        _statusMessage.value = "Подключено к ELM327"
                        soundEffectsHelper.playConnectSound()
                    }
                    event == "CONNECTED_BLUETOOTH_IGNITION_OFF" -> {
                        _statusMessage.value = "Подключено к ELM327"
                        soundEffectsHelper.playConnectSound()
                    }
                    event == "IGNITION_TURNED_OFF" -> {
                        persistProfile() // Auto-export profile and save settings on every vehicle turn-off
                    }
                    event == "IGNITION_TURNED_ON" -> {
                    }
                    event == "NO_CONNECTION_FOR_SCAN" -> {
                        _statusMessage.value = "Ошибка: адаптер ELM327 не подключен"
                        ttsHelper.speak("Адаптер ELM327 не подключен. Сканирование невозможно.")
                    }
                    event == "NO_CONNECTION_FOR_CLEAR" -> {
                        _statusMessage.value = "Ошибка: адаптер ELM327 не подключен"
                        ttsHelper.speak("Адаптер ELM327 не подключен. Сброс ошибок невозможен.")
                    }
                    event == "SCAN_STARTED_INITIAL" -> {
                        _statusMessage.value = "Фоновое сканирование всех блоков на ошибки..."
                    }
                    event == "SCAN_STARTED_PERIODIC" -> {
                        _statusMessage.value = "Периодическая фоновая проверка блоков на ошибки..."
                    }
                    event == "SCAN_STARTED" -> {
                        _statusMessage.value = "Запущено полное сканирование блоков ECU..."
                        ttsHelper.speak("Запускаю полное сканирование блоков управления автомобиля на ошибки.")
                    }
                    event == "INITIAL_SCAN_COMPLETED" -> {
                        val count = dtcErrors.value.size
                        val codesList = if (count > 0) dtcErrors.value.joinToString(", ") { it.code } else ""
                        _statusMessage.value = if (count > 0) {
                            "Фоновое сканирование: обнаружено $count ошибок ($codesList)"
                        } else {
                            "Фоновое сканирование: ошибок не обнаружено"
                        }
                        // Speak voice announcement ONLY if errors were found in background scan
                        if (count > 0) {
                            ttsHelper.speak("Внимание! При фоновом сканировании обнаружено $count ошибок: $codesList.")
                        }

                        // Save scan result to Room history
                        dao.insertScanHistory(
                            ScanHistoryEntity(
                                timestamp = System.currentTimeMillis(),
                                vin = vehicleInfo.value.vin,
                                vehicleModel = "${vehicleInfo.value.make} ${vehicleInfo.value.model}",
                                totalDtcFound = count,
                                dtcCodesJson = dtcErrors.value.joinToString(",") { it.code }
                            )
                        )
                    }
                    event == "SCAN_COMPLETED" -> {
                        val count = dtcErrors.value.size
                        val codesList = if (count > 0) dtcErrors.value.joinToString(", ") { it.code } else ""
                        _statusMessage.value = if (count > 0) {
                            "Сканирование завершено. Найдено ошибок: $count ($codesList)"
                        } else {
                            "Сканирование завершено. Ошибок не обнаружено"
                        }
                        val speakText = if (count > 0) {
                            "Внимание! При сканировании модулей обнаружено $count ошибок: $codesList. Список выведен на экран."
                        } else {
                            "Сканирование всех модулей завершено. Активных ошибок не обнаружено. Автомобиль исправен."
                        }
                        ttsHelper.speak(speakText)

                        // Save scan result to Room history
                        dao.insertScanHistory(
                            ScanHistoryEntity(
                                timestamp = System.currentTimeMillis(),
                                vin = vehicleInfo.value.vin,
                                vehicleModel = "${vehicleInfo.value.make} ${vehicleInfo.value.model}",
                                totalDtcFound = count,
                                dtcCodesJson = dtcErrors.value.joinToString(",") { it.code }
                            )
                        )
                    }
                    event == "PERIODIC_SCAN_COMPLETED" -> {
                        val count = dtcErrors.value.size
                        val codesList = if (count > 0) dtcErrors.value.joinToString(", ") { it.code } else ""
                        _statusMessage.value = if (count > 0) {
                            "Периодическая проверка: обнаружено $count ошибок ($codesList)"
                        } else {
                            "Периодическая проверка: ошибок не обнаружено"
                        }
                        // Speak voice announcement ONLY if errors were found
                        if (count > 0) {
                            ttsHelper.speak("Периодическая диагностика: в блоках управления обнаружено $count ошибок: $codesList.")
                        }
                    }
                    event.startsWith("REALTIME_MIL_TRIGGERED:") -> {
                        val codeInfo = event.substringAfter("REALTIME_MIL_TRIGGERED:").trim()
                        _statusMessage.value = "ВНИМАНИЕ: Загорелся Check Engine! Ошибка: $codeInfo"
                        ttsHelper.speak("Внимание! На приборной панели загорелся индикатор Чек Энджин. В реальном времени зафиксирована ошибка: $codeInfo.", overrideToggle = true)
                    }
                    event == "CLEAR_STARTED" -> {
                        _statusMessage.value = "Выполняется сброс диагностических ошибок (Mode 04)..."
                        ttsHelper.speak("Выполняю сброс диагностических кодов и очистку сохранения памяти ошибок.")
                    }
                    event == "CLEAR_COMPLETED" -> {
                        _statusMessage.value = "Сброс ошибок успешно выполнен"
                        ttsHelper.speak("Сброс ошибок успешно выполнен. Чек энджин погашен. Проведите контрольную поездку.")
                    }
                    event.startsWith("CONNECTION_LOST:") -> {
                        val reason = event.substringAfter("CONNECTION_LOST:").trim()
                        _statusMessage.value = "Потеряна связь: $reason"
                        soundEffectsHelper.playDisconnectSound()
                    }
                    else -> {
                        _statusMessage.value = event
                    }
                }
            }
        }

        // Sensor real-time monitoring & logging loop
        viewModelScope.launch {
            sensors.collectLatest { sensorList ->
                // 1. Поездочный лог: всегда пишутся 4 датчика, выбранные для главного экрана
                val dashboardPids: List<String> = _dashboardTileConfigs.value.map { it.sensorPid }
                val dashboardSensors: List<ObdSensor> = dashboardPids.mapNotNull { pid ->
                    sensorList.find { it.pid == pid }
                }.ifEmpty {
                    sensorList.take(4)
                }
                val closedTripSession = logger.logTripContinuously(dashboardSensors)
                if (closedTripSession != null) {
                    dao.insertLogSession(
                        SensorLogSessionEntity(
                            fileName = closedTripSession.fileName,
                            timestamp = closedTripSession.timestamp,
                            recordCount = closedTripSession.recordCount,
                            durationSeconds = closedTripSession.durationSeconds,
                            filePath = java.io.File(getApplication<android.app.Application>().getExternalFilesDir(null), "SensorLogs/${closedTripSession.fileName}").absolutePath
                        )
                    )
                }

                // 2. Отдельный лог по голосовой команде или ручной кнопке: пишутся все датчики, выбранные в настройках
                if (_isLogging.value) {
                    val settingsSensors = sensorList.filter { it.isSelected }.ifEmpty { sensorList }
                    logger.logVoiceSensors(settingsSensors)
                }

                // Sensor real-time monitoring & user custom alerts check
                val now = System.currentTimeMillis()
                var triggeredMsg: String? = null

                for (alert in _sensorAlerts.value) {
                    if (!alert.isEnabled) continue
                    val sensor = sensorList.find { it.pid == alert.sensorPid } ?: continue

                    val isTriggered = when (alert.condition) {
                        AlertCondition.GREATER_THAN -> sensor.value > alert.thresholdValue
                        AlertCondition.LESS_THAN -> sensor.value < alert.thresholdValue
                    }

                    val repeatIntervalMs = (alert.repeatIntervalSeconds.takeIf { it > 0 } ?: 30) * 1000L

                    if (isTriggered && (now - alert.lastTriggeredTime >= repeatIntervalMs)) {
                        alert.lastTriggeredTime = now

                        val condLabel = if (alert.condition == AlertCondition.GREATER_THAN) "превысила порог" else "упала ниже порога"
                        val formattedSensorVal = com.example.data.formatSensorValue(sensor.value, alert.sensorPid, alert.unit)
                        val formattedThresholdVal = com.example.data.formatSensorValue(alert.thresholdValue, alert.sensorPid, alert.unit)

                        triggeredMsg = "⚠️ ВНИМАНИЕ: ${alert.sensorName} ($formattedSensorVal ${alert.unit}) — $condLabel $formattedThresholdVal ${alert.unit}!"

                        // Sound Beep
                        try {
                            val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 85)
                            toneGen.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 500)
                        } catch (e: Exception) {
                            // Ignore tone errors
                        }

                        // Voice warning
                        val speechVal = com.example.data.formatSensorValueForSpeech(sensor.value, alert.sensorPid, alert.unit)
                        val voiceText = "Внимание! ${alert.sensorName} составляет $speechVal ${alert.unit}."
                        ttsHelper.speak(voiceText, overrideToggle = true)
                        break
                    }
                }

                _aiAlert.value = triggeredMsg
            }
        }
    }

    fun isBluetoothEnabled(): Boolean = elm327Manager.isBluetoothEnabled()

    fun enableBluetooth(): Boolean = elm327Manager.enableBluetooth()

    fun getPairedDevices(): List<android.bluetooth.BluetoothDevice> = elm327Manager.getPairedDevices()

    fun getSavedDeviceName(): String? = elm327Manager.getSavedDeviceName()

    fun getSavedDeviceAddress(): String? = elm327Manager.getSavedDeviceAddress()

    fun clearSavedDevice() = elm327Manager.clearSavedDevice()

    fun autoConnectIfSavedDeviceExists() {
        if (connectionState.value == ConnectionState.CONNECTED_BLUETOOTH || 
            connectionState.value == ConnectionState.CONNECTING) {
            return
        }
        if (!isBluetoothEnabled()) {
            enableBluetooth()
        }
        val savedDevice = elm327Manager.getSavedBluetoothDevice()
        if (savedDevice != null) {
            _statusMessage.value = "Автоподключение к запомненному ELM327 (${savedDevice.name ?: savedDevice.address})..."
            connectBluetoothDevice(savedDevice)
        }
    }

    fun connectAutoOrPick(onNeedPicker: () -> Unit) {
        if (!isBluetoothEnabled()) {
            enableBluetooth()
        }
        val savedDevice = elm327Manager.getSavedBluetoothDevice()
        if (savedDevice != null) {
            _statusMessage.value = "Подключение к запомненному ELM327 (${savedDevice.name ?: savedDevice.address})..."
            connectBluetoothDevice(savedDevice)
        } else {
            onNeedPicker()
        }
    }

    fun connectBluetoothDevice(device: android.bluetooth.BluetoothDevice) {
        elm327Manager.connectBluetoothDevice(device)
    }

    fun disconnect() {
        elm327Manager.disconnect()
    }

    fun toggleVoiceFeedback() {
        val newState = !isVoiceEnabled.value
        ttsHelper.setVoiceEnabled(newState)
    }

    fun performFullEcuScan() {
        elm327Manager.performFullEcuScan()
    }

    fun stopEcuScan() {
        elm327Manager.stopEcuScan()
    }

    fun clearDtcErrors() {
        elm327Manager.clearDtcErrors()
    }

    fun toggleSensorSelection(pid: String) {
        elm327Manager.toggleSensorSelection(pid)
    }

    fun setSensorSelected(pid: String, isSelected: Boolean) {
        elm327Manager.setSensorSelected(pid, isSelected)
    }

    fun selectAllSensors() {
        elm327Manager.selectAllSensors()
    }

    fun deselectAllSensors() {
        elm327Manager.deselectAllSensors()
    }

    fun selectDefaultSensors() {
        elm327Manager.selectDefaultSensors()
    }

    fun runPidDiscovery() {
        viewModelScope.launch {
            elm327Manager.discoverSupportedPids()
        }
    }

    fun loadManufacturerProfile(profileId: String) {
        elm327Manager.loadManufacturerProfile(profileId)
    }

    fun unloadManufacturerProfile() {
        elm327Manager.unloadManufacturerProfile()
    }

    fun addCustomSensor(sensor: com.example.data.ObdSensor) {
        elm327Manager.addCustomSensor(sensor)
    }

    fun removeCustomSensor(pid: String) {
        elm327Manager.removeCustomSensor(pid)
    }

    fun importSensorsFromCsv(csvContent: String): Int {
        return elm327Manager.importSensorsFromCsv(csvContent)
    }

    fun exportSensorsToCsv(): String {
        return elm327Manager.exportSensorsToCsv()
    }

    fun startVoiceLogging(): SensorLogSession? {
        val selectedSensors = sensors.value.filter { it.isSelected }.ifEmpty { sensors.value }
        val session = logger.startVoiceLogging(selectedSensors)
        if (session != null) {
            _isLogging.value = true
        }
        return session
    }

    fun stopVoiceLogging(): SensorLogSession? {
        val session = logger.stopVoiceLogging()
        _isLogging.value = false
        if (session != null) {
            viewModelScope.launch {
                dao.insertLogSession(
                    SensorLogSessionEntity(
                        fileName = session.fileName,
                        timestamp = session.timestamp,
                        recordCount = session.recordCount,
                        durationSeconds = session.durationSeconds,
                        filePath = session.fileName
                    )
                )
            }
        }
        return session
    }

    fun startSensorLogging() {
        val session = startVoiceLogging()
        if (session != null) {
            val count = sensors.value.count { it.isSelected }.takeIf { it > 0 } ?: sensors.value.size
            ttsHelper.speak("Запущена запись $count выбранных в настройках параметров в отдельный ЛОГ-файл.")
        }
    }

    fun stopSensorLogging() {
        val session = stopVoiceLogging()
        if (session != null) {
            ttsHelper.speak("Запись ЛОГ-файла остановлена. Сохранено записей: ${session.recordCount}.")
        }
    }

    fun shareLogFile(file: File) {
        logger.shareLogFile(file)
    }

    fun deleteLogFile(file: File): Boolean {
        val deleted = logger.deleteLogFile(file)
        if (_selectedLogSession.value?.fileName == file.name) {
            _selectedLogSession.value = null
        }
        return deleted
    }

    fun getLogFiles(): List<File> = logger.getLogFiles()
    fun getLogDurationText(file: File): String = logger.getLogDurationText(file)

    fun askAiAboutSingleDtc(dtcCode: String, description: String) {
        activeAiJob?.cancel()
        activeAiJob = viewModelScope.launch {
            _isAiLoading.value = true
            val promptText = "Расскажи подробнее про ошибку $dtcCode ($description). Что мне делать и куда смотреть?"
            addUserChatMessage(promptText)

            val reply = aiService.consultOnSingleDtc(dtcCode, description, vehicleInfo.value, chatMessages.value)
            addAiChatMessage(reply)
            _isAiLoading.value = false
            ttsHelper.speak(reply)
        }
    }

    fun askAiAboutAllDtcs() {
        activeAiJob?.cancel()
        activeAiJob = viewModelScope.launch {
            _isAiLoading.value = true
            val promptText = "Дай комплексную консультацию диагноста по всем найденным ошибкам автомобиля."
            addUserChatMessage(promptText)

            val reply = aiService.consultOnAllDtcs(dtcErrors.value, vehicleInfo.value, chatMessages.value)
            addAiChatMessage(reply)
            _isAiLoading.value = false
            ttsHelper.speak(reply)
        }
    }

    fun askAiCustomQuery(query: String) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return
        activeAiJob?.cancel()
        activeAiJob = viewModelScope.launch {
            try {
                if (handleAiCommand(cleanQuery)) return@launch

                _isAiLoading.value = true
                addUserChatMessage(cleanQuery)

                val reply = aiService.consultOnLiveSensors(
                    sensors.value,
                    dtcErrors.value,
                    cleanQuery,
                    vehicleInfo.value,
                    chatMessages.value
                )

                val cleanReply = reply.ifBlank {
                    "По вашему вопросу точной информации не найдено. Попробуйте уточнить вопрос."
                }

                addAiChatMessage(cleanReply)
                ttsHelper.speak(cleanReply)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error in askAiCustomQuery: ${e.message}", e)
                val fallbackMsg = "Произошла ошибка при обработке запроса. Пожалуйста, повторите вопрос."
                addAiChatMessage(fallbackMsg)
                ttsHelper.speak(fallbackMsg)
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun startAiSensorMonitoring(
        sensorPid: String,
        sensorName: String,
        unit: String,
        durationSeconds: Int
    ) {
        monitoringJob?.cancel()
        val clampedDuration = durationSeconds.coerceIn(5, 180)
        val initialVal = sensors.value.find { it.pid == sensorPid }?.value ?: 0.0
        val initialPoints = mutableListOf<Double>()
        if (initialVal != 0.0) initialPoints.add(initialVal)

        // Initialize CSV file for this monitoring session
        val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
        val cleanName = sensorName.replace(Regex("[^a-zA-Zа-яА-Я0-9]"), "_")
        val logsDir = File(getApplication<android.app.Application>().getExternalFilesDir(null), "SensorLogs").apply { if (!exists()) mkdirs() }
        val monitorLogFile = File(logsDir, "AutoScan_Monitor_${cleanName}_${timeStamp}.csv")
        activeMonitoringLogFile = monitorLogFile
        try {
            activeMonitoringFileWriter?.close()
            activeMonitoringFileWriter = java.io.FileWriter(monitorLogFile, true).apply {
                write("Timestamp,$sensorName,Скорость,Обороты двигателя,Напряжение\n")
                flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        _aiMonitoringState.value = AiMonitoringState(
            isActive = true,
            sensorPid = sensorPid,
            sensorName = sensorName,
            unit = unit,
            targetDurationSeconds = clampedDuration,
            elapsedSeconds = 0,
            currentLiveValue = initialVal,
            points = initialPoints.toList()
        )

        val startAnnounce = "Начинаю замер и запись лога датчика «$sensorName» на $clampedDuration сек..."
        _statusMessage.value = startAnnounce
        ttsHelper.speak("Запускаю замер и запись лога датчика $sensorName на $clampedDuration секунд...")

        monitoringJob = viewModelScope.launch {
            var elapsedMs = 0L
            val sampleIntervalMs = 350L

            while (elapsedMs < clampedDuration * 1000L) {
                kotlinx.coroutines.delay(sampleIntervalMs)
                elapsedMs += sampleIntervalMs

                val currentSensor = sensors.value.find { it.pid == sensorPid }
                val currentVal = currentSensor?.value ?: 0.0
                initialPoints.add(currentVal)

                val elapsedSec = (elapsedMs / 1000L).toInt()
                _aiMonitoringState.value = _aiMonitoringState.value.copy(
                    elapsedSeconds = elapsedSec,
                    currentLiveValue = currentVal,
                    points = initialPoints.toList()
                )

                // Write telemetry sample to CSV log
                try {
                    val timeFormatted = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
                    val spd = sensors.value.find { it.pid == "010D" }?.value ?: 0.0
                    val rpm = sensors.value.find { it.pid == "010C" }?.value ?: 0.0
                    val volt = sensors.value.find { it.pid == "0142" }?.value ?: 14.1
                    activeMonitoringFileWriter?.write("$timeFormatted,$currentVal,$spd,$rpm,$volt\n")
                    activeMonitoringFileWriter?.flush()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Monitoring completed
            executeFinishAiSensorMonitoring(sensorPid, sensorName, unit, clampedDuration, initialPoints)
        }
    }

    fun finishAiSensorMonitoring() {
        val state = _aiMonitoringState.value
        if (!state.isActive) return
        monitoringJob?.cancel()
        monitoringJob = null
        viewModelScope.launch {
            executeFinishAiSensorMonitoring(
                state.sensorPid,
                state.sensorName,
                state.unit,
                state.elapsedSeconds.coerceAtLeast(1),
                state.points
            )
        }
    }

    fun cancelAiSensorMonitoring() {
        val state = _aiMonitoringState.value
        monitoringJob?.cancel()
        monitoringJob = null
        _aiMonitoringState.value = AiMonitoringState(isActive = false)
        try {
            activeMonitoringFileWriter?.flush()
            activeMonitoringFileWriter?.close()
        } catch (e: Exception) {}
        activeMonitoringFileWriter = null

        if (state.isActive && state.points.size >= 5) {
            viewModelScope.launch {
                executeFinishAiSensorMonitoring(
                    state.sensorPid,
                    state.sensorName,
                    state.unit,
                    state.elapsedSeconds.coerceAtLeast(1),
                    state.points
                )
            }
        } else {
            val msg = "Мониторинг датчика остановлен."
            _statusMessage.value = msg
            ttsHelper.speak(msg)
        }
    }

    private suspend fun executeFinishAiSensorMonitoring(
        sensorPid: String,
        sensorName: String,
        unit: String,
        durationSec: Int,
        points: List<Double>
    ) {
        _aiMonitoringState.value = AiMonitoringState(isActive = false)
        try {
            activeMonitoringFileWriter?.flush()
            activeMonitoringFileWriter?.close()
        } catch (e: Exception) {}
        activeMonitoringFileWriter = null

        val cleanPoints = if (points.isEmpty()) listOf(0.0) else points
        val minVal = cleanPoints.minOrNull() ?: 0.0
        val maxVal = cleanPoints.maxOrNull() ?: 0.0
        val avgVal = cleanPoints.average()
        val startVal = cleanPoints.firstOrNull() ?: 0.0
        val endVal = cleanPoints.lastOrNull() ?: 0.0
        val delta = endVal - startVal

        // Standard deviation & fluctuation
        val variance = cleanPoints.map { (it - avgVal) * (it - avgVal) }.average()
        val stdDev = kotlin.math.sqrt(variance)
        val fluctuation = if (avgVal != 0.0) (stdDev / kotlin.math.abs(avgVal)) * 100.0 else 0.0

        val stats = com.example.data.ai.MonitoringStats(
            sensorName = sensorName,
            unit = unit,
            durationSeconds = durationSec,
            sampleCount = cleanPoints.size,
            min = minVal,
            max = maxVal,
            avg = avgVal,
            startVal = startVal,
            endVal = endVal,
            delta = delta,
            stdDev = stdDev,
            fluctuationPercent = fluctuation
        )

        val monitorLogFile = activeMonitoringLogFile
        if (monitorLogFile != null && monitorLogFile.exists()) {
            _lastMonitoringFile.value = monitorLogFile
            viewModelScope.launch {
                dao.insertLogSession(
                    SensorLogSessionEntity(
                        fileName = monitorLogFile.name,
                        timestamp = System.currentTimeMillis(),
                        recordCount = cleanPoints.size,
                        durationSeconds = durationSec.toLong(),
                        filePath = monitorLogFile.absolutePath
                    )
                )
            }
            val parsed = LogDataParser.parseCsvFile(monitorLogFile)
            if (parsed != null) {
                _selectedLogSession.value = parsed
            }
        }

        // Store last monitoring stats in AI service and local StateFlow
        aiService.lastMonitoringStats = stats
        aiService.lastMonitoringPoints = cleanPoints
        _lastMonitoringStats.value = stats

        val avgFormatted = String.format(java.util.Locale.US, "%.1f", avgVal)
        val minFormatted = String.format(java.util.Locale.US, "%.1f", minVal)
        val maxFormatted = String.format(java.util.Locale.US, "%.1f", maxVal)
        val flucFormatted = String.format(java.util.Locale.US, "%.1f", fluctuation)

        val logFileName = monitorLogFile?.name ?: "Log.csv"
        val finishMessage = "⏱️ Замер датчика **«$sensorName»** завершен ($durationSec сек)!\n" +
                "• Записан лог: **$logFileName** (${cleanPoints.size} точек)\n" +
                "• Мин: **$minFormatted $unit** | Макс: **$maxFormatted $unit**\n" +
                "• Среднее: **$avgFormatted $unit** | Колебания: **$flucFormatted%**\n\n" +
                "💡 *Вы можете спросить меня: «Как прошел замер?» или сказать «Покажи график».*"
        addAiChatMessage(finishMessage)

        val minSpeech = com.example.data.formatSensorValueForSpeech(minVal, sensorPid, unit)
        val maxSpeech = com.example.data.formatSensorValueForSpeech(maxVal, sensorPid, unit)
        val avgSpeech = com.example.data.formatSensorValueForSpeech(avgVal, sensorPid, unit)
        val voiceAnnounce = "Замер завершен. Записан лог датчика $sensorName за $durationSec секунд. Мин $minSpeech, макс $maxSpeech, среднее $avgSpeech. Можем обсудить замер или показать график."
        _statusMessage.value = "Замер «$sensorName» завершен. Лог сохранен."
        ttsHelper.speak(voiceAnnounce)
    }

    fun askAiToAnalyzeLastMonitoring() {
        val stats = _lastMonitoringStats.value ?: aiService.lastMonitoringStats ?: return
        val points = aiService.lastMonitoringPoints
        activeAiJob?.cancel()
        activeAiJob = viewModelScope.launch {
            _isAiLoading.value = true
            val promptText = "Проведи экспертную диагностику замера датчика «${stats.sensorName}» за ${stats.durationSeconds} сек. Оцени стабильность, минимум (${stats.min} ${stats.unit}), максимум (${stats.max} ${stats.unit}), среднее (${String.format(java.util.Locale.US, "%.1f", stats.avg)} ${stats.unit})."
            addUserChatMessage(promptText)

            val reply = aiService.analyzeMonitoringSession(stats, points, vehicleInfo.value, chatMessages.value)
            addAiChatMessage(reply)
            _isAiLoading.value = false
            ttsHelper.speak(reply)
        }
    }

    fun askAiToAnalyzeLogFile(file: File) {
        activeAiJob?.cancel()
        activeAiJob = viewModelScope.launch {
            _isAiLoading.value = true
            val parsed = LogDataParser.parseCsvFile(file)
            if (parsed == null) {
                _isAiLoading.value = false
                val errMsg = "Не удалось распарсить файл «${file.name}» для анализа."
                addAiChatMessage(errMsg)
                ttsHelper.speak(errMsg)
                return@launch
            }
            _selectedLogSession.value = parsed
            val userPrompt = "Проведи экспертный анализ ЛОГ-файла телеметрии «${file.name}»."
            addUserChatMessage(userPrompt)

            val reply = aiService.analyzeLogSession(parsed, vehicleInfo.value, chatMessages.value)
            addAiChatMessage(reply)
            _isAiLoading.value = false
            ttsHelper.speak(reply)
        }
    }

    fun askAiToAnalyzeParsedSession(session: ParsedLogSession, userQuestion: String? = null) {
        activeAiJob?.cancel()
        activeAiJob = viewModelScope.launch {
            _isAiLoading.value = true
            _selectedLogSession.value = session
            val promptText = userQuestion ?: "Проведи экспертный анализ параметров телеметрии поездки «${session.fileName}»."
            addUserChatMessage(promptText)

            val reply = aiService.analyzeLogSession(session, vehicleInfo.value, chatMessages.value, userQuestion)
            addAiChatMessage(reply)
            _isAiLoading.value = false
            ttsHelper.speak(reply)
        }
    }

    fun askAiToAnalyzeLatestLog() {
        val files = logger.getLogFiles()
        if (files.isNotEmpty()) {
            val latest = files.maxByOrNull { it.lastModified() } ?: files.first()
            askAiToAnalyzeLogFile(latest)
        } else {
            viewModelScope.launch {
                val errMsg = "В памяти еще нет сохраненных поездок. Запустите поездку или мониторинг для записи лога."
                addAiChatMessage(errMsg)
                ttsHelper.speak(errMsg)
            }
        }
    }

    private suspend fun handleAiCommand(text: String): Boolean {
        var rawQuery = text.trim()
        var q = rawQuery.lowercase()
        // Normalize numbers
        q = q.replace("(\\d+)[.,\\s](\\d{3})(?!\\d)".toRegex(), "$1$2").replace(",", ".")

        // 0. Immediate Stop / Silence Command ("Стоп", "Хватит", "Замолчи", "Тишина", "Остановись")
        val isStopSpeechCommand = q == "стоп" || q == "стоп игра" || q == "остановись" || q == "хватит" || 
                q == "замолчи" || q == "молчи" || q == "прекрати" || q == "тишина" || 
                q == "стоп говорить" || q == "отмена" || q == "перестань" ||
                q == "стоп звук" || q == "выключи звук" ||
                q.startsWith("стоп ") || q.startsWith("хватит ") || q.startsWith("замолчи ")
        if (isStopSpeechCommand) {
            stopSpeaking()
            return true
        }

        val isConnected = connectionState.value == ConnectionState.CONNECTED_BLUETOOTH
        val currentSensors = sensors.value
        val replyParts = mutableListOf<String>()

        // 0.1 Voice Command: Start / Stop Recording Log in a separate file (all sensors selected in settings)
        val isVoiceLogStart = (
            q.contains("запиши лог") || q.contains("записать лог") ||
            q.contains("начни запись лог") || q.contains("запусти запись лог") ||
            q.contains("включи запись лог") || q.contains("записывай лог") ||
            q.contains("пиши лог") || q.contains("сохрани лог") ||
            q.contains("запись параметров") || q.contains("запиши датчики") ||
            q.contains("начать запись лог") || q.contains("запись в лог") ||
            q.contains("start log") || q.contains("record log")
        ) && !q.contains("останов") && !q.contains("прекрат") && !q.contains("заверш") && !q.contains("стоп") && !q.contains("выключ")

        val isVoiceLogStop = (
            q.contains("останови запись лог") || q.contains("остановить запись лог") ||
            q.contains("заверши запись лог") || q.contains("прекрати запись лог") ||
            q.contains("стоп лог") || q.contains("останови лог") ||
            q.contains("выключи запись лог") || q.contains("хватит писать лог") ||
            q.contains("стоп запись") || q.contains("stop log")
        )

        if (isVoiceLogStart) {
            val session = startVoiceLogging()
            val selectedSensors = sensors.value.filter { it.isSelected }.ifEmpty { sensors.value }
            val count = selectedSensors.size
            val names = selectedSensors.take(4).joinToString { it.name } + if (count > 4) " и еще ${count - 4}" else ""
            val fileName = session?.fileName ?: "VoiceLog.csv"
            val resp = "Начинаю запись в отдельный файл «$fileName».\nВ лог записываются все датчики, выбранные в настройках ($count шт.): $names.\n\nДля завершения скажите: «Останови запись лога»."
            addUserChatMessage(text)
            addAiChatMessage(resp)
            ttsHelper.speak("Запись отдельного лога начата. Пишу все выбранные в настройках датчики: $count штук.")
            return true
        }

        if (isVoiceLogStop) {
            val session = stopVoiceLogging()
            val resp = if (session != null) {
                "Запись отдельного лога завершена.\nСохранено ${session.recordCount} точек (${session.durationSeconds} сек) в файл «${session.fileName}».\nФайл сохранен в журнале логов."
            } else {
                "Запись отдельного лога не была активна."
            }
            addUserChatMessage(text)
            addAiChatMessage(resp)
            ttsHelper.speak(if (session != null) "Запись отдельного лога завершена. Сохранено ${session.recordCount} точек." else "Запись лога не была запущена.")
            return true
        }

        // 1. Navigation Voice Commands
        val isNavDiagnostics = (q.contains("открой ошибки") || q.contains("покажи ошибки") || 
                q.contains("экран ошибок") || q.contains("экран диагностики") || 
                q.contains("перейди в диагностику") || q.contains("раздел ошибок"))
        if (isNavDiagnostics) {
            navigateTo("mode1_diagnostics")
            val resp = if (dtcErrors.value.isEmpty()) "Открываю диагностику. Активных ошибок нет." else "Открываю диагностику. Найдено ошибок: ${dtcErrors.value.size}."
            addUserChatMessage(text)
            addAiChatMessage(resp)
            ttsHelper.speak(resp)
            return true
        }

        val isNavGraphs = (q.contains("покажи график") || q.contains("открой график") ||
                q.contains("показать график") || q.contains("выведи график") ||
                q.contains("покажи графики") || q.contains("открой графики") || 
                q.contains("экран графиков") || q.contains("график лога") ||
                q.contains("график поездки") || q.contains("выведи мне график"))
        if (isNavGraphs) {
            if (_selectedLogSession.value == null) {
                val lastMon = _lastMonitoringFile.value
                if (lastMon != null && lastMon.exists()) {
                    val parsed = LogDataParser.parseCsvFile(lastMon)
                    if (parsed != null) _selectedLogSession.value = parsed
                } else {
                    val files = logger.ensureSampleTripLogsExist()
                    val target = files.find { it.name.contains("_1605") } ?: files.firstOrNull()
                    if (target != null) {
                        val parsed = LogDataParser.parseCsvFile(target)
                        if (parsed != null) _selectedLogSession.value = parsed
                    }
                }
            }
            navigateTo("log_graph_viewer")
            val resp = "Открываю просмотр графиков."
            addUserChatMessage(text)
            addAiChatMessage(resp)
            ttsHelper.speak(resp)
            return true
        }

        val isNavSensors = (q.contains("открой датчики") || q.contains("покажи датчики") ||
                q.contains("живые данные") || q.contains("экран датчиков") ||
                q.contains("все приборы") || q.contains("панель приборов"))
        if (isNavSensors) {
            navigateTo("mode3_sensors")
            val resp = "Открываю экран датчиков в реальном времени."
            addUserChatMessage(text)
            addAiChatMessage(resp)
            ttsHelper.speak(resp)
            return true
        }

        val isNavLogs = (q.contains("открой историю") || q.contains("журнал логов") ||
                q.contains("список логов") || q.contains("история поездок") ||
                q.contains("мои поездки") || q.contains("открой логи"))
        if (isNavLogs) {
            navigateTo("logs_history")
            val resp = "Открываю журнал поездок и логов."
            addUserChatMessage(text)
            addAiChatMessage(resp)
            ttsHelper.speak(resp)
            return true
        }

        val isNavSettings = (q.contains("открой настройки") || q.contains("перейди в настройки") || q == "настройки")
        if (isNavSettings) {
            navigateTo("settings")
            val resp = "Открываю настройки приложения."
            addUserChatMessage(text)
            addAiChatMessage(resp)
            ttsHelper.speak(resp)
            return true
        }

        val isNavHome = (q.contains("главный экран") || q.contains("на главную") || q == "домой" || q == "назад")
        if (isNavHome) {
            navigateTo("home")
            val resp = "Перехожу на главный экран."
            addUserChatMessage(text)
            addAiChatMessage(resp)
            ttsHelper.speak(resp)
            return true
        }

        // 2. Clear DTC / Reset Check Engine Voice Command (Broad matching for natural commands)
        val hasClearWord = q.contains("сброс") || q.contains("сбрось") || q.contains("очист") || 
                q.contains("удал") || q.contains("сотр") || q.contains("стереть") || 
                q.contains("погас") || q.contains("потуш") || q.contains("убер")
        val hasDtcTarget = q.contains("ошибк") || q.contains("чек") || q.contains("check") || 
                q.contains("неисправност") || q.contains("памят") || q.contains("коды")
        val isClearDtcRequest = hasClearWord && hasDtcTarget

        // 3. Diagnostic Scan Voice Command
        val isDiagScanRequest = (
            q.contains("диагностик") || q.contains("просканируй") || 
            q.contains("запусти скан") || q.contains("проверь ошибк") || 
            q.contains("сделай скан") || q.contains("поиск ошибок") ||
            (q.contains("провер") && (q.contains("блок") || q.contains("автомобил") || q.contains("машин") || q.contains("эбу") || q.contains("чек")))
        ) && !hasClearWord

        // 4. Query Current Errors Voice Command
        val isQueryErrorsRequest = (
            q.contains("какие ошибк") || q.contains("есть ошибк") || 
            q.contains("есть ли ошибк") || q.contains("что с ошибк") || 
            q.contains("что по ошибк") || q.contains("список ошибок") || 
            q.contains("какие коды") || q.contains("горит ли чек") || 
            q.contains("какие неисправност") || q.contains("есть неисправност")
        ) && !isDiagScanRequest && !hasClearWord

        // 5. Explicit Log Analysis Request
        val isLogAnalysisRequest = (
            q.contains("проанализируй лог") || q.contains("анализ лога") || 
            q.contains("что в логе") || q.contains("разбери лог") ||
            q.contains("проанализируй поездку") || q.contains("экспертный анализ лога")
        )
        if (isLogAnalysisRequest) {
            askAiToAnalyzeLatestLog()
            return true
        }

        // 6. Check for Discussing Last Monitoring Session
        val isDiscussMonitoringRequest = (
            q.contains("как замер") || q.contains("про замер") || q.contains("по замеру") ||
            q.contains("о замере") || q.contains("что с замером") || q.contains("итог замера") ||
            q.contains("результат замера") || q.contains("оцени замер") || q.contains("проанализируй замер") ||
            q.contains("как прошел замер")
        )
        if (isDiscussMonitoringRequest && (_lastMonitoringStats.value != null || aiService.lastMonitoringStats != null)) {
            askAiToAnalyzeLastMonitoring()
            return true
        }

        // 5. Check for AI Sensor Monitoring commands (e.g. "отследи скорость", "замерь температуру на минуту")
        val isMonitoringRequest = (
            q.contains("помониторь") || q.contains("промониторь") || q.contains("мониторь") ||
            q.contains("отследи") || q.contains("отслеживай") || q.contains("проследи") || q.contains("следи за") ||
            q.contains("понаблюдай") || q.contains("наблюдай") ||
            q.contains("замерь") || q.contains("замеряй") || q.contains("сделай замер") || q.contains("запусти замер") ||
            q.contains("включи мониторинг") || q.contains("запусти мониторинг")
        ) && (
            q.contains("скорост") || q.contains("температур") || q.contains("оборот") || q.contains("вольт") ||
            q.contains("напряжен") || q.contains("дроссел") || q.contains("давлен") || q.contains("маф") ||
            q.contains("топлив") || q.contains("расход") || q.contains("зажиган") || q.contains("нагрузк")
        )

        // 6. Alert disable / enable / clear
        val isDisableAlertsRequest = (
            (q.contains("отключ") || q.contains("выключ") || q.contains("заглуш") || q.contains("выруб") || 
             q.contains("сним") || q.contains("погас") || q.contains("замолч") || q.contains("убер") || 
             q.contains("останов") || q.contains("хватит") || q.contains("тишин") || q.contains("без звук")) &&
            (q.contains("аларм") || q.contains("предупрежден") || q.contains("оповещен") || 
             q.contains("сигнал") || q.contains("звук") || q.contains("пищалк") || q.contains("зуммер"))
        ) || q == "отключи аларм" || q == "отключи алармы" || q == "выключи аларм" || q == "выключи алармы" ||
             q == "заглуши аларм" || q == "выруби аларм" || q == "стоп сигнал" || q == "тишина"

        val isEnableAlertsRequest = (
            (q.contains("включи") || q.contains("активируй") || q.contains("верни")) &&
            (q.contains("аларм") || q.contains("предупрежден") || q.contains("оповещен") || q.contains("сигнал"))
        )

        val isExplicitAlertCreate = (
            q.contains("добавь предупреждение") || q.contains("поставь предупреждение") ||
            q.contains("создай предупреждение") || q.contains("добавь алерт") ||
            q.contains("создай алерт") || q.contains("поставь алерт") ||
            q.contains("предупреждение по") || q.contains("предупреждение о") ||
            (q.contains("предупреди") && (q.contains("если") || q.contains("когда") || q.contains("при") || q.contains(">") || q.contains("<") || q.contains("выше") || q.contains("ниже") || q.contains("больше") || q.contains("меньше") || q.contains("будет") || q.contains("станет") || q.contains("будут"))) ||
            (q.contains("сообщи") && (q.contains("если") || q.contains("когда") || q.contains("при") || q.contains(">") || q.contains("<") || q.contains("выше") || q.contains("ниже") || q.contains("больше") || q.contains("меньше") || q.contains("будет") || q.contains("станет") || q.contains("будут"))) ||
            (q.contains("уведоми") && (q.contains("если") || q.contains("когда") || q.contains("при") || q.contains(">") || q.contains("<") || q.contains("выше") || q.contains("ниже") || q.contains("больше") || q.contains("меньше") || q.contains("будет") || q.contains("станет") || q.contains("будут"))) ||
            (q.contains("подай сигнал") && (q.contains("если") || q.contains("когда") || q.contains("при") || q.contains(">") || q.contains("<"))) ||
            (q.contains("включи сигнал") && (q.contains("если") || q.contains("когда") || q.contains("при") || q.contains(">") || q.contains("<")))
        )

        // 7. Direct Car Telemetry queries (Strict: user asks specifically about vehicle telemetry)
        val isSpeedQuery = (q.contains("какая сейчас скорость") || q.contains("какая скорость") || 
                q.contains("с какой скоростью едем") || q.contains("сколько едем") || 
                q.contains("на спидометре") || q.contains("скорость авто") || q == "скорость" || q == "спидометр") &&
                !isMonitoringRequest && !isExplicitAlertCreate && !isDisableAlertsRequest

        val isRpmQuery = (q.contains("какие обороты") || q.contains("сколько оборотов") || 
                q.contains("на тахометре") || q.contains("обороты двигателя") || 
                q.contains("обороты мотора") || q == "обороты" || q == "тахометр") &&
                !isMonitoringRequest && !isExplicitAlertCreate && !isDisableAlertsRequest

        val isEngineTempQuery = (q.contains("температура двигателя") || q.contains("температура мотора") || 
                q.contains("температура ож") || q.contains("температура охлаждающей") || 
                q.contains("двигатель греется") || q.contains("перегрев мотора")) &&
                !isMonitoringRequest && !isExplicitAlertCreate && !isDisableAlertsRequest

        val isIntakeTempQuery = (q.contains("температура во впуске") || q.contains("температура на впуске") || 
                q.contains("датчик iat")) &&
                !isMonitoringRequest && !isExplicitAlertCreate && !isDisableAlertsRequest

        val isVoltQuery = (q.contains("напряжение аккумулятора") || q.contains("напряжение акб") || 
                q.contains("напряжение сети") || q.contains("зарядка аккумулятора") || 
                q.contains("зарядка акб") || q.contains("заряд генератора") || 
                q.contains("сколько вольт в сети") || q.contains("напряжение бортовой сети") ||
                q == "напряжение" || q == "вольтаж") &&
                !isMonitoringRequest && !isExplicitAlertCreate && !isDisableAlertsRequest

        val isFuelLevelQuery = (q.contains("сколько топлива в баке") || q.contains("уровень топлива") || 
                q.contains("сколько бензина в баке") || q.contains("остаток топлива") || 
                q.contains("сколько в баке")) &&
                !isMonitoringRequest && !isExplicitAlertCreate && !isDisableAlertsRequest

        val isFuelConsQuery = (q.contains("расход топлива") || q.contains("какой расход у машины") || 
                q.contains("сколько расходует авто") || q.contains("сколько жрет")) &&
                !isMonitoringRequest && !isExplicitAlertCreate && !isDisableAlertsRequest

        val isAllSensorsQuery = (q.contains("все датчики") || q.contains("все приборы") || 
                q.contains("сводка датчиков") || q.contains("показания авто") || 
                q.contains("статус автомобиля")) &&
                !isMonitoringRequest && !isExplicitAlertCreate && !isDisableAlertsRequest

        // Execute actions and build responses for matched intents
        if (isClearDtcRequest) {
            if (isConnected) {
                clearDtcErrors()
                replyParts.add("Выполняю команду сброса ошибок и очистки памяти ЭБУ.")
            } else {
                replyParts.add("Для сброса ошибок подключите адаптер ELM327 и включите зажигание.")
            }
        }

        if (isDiagScanRequest) {
            if (isConnected) {
                performFullEcuScan()
                replyParts.add("Запускаю сканирование блоков управления на ошибки.")
            } else {
                replyParts.add("Для запуска диагностики подключите адаптер ELM327.")
            }
        }

        if (isQueryErrorsRequest) {
            val wantsDetailed = q.contains("подробн") || q.contains("детальн") || q.contains("разверн") || q.contains("почему") || q.contains("расскажи")
            if (wantsDetailed && replyParts.isEmpty()) {
                _isAiLoading.value = true
                addUserChatMessage(text)
                val reply = aiService.consultOnAllDtcs(dtcErrors.value, vehicleInfo.value, chatMessages.value)
                addAiChatMessage(reply)
                _isAiLoading.value = false
                ttsHelper.speak(reply)
                return true
            } else {
                val reply = if (dtcErrors.value.isEmpty()) {
                    "Ошибок не обнаружено, все системы автомобиля в норме."
                } else {
                    val count = dtcErrors.value.size
                    val list = dtcErrors.value.joinToString(", ") { "${it.code} (${it.description})" }
                    "Найдено $count ошибок: $list."
                }
                replyParts.add(reply)
            }
        }

        if (isDisableAlertsRequest) {
            val targetSensorPid = when {
                q.contains("впуск") -> "010F"
                q.contains("оборот") || q.contains("тахометр") || q.contains("rpm") || q.contains("об/мин") -> "010C"
                q.contains("температур") || q.contains("двс") || q.contains("греет") || q.contains("перегрев") || q.contains("ож") -> "0105"
                q.contains("вольт") || q.contains("напряжен") || q.contains("аккум") || q.contains("акб") || q.contains("заряд") || q.contains("батаре") -> "0142"
                q.contains("скорост") || q.contains("спидометр") || q.contains("км") -> "010D"
                q.contains("топлив") || q.contains("бак") || q.contains("бензин") -> "012F"
                else -> null
            }

            _aiAlert.value = null
            ttsHelper.stop()

            if (targetSensorPid != null) {
                _sensorAlerts.value = _sensorAlerts.value.map {
                    if (it.sensorPid == targetSensorPid) it.copy(isEnabled = false) else it
                }
                replyParts.add("Предупреждение отключено.")
            } else {
                _sensorAlerts.value = _sensorAlerts.value.map { it.copy(isEnabled = false) }
                replyParts.add("Активные алармы отключены.")
            }
            persistProfile()
        }

        if (isEnableAlertsRequest) {
            _sensorAlerts.value = _sensorAlerts.value.map { it.copy(isEnabled = true) }
            persistProfile()
            replyParts.add("Алармы и предупреждения включены.")
        }

        val isRepeatIntervalRequest = (q.contains("повторн") || q.contains("интервал") || q.contains("повтор")) &&
                (q.contains("оповещен") || q.contains("предупрежден") || q.contains("датчик") || q.contains("сигнал") || q.contains("алерт"))
        if (isRepeatIntervalRequest) {
            val num = "(\\d+)".toRegex().find(q)?.value?.toIntOrNull() ?: 30
            val targetInterval = if (num > 0) num else 30
            _sensorAlerts.value = _sensorAlerts.value.map { it.copy(repeatIntervalSeconds = targetInterval) }
            persistProfile()
            replyParts.add("Установлен интервал повторного оповещения датчиков: $targetInterval секунд.")
        }

        if (q.contains("удали все предупреждения") || q.contains("очисти предупреждения") || q.contains("сбрось предупреждения") || q.contains("удали предупреждения")) {
            _sensorAlerts.value = emptyList()
            persistProfile()
            replyParts.add("Список предупреждений и порогов датчиков очищен.")
        }

        if (isExplicitAlertCreate) {
            val numberRegex = "(\\d+(\\.\\d+)?)".toRegex()
            val numbers = numberRegex.findAll(q).mapNotNull { it.value.toDoubleOrNull() }.toList()

            if (numbers.isNotEmpty()) {
                val targetValue = numbers[0]
                val sensorTriple = when {
                    q.contains("впуск") -> Triple("010F", "Температура впуска", "°C")
                    q.contains("оборот") || q.contains("тахометр") || q.contains("rpm") || q.contains("об/мин") -> Triple("010C", "Обороты двигателя", "об/мин")
                    q.contains("температур") || q.contains("двс") || q.contains("греет") || q.contains("перегрев") || q.contains("ож") || q.contains("градус") -> Triple("0105", "Температура двигателя", "°C")
                    q.contains("вольт") || q.contains("напряжен") || q.contains("аккум") || q.contains("акб") || q.contains("заряд") || q.contains("батаре") -> Triple("0142", "Напряжение аккумулятора", "В")
                    q.contains("скорост") || q.contains("спидометр") || q.contains("км") -> Triple("010D", "Скорость", "км/ч")
                    q.contains("моментальн") -> Triple("015E", "Моментальный расход топлива", "л/100км")
                    q.contains("расход") -> Triple("015F", "Средний расход топлива", "л/100км")
                    q.contains("топлив") || q.contains("бак") || q.contains("бензин") || q.contains("уровень") -> Triple("012F", "Уровень топлива", "%")
                    else -> Triple("0105", "Температура двигателя", "°C")
                }

                val condition = if (q.contains("меньше") || q.contains("упадет") || q.contains("упасть") || q.contains("ниже") || q.contains("падении") || q.contains("снизится") || q.contains("уменьшится") || q.contains("<")) {
                    AlertCondition.LESS_THAN
                } else {
                    AlertCondition.GREATER_THAN
                }

                val condLabel = if (condition == AlertCondition.GREATER_THAN) "выше" else "ниже"
                val displayVal = if (targetValue % 1.0 == 0.0) targetValue.toInt().toString() else targetValue.toString()

                val newAlert = SensorAlert(
                    sensorPid = sensorTriple.first,
                    sensorName = sensorTriple.second,
                    condition = condition,
                    thresholdValue = targetValue,
                    unit = sensorTriple.third,
                    customMessage = "Предупреждение по ${sensorTriple.second}"
                )
                addSensorAlert(newAlert)
                replyParts.add("Создано предупреждение: подать сигнал, когда ${sensorTriple.second} станет $condLabel $displayVal ${sensorTriple.third}.")
            }
        }

        if (isMonitoringRequest) {
            var duration = 30
            when {
                q.contains("полторы минуты") || q.contains("полтора минуты") -> duration = 90
                q.contains("полминуты") || q.contains("пол минуты") -> duration = 30
                q.contains("две минуты") || q.contains("2 минуты") || q.contains("2 мин") -> duration = 120
                q.contains("три минуты") || q.contains("3 минуты") || q.contains("3 мин") -> duration = 180
                q.contains("минуту") || q.contains("минута") || q.contains("1 минуту") || q.contains("одну минуту") || q.contains("1 мин") -> duration = 60
                else -> {
                    val minRegex = "(\\d+)\\s*(мин|минут)".toRegex()
                    val minMatch = minRegex.find(q)
                    if (minMatch != null) {
                        duration = (minMatch.groupValues[1].toIntOrNull() ?: 1) * 60
                    } else {
                        val secRegex = "(\\d+)\\s*(сек|секунд|с\\b)".toRegex()
                        val secMatch = secRegex.find(q)
                        if (secMatch != null) {
                            duration = secMatch.groupValues[1].toIntOrNull() ?: 30
                        } else {
                            val inTimeRegex = "(?:в течени[еи]|на протяжении|на)\\s*(\\d+)".toRegex()
                            val inMatch = inTimeRegex.find(q)
                            if (inMatch != null) {
                                duration = inMatch.groupValues[1].toIntOrNull() ?: 30
                            } else {
                                val anyNumber = "(\\d+)".toRegex().find(q)
                                if (anyNumber != null) {
                                    duration = anyNumber.value.toIntOrNull() ?: 30
                                }
                            }
                        }
                    }
                }
            }
            duration = duration.coerceIn(5, 180)

            val sensorTriple = when {
                q.contains("скорост") || q.contains("спидометр") || q.contains("км/ч") || q.contains("разгон") -> Triple("010D", "Скорость", "км/ч")
                q.contains("впуск") -> Triple("010F", "Температура впуска", "°C")
                q.contains("оборот") || q.contains("тахометр") || q.contains("rpm") || q.contains("об/мин") -> Triple("010C", "Обороты двигателя", "об/мин")
                q.contains("вольт") || q.contains("напряжен") || q.contains("аккум") || q.contains("акб") || q.contains("заряд") || q.contains("батаре") || q.contains("генератор") -> Triple("0142", "Напряжение аккумулятора", "В")
                q.contains("температур") || q.contains("двс") || q.contains("двигател") || q.contains("мотор") || q.contains("греет") || q.contains("перегрев") || q.contains("ож") || q.contains("градус") -> Triple("0105", "Температура двигателя", "°C")
                q.contains("дроссел") || q.contains("заслонк") || q.contains("педал") -> Triple("0111", "Положение дроссельной заслонки", "%")
                q.contains("давлен") || q.contains("map") || q.contains("коллектор") -> Triple("010B", "Давление во впуске (MAP)", "кПа")
                q.contains("маф") || q.contains("воздух") || q.contains("maf") -> Triple("0110", "Расход воздуха (MAF)", "г/с")
                q.contains("моментальн") || q.contains("расход") -> Triple("015E", "Моментальный расход топлива", "л/100км")
                q.contains("топлив") || q.contains("бак") || q.contains("бензин") || q.contains("уровень") -> Triple("012F", "Уровень топлива", "%")
                q.contains("коррекц") -> Triple("0106", "Краткосрочная коррекция топлива", "%")
                q.contains("зажиган") || q.contains("уоз") || q.contains("угол") -> Triple("010E", "Угол опережения зажигания", "°")
                q.contains("нагрузк") -> Triple("0104", "Расчетная нагрузка на двигатель", "%")
                else -> Triple("010D", "Скорость", "км/ч")
            }

            startAiSensorMonitoring(sensorTriple.first, sensorTriple.second, sensorTriple.third, duration)
            addUserChatMessage(text)
            val ackMsg = "Запускаю замер датчика «${sensorTriple.second}» на $duration сек с записью лога."
            addAiChatMessage(ackMsg)
            ttsHelper.speak(ackMsg)
            return true
        }

        if (isSpeedQuery) {
            if (isConnected) {
                val speedSensor = currentSensors.find { it.pid == "010D" }
                val speedVal = speedSensor?.value?.toInt() ?: 0
                val textSpd = if (speedVal > 0) "Скорость автомобиля: $speedVal км/ч." else "Скорость: 0 км/ч (автомобиль стоит на месте)."
                replyParts.add(textSpd)
            } else {
                replyParts.add("Данные спидометра недоступны (ELM327 не подключен).")
            }
        }

        if (isRpmQuery) {
            if (isConnected) {
                val rpmSensor = currentSensors.find { it.pid == "010C" }
                val rpmVal = rpmSensor?.value?.toInt() ?: 0
                val textRpm = if (rpmVal > 0) "Обороты двигателя: $rpmVal об/мин." else "Обороты: 0 об/мин (двигатель заглушен)."
                replyParts.add(textRpm)
            } else {
                replyParts.add("Данные тахометра недоступны (ELM327 не подключен).")
            }
        }

        if (isEngineTempQuery) {
            if (isConnected) {
                val tempSensor = currentSensors.find { it.pid == "0105" }
                val tempVal = tempSensor?.value?.toInt() ?: 0
                val statusText = when {
                    tempVal < 70 -> "мотор прогревается"
                    tempVal in 70..105 -> "рабочая температура в норме"
                    else -> "внимание, повышенная температура!"
                }
                replyParts.add("Температура охлаждающей жидкости: $tempVal °C ($statusText).")
            } else {
                replyParts.add("Температура охлаждающей жидкости недоступна (ELM327 не подключен).")
            }
        }

        if (isIntakeTempQuery) {
            if (isConnected) {
                val iatSensor = currentSensors.find { it.pid == "010F" }
                val iatVal = iatSensor?.value?.toInt() ?: 0
                replyParts.add("Температура воздуха на впуске: $iatVal °C.")
            } else {
                replyParts.add("Температура на впуске недоступна (ELM327 не подключен).")
            }
        }

        if (isVoltQuery) {
            if (isConnected) {
                val voltSensor = currentSensors.find { it.pid == "0142" }
                val voltVal = voltSensor?.value ?: 0.0
                val vStr = com.example.data.formatSensorValueForSpeech(voltVal, "0142", "В")
                val statusText = when {
                    voltVal in 13.6..14.8 -> "зарядка генератора в норме"
                    voltVal > 14.8 -> "внимание, повышенное напряжение"
                    voltVal in 11.8..13.5 -> "напряжение в норме"
                    else -> "низкий заряд"
                }
                replyParts.add("Напряжение в бортовой сети: $vStr В ($statusText).")
            } else {
                replyParts.add("Напряжение аккумулятора недоступно (ELM327 не подключен).")
            }
        }

        if (isFuelLevelQuery) {
            if (isConnected) {
                val flSensor = currentSensors.find { it.pid == "012F" }
                val flVal = flSensor?.value?.toInt() ?: 0
                replyParts.add("Уровень топлива в баке: $flVal %.")
            } else {
                replyParts.add("Данные уровня топлива недоступны (ELM327 не подключен).")
            }
        }

        if (isFuelConsQuery) {
            if (isConnected) {
                val instSensor = currentSensors.find { it.pid == "015E" }
                val avgSensor = currentSensors.find { it.pid == "015F" }
                val instVal = com.example.data.formatSensorValueForSpeech(instSensor?.value ?: 0.0, "015E", "л/100км")
                val avgVal = com.example.data.formatSensorValueForSpeech(avgSensor?.value ?: 0.0, "015F", "л/100км")
                replyParts.add("Расход топлива: моментальный $instVal л/100км, средний $avgVal л/100км.")
            } else {
                replyParts.add("Данные расхода топлива недоступны (ELM327 не подключен).")
            }
        }

        if (isAllSensorsQuery) {
            if (isConnected) {
                val rpm = com.example.data.formatSensorValueForSpeech(currentSensors.find { it.pid == "010C" }?.value ?: 0.0, "010C", "об/мин")
                val temp = com.example.data.formatSensorValueForSpeech(currentSensors.find { it.pid == "0105" }?.value ?: 0.0, "0105", "°C")
                val volt = com.example.data.formatSensorValueForSpeech(currentSensors.find { it.pid == "0142" }?.value ?: 14.1, "0142", "В")
                val speed = com.example.data.formatSensorValueForSpeech(currentSensors.find { it.pid == "010D" }?.value ?: 0.0, "010D", "км/ч")
                val fuel = com.example.data.formatSensorValueForSpeech(currentSensors.find { it.pid == "012F" }?.value ?: 0.0, "012F", "%")
                replyParts.add("Параметры: скорость $speed км/ч, обороты $rpm об/мин, температура $temp °C, напряжение $volt В, топливо $fuel %.")
            } else {
                replyParts.add("ELM327 не подключен. Подключите адаптер для считывания телеметрии.")
            }
        }

        if (replyParts.isNotEmpty()) {
            addUserChatMessage(text)
            val combinedReply = replyParts.joinToString(" ")
            addAiChatMessage(combinedReply)
            ttsHelper.speak(combinedReply)
            return true
        }

        return false
    }


    fun exportProfileToFile(context: android.content.Context): File? {
        val profile = currentProfileData()
        return ProfileManager.exportProfileToFile(context, profile)
    }

    fun exportProfileToUri(context: android.content.Context, uri: android.net.Uri): Boolean {
        val profile = currentProfileData()
        return ProfileManager.saveProfileToUri(context, uri, profile)
    }

    fun importProfileFromUri(context: android.content.Context, uri: android.net.Uri): Boolean {
        val profile = ProfileManager.loadProfileFromUri(context, uri) ?: return false
        applyProfile(profile)
        return true
    }

    fun getExportProfileJson(): String {
        val profile = currentProfileData()
        return ProfileManager.serializeToJson(profile)
    }

    fun importProfileFromJson(jsonStr: String): Boolean {
        val profile = ProfileManager.deserializeFromJson(jsonStr) ?: return false
        applyProfile(profile)
        return true
    }

    private fun applyProfile(profile: AppProfileData) {
        _sensorAlerts.value = profile.alerts
        _chatMessages.value = profile.chatMessages
        ttsHelper.setVoiceEnabled(profile.isVoiceEnabled)
        handsFreeController.updateSettings(
            wakePhrase = profile.wakePhrase,
            readyResponse = profile.readyResponse,
            isEnabled = profile.isHandsFreeEnabled
        )
        if (profile.savedDeviceAddress != null && profile.savedDeviceName != null) {
            elm327Manager.saveConnectedDevice(profile.savedDeviceAddress, profile.savedDeviceName)
        }

        viewModelScope.launch {
            dao.clearChatHistory()
            profile.chatMessages.forEach { msg ->
                dao.insertChatMessage(ChatMessageEntity(msg.id, msg.sender.name, msg.text, msg.timestamp))
            }
        }
        persistProfile()
    }

    fun exitApplication(context: android.content.Context) {
        persistProfile()
        elm327Manager.disconnect()
        ObdForegroundService.stopService(context)
        val exitIntent = Intent(ObdForegroundService.ACTION_EXIT_APP).apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(exitIntent)
    }

    fun stopSpeaking() {
        activeAiJob?.cancel()
        activeAiJob = null
        _isAiLoading.value = false
        ttsHelper.stop()
    }

    fun startVoiceInput() {
        stopSpeaking()
        speechHelper.startListening { spoken ->
            if (spoken.isNotBlank()) {
                askAiCustomQuery(spoken)
            }
        }
    }

    fun stopVoiceInput() {
        speechHelper.stopListening()
    }

    private suspend fun addUserChatMessage(text: String) {
        val msg = ChatMessage(sender = ChatSender.USER, text = text)
        dao.insertChatMessage(ChatMessageEntity(msg.id, "USER", text, msg.timestamp))
        val cutoffWeekMs = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000L
        try { dao.deleteChatMessagesOlderThan(cutoffWeekMs) } catch (e: Exception) {}
    }

    private suspend fun addAiChatMessage(text: String) {
        val msg = ChatMessage(sender = ChatSender.AI, text = text)
        dao.insertChatMessage(ChatMessageEntity(msg.id, "AI", text, msg.timestamp))
        val cutoffWeekMs = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000L
        try { dao.deleteChatMessagesOlderThan(cutoffWeekMs) } catch (e: Exception) {}
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            dao.clearChatHistory()
        }
    }

    fun selectLogFileForGraph(file: File) {
        val parsed = LogDataParser.parseCsvFile(file)
        _selectedLogSession.value = parsed
    }

    fun shareCurrentLog() {
        val session = _selectedLogSession.value ?: return
        val logs = logger.getLogFiles()
        val match = logs.find { it.name == session.fileName }
        if (match != null) {
            logger.shareLogFile(match)
        }
    }

    fun deleteCurrentLog(): Boolean {
        val session = _selectedLogSession.value ?: return false
        val logs = logger.getLogFiles()
        val match = logs.find { it.name == session.fileName }
        val success = if (match != null) {
            logger.deleteLogFile(match)
        } else {
            false
        }
        _selectedLogSession.value = null
        return success
    }

    fun updateFuelLevelManually(levelPercent: Double) {
        elm327Manager.updateFuelLevelManually(levelPercent)
    }

    // OBD Protocol management
    val availableProtocols = elm327Manager.availableProtocols
    val selectedProtocol: StateFlow<String> = elm327Manager.selectedProtocol

    fun applyProtocol(protocolCode: String, onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            val res = elm327Manager.applyProtocol(protocolCode)
            _statusMessage.value = res.second
            onResult?.invoke(res.first, res.second)
        }
    }

    // Fuel Level Strategy management
    val availableFuelStrategies = elm327Manager.adaptiveResolver.getAllFuelStrategies()
    val selectedFuelStrategyId: StateFlow<String> = elm327Manager.selectedFuelStrategyId

    fun setFuelStrategy(strategyId: String) {
        elm327Manager.setFuelStrategy(strategyId)
        val stratName = availableFuelStrategies.find { it.id == strategyId }?.name ?: strategyId
        _statusMessage.value = "Выбран метод уровня топлива: $stratName"
    }

    fun testFuelStrategy(strategyId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = elm327Manager.testFuelStrategy(strategyId)
            _statusMessage.value = res.second
            onResult(res.first, res.second)
        }
    }

    override fun onCleared() {
        persistProfile()
        super.onCleared()
        ttsHelper.shutdown()
        soundEffectsHelper.release()
        speechHelper.destroy()
        handsFreeController.destroy()
        elm327Manager.disconnect()
    }
}

data class AiMonitoringState(
    val isActive: Boolean = false,
    val sensorPid: String = "010C",
    val sensorName: String = "Обороты двигателя",
    val unit: String = "об/мин",
    val targetDurationSeconds: Int = 15,
    val elapsedSeconds: Int = 0,
    val currentLiveValue: Double = 0.0,
    val points: List<Double> = emptyList()
)
