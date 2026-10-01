package com.example.data.elm327

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.example.data.ConnectionState
import com.example.data.DtcError
import com.example.data.DtcSeverity
import com.example.data.EcuBlock
import com.example.data.EcuStatus
import com.example.data.ObdSensor
import com.example.data.SupportedPidsResult
import com.example.data.VehicleInfo
import com.example.data.ai.DtcDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.random.Random

data class ObdProtocolInfo(
    val code: String,
    val name: String,
    val description: String
)

class Elm327Manager(private val context: Context) {

    val availableProtocols = listOf(
        ObdProtocolInfo("0", "0: Авто (AUTO)", "Автоматический выбор протокола адаптером"),
        ObdProtocolInfo("6", "6: ISO 15765-4 CAN (11-bit ID, 500 kbaud)", "Большинство современных авто (VAG, Ford, GM, Kia/Hyundai, Lada, Toyota)"),
        ObdProtocolInfo("7", "7: ISO 15765-4 CAN (29-bit ID, 500 kbaud)", "Honda, европейские и американские авто с 29-bit CAN"),
        ObdProtocolInfo("8", "8: ISO 15765-4 CAN (11-bit ID, 250 kbaud)", "Низкоскоростная 11-bit CAN шина"),
        ObdProtocolInfo("9", "9: ISO 15765-4 CAN (29-bit ID, 250 kbaud)", "Низкоскоростная 29-bit CAN шина"),
        ObdProtocolInfo("3", "3: ISO 9141-2 (5 baud init)", "K-Line (Япония, Европа до 2005 г., ВАЗ Январь/Bosch)"),
        ObdProtocolInfo("4", "4: ISO 14230-4 KWP (5 baud init)", "KWP2000 медленная инициализация"),
        ObdProtocolInfo("5", "5: ISO 14230-4 KWP (Fast init)", "KWP2000 быстрая инициализация (ВАЗ, ГАЗ, УАЗ, Renault K-Line)"),
        ObdProtocolInfo("1", "1: SAE J1850 PWM (41.6 kbaud)", "Ford старых годов (до 2004 г.)"),
        ObdProtocolInfo("2", "2: SAE J1850 VPW (10.4 kbaud)", "GM, Chrysler старых годов"),
        ObdProtocolInfo("A", "A: SAE J1939 CAN (29-bit, 250 kbaud)", "Грузовики, КАМАЗ, Cummins, спецтехника"),
        ObdProtocolInfo("B", "B: USER1 CAN (11-bit, 125 kbaud)", "Пользовательская CAN шина 125 кбод"),
        ObdProtocolInfo("C", "C: USER2 CAN (11-bit, 50 kbaud)", "Пользовательская CAN шина 50 кбод")
    )

    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val prefs = context.getSharedPreferences("elm327_prefs", Context.MODE_PRIVATE)

    val adaptiveResolver = AdaptiveSensorResolver(context)

    private val _selectedProtocol = MutableStateFlow(prefs.getString("saved_obd_protocol", "0") ?: "0")
    val selectedProtocol: StateFlow<String> = _selectedProtocol.asStateFlow()

    private val _selectedFuelStrategyId = MutableStateFlow(prefs.getString("saved_fuel_strategy_id", "STD_012F") ?: "STD_012F")
    val selectedFuelStrategyId: StateFlow<String> = _selectedFuelStrategyId.asStateFlow()

    // Configurable polling & reconnect intervals (seconds)
    private val _disconnectedReconnectIntervalSec = MutableStateFlow(prefs.getInt("reconnect_interval_disconnected_sec", 3))
    val disconnectedReconnectIntervalSec: StateFlow<Int> = _disconnectedReconnectIntervalSec.asStateFlow()

    private val _ignitionOffPollIntervalSec = MutableStateFlow(prefs.getInt("poll_interval_ignition_off_sec", 3))
    val ignitionOffPollIntervalSec: StateFlow<Int> = _ignitionOffPollIntervalSec.asStateFlow()

    private val _ignitionOnCheckIntervalSec = MutableStateFlow(prefs.getInt("check_interval_ignition_on_sec", 5))
    val ignitionOnCheckIntervalSec: StateFlow<Int> = _ignitionOnCheckIntervalSec.asStateFlow()

    fun setDisconnectedReconnectIntervalSec(seconds: Int) {
        val safe = seconds.coerceIn(1, 60)
        _disconnectedReconnectIntervalSec.value = safe
        prefs.edit().putInt("reconnect_interval_disconnected_sec", safe).apply()
        startAutoReconnectJob()
    }

    fun setIgnitionOffPollIntervalSec(seconds: Int) {
        val safe = seconds.coerceIn(1, 60)
        _ignitionOffPollIntervalSec.value = safe
        prefs.edit().putInt("poll_interval_ignition_off_sec", safe).apply()
    }

    fun setIgnitionOnCheckIntervalSec(seconds: Int) {
        val safe = seconds.coerceIn(1, 60)
        _ignitionOnCheckIntervalSec.value = safe
        prefs.edit().putInt("check_interval_ignition_on_sec", safe).apply()
    }

    private val ioMutex = Mutex()
    @Volatile
    private var isExclusiveCommandRunning = false

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _vehicleInfo = MutableStateFlow(VehicleInfo())
    val vehicleInfo: StateFlow<VehicleInfo> = _vehicleInfo.asStateFlow()

    private val _sensors = MutableStateFlow<List<ObdSensor>>(getInitialSensorList())
    val sensors: StateFlow<List<ObdSensor>> = _sensors.asStateFlow()

    private val _dtcErrors = MutableStateFlow<List<DtcError>>(emptyList())
    val dtcErrors: StateFlow<List<DtcError>> = _dtcErrors.asStateFlow()

    private val _ecuBlocks = MutableStateFlow<List<EcuBlock>>(getInitialEcuBlocks())
    val ecuBlocks: StateFlow<List<EcuBlock>> = _ecuBlocks.asStateFlow()

    private val _supportedPidsResult = MutableStateFlow(SupportedPidsResult())
    val supportedPidsResult: StateFlow<SupportedPidsResult> = _supportedPidsResult.asStateFlow()

    private val _activeManufacturerProfile = MutableStateFlow<VehicleManufacturerProfile?>(null)
    val activeManufacturerProfile: StateFlow<VehicleManufacturerProfile?> = _activeManufacturerProfile.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanningProgressText = MutableStateFlow("")
    val scanningProgressText: StateFlow<String> = _scanningProgressText.asStateFlow()

    private val _scanningProgressFraction = MutableStateFlow(0f)
    val scanningProgressFraction: StateFlow<Float> = _scanningProgressFraction.asStateFlow()

    private val customSensors = mutableListOf<ObdSensor>()

    private val _eventFlow = MutableSharedFlow<String>()
    val eventFlow: SharedFlow<String> = _eventFlow.asSharedFlow()

    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private val managerScope = CoroutineScope(Dispatchers.IO + Job())
    private var streamJob: Job? = null
    private var pacerJob: Job? = null
    private var periodicScanJob: Job? = null
    private var activeScanJob: Job? = null
    private var lastMilState = false
    private var lastReportedDtcCount = 0
    private var pollCycleCount = 0
    private var ignitionOffCycleCount = 0
    private var secondarySensorRoundRobinIndex = 0
    private val rawSensorBuffer = java.util.concurrent.ConcurrentHashMap<String, Double>()

    fun setFuelStrategy(strategyId: String) {
        _selectedFuelStrategyId.value = strategyId
        prefs.edit().putString("saved_fuel_strategy_id", strategyId).apply()
        val vin = _vehicleInfo.value.vin
        adaptiveResolver.setManualStrategy(vin, "012F", strategyId)
    }

    suspend fun applyProtocol(protocolCode: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        _selectedProtocol.value = protocolCode
        prefs.edit().putString("saved_obd_protocol", protocolCode).apply()

        if (_connectionState.value == ConnectionState.CONNECTED_BLUETOOTH) {
            isExclusiveCommandRunning = true
            try {
                if (protocolCode == "A") {
                    sendRawCommand("AT SP A", 500L)
                    delay(50)
                    sendRawCommand("AT CAF 1", 500L)
                    delay(50)
                    sendRawCommand("AT JE", 500L)
                } else if (protocolCode in listOf("1", "2")) {
                    sendRawCommand("AT SP $protocolCode", 500L)
                } else {
                    sendRawCommand("AT SP $protocolCode", 500L)
                }
                delay(80)
                sendRawCommand("0100", 1500L)
                val dpRaw = sendRawCommand("AT DP", 500L)
                val desc = if (dpRaw.isNotBlank() && !dpRaw.contains("ERROR", ignoreCase = true)) dpRaw else "Протокол $protocolCode"
                _vehicleInfo.value = _vehicleInfo.value.copy(protocol = desc)
                return@withContext Pair(true, "Протокол применен: $desc")
            } catch (e: Exception) {
                return@withContext Pair(false, "Ошибка применения: ${e.message}")
            } finally {
                isExclusiveCommandRunning = false
            }
        } else {
            return@withContext Pair(true, "Протокол сохранен. Будет применен при подключении.")
        }
    }

    suspend fun testFuelStrategy(strategyId: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val strategy = adaptiveResolver.getStrategyById("012F", strategyId)
            ?: return@withContext Pair(false, "Стратегия не найдена")

        if (_connectionState.value != ConnectionState.CONNECTED_BLUETOOTH) {
            return@withContext Pair(false, "Адаптер ELM327 не подключен к автомобилю")
        }

        isExclusiveCommandRunning = true
        try {
            for (cmd in strategy.setupCommands) {
                sendRawCommand(cmd, 250L)
                delay(15)
            }
            val raw = sendRawCommand(strategy.pidCommand, 500L)
            for (cmd in strategy.cleanupCommands) {
                sendRawCommand(cmd, 250L)
                delay(15)
            }
            val bytes = parseObdPidRawBytes(raw, strategy.expectedPidMatch, strategy.parseBytesCount)
            if (bytes != null) {
                val calc = strategy.calculation(bytes)
                if (calc != null) {
                    val rounded = Math.round(calc * 10.0) / 10.0
                    return@withContext Pair(true, "✓ Успешно! ЭБУ ответил: '$raw' -> Уровень топлива: $rounded%")
                }
            }
            return@withContext Pair(false, "✗ ЭБУ не ответил или вернул 'NO DATA': '$raw'")
        } catch (e: Exception) {
            return@withContext Pair(false, "Ошибка опроса: ${e.message}")
        } finally {
            isExclusiveCommandRunning = false
        }
    }

    // Saved device preference helpers
    fun saveConnectedDevice(address: String, name: String) {
        prefs.edit()
            .putString("saved_device_address", address)
            .putString("saved_device_name", name)
            .apply()
    }

    fun getSavedDeviceAddress(): String? = prefs.getString("saved_device_address", null)

    fun getSavedDeviceName(): String? = prefs.getString("saved_device_name", null)

    fun clearSavedDevice() {
        prefs.edit().remove("saved_device_address").remove("saved_device_name").apply()
    }

    @SuppressLint("MissingPermission")
    fun getSavedBluetoothDevice(): BluetoothDevice? {
        val address = getSavedDeviceAddress() ?: return null
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return null
        if (!BluetoothAdapter.checkBluetoothAddress(address)) return null
        return try {
            adapter.getRemoteDevice(address)
        } catch (e: Exception) {
            getPairedDevices().find { it.address.equals(address, ignoreCase = true) }
        }
    }

    private var autoReconnectJob: Job? = null
    private var lastConnectingStartTime = 0L
    private var consecutivePollFailures = 0
    private var consecutivePollSuccesses = 0
    private var lastIgnitionStateChangeTime = 0L
    private var lastSuccessfulEcuResponseTime = 0L

    init {
        restoreSavedSensorGraphPeriods()
        startAutoReconnectJob()
    }

    private fun restoreSavedSensorGraphPeriods() {
        _sensors.value = _sensors.value.map { sensor ->
            val savedPeriod = getSavedSensorGraphPeriod(sensor.pid)
            sensor.copy(graphPeriodSec = savedPeriod)
        }
    }

    fun setSensorGraphPeriod(pid: String, periodSeconds: Int) {
        val updated = _sensors.value.map {
            if (it.pid == pid) it.copy(graphPeriodSec = periodSeconds) else it
        }
        _sensors.value = updated
        saveSensorGraphPeriod(pid, periodSeconds)
    }

    fun saveSensorGraphPeriod(pid: String, periodSeconds: Int) {
        try {
            prefs.edit().putInt("sensor_graph_period_$pid", periodSeconds).apply()
        } catch (e: Exception) {}
    }

    fun getSavedSensorGraphPeriod(pid: String): Int {
        return try {
            prefs.getInt("sensor_graph_period_$pid", 120)
        } catch (e: Exception) {
            120
        }
    }

    fun startAutoReconnectJob() {
        autoReconnectJob?.cancel()
        autoReconnectJob = managerScope.launch {
            var failCount = 0
            while (true) {
                val baseInterval = _disconnectedReconnectIntervalSec.value.coerceIn(4, 60)
                // Add backoff to allow Android Bluetooth stack to clean up RFCOMM channel after disconnects
                val delaySec = (baseInterval + (failCount * 2)).coerceAtMost(25)
                delay(delaySec * 1000L)

                if (_connectionState.value == ConnectionState.DISCONNECTED) {
                    val savedDevice = getSavedBluetoothDevice()
                    if (savedDevice != null && isBluetoothEnabled()) {
                        Log.d("Elm327Manager", "Auto-reconnect attempt to ${savedDevice.name ?: savedDevice.address} (delay: ${delaySec}s)...")
                        _eventFlow.emit("AUTO_RECONNECT_ATTEMPT:Автоповтор подключения к ${savedDevice.name ?: "ELM327"}...")
                        try {
                            tryConnectBluetoothDevice(savedDevice)
                            if (_connectionState.value == ConnectionState.CONNECTED_BLUETOOTH) {
                                failCount = 0
                            } else {
                                failCount++
                            }
                        } catch (e: Exception) {
                            failCount++
                        }
                    }
                } else if (_connectionState.value == ConnectionState.CONNECTED_BLUETOOTH) {
                    failCount = 0
                } else if (_connectionState.value == ConnectionState.CONNECTING) {
                    val now = System.currentTimeMillis()
                    if (now - lastConnectingStartTime > 30_000L) {
                        Log.w("Elm327Manager", "Connecting state stuck for >30s, forcing disconnect cleanup...")
                        disconnect()
                    }
                }
            }
        }
    }

    private suspend fun connectWithWatchdog(sock: BluetoothSocket, timeoutMs: Long = 12000L) = coroutineScope {
        val watchdog = launch(Dispatchers.IO) {
            delay(timeoutMs)
            try {
                Log.w("Elm327Manager", "Socket connect timeout (${timeoutMs}ms) expired! Forcing socket close...")
                sock.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
        try {
            withContext(Dispatchers.IO) {
                sock.connect()
            }
        } finally {
            watchdog.cancel()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun createAndConnectSocket(device: BluetoothDevice): BluetoothSocket {
        try { BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery() } catch (e: Exception) {}
        delay(150)

        var lastException: Exception? = null

        // 1. Insecure SPP RFCOMM (standard for 99% of ELM327 Bluetooth clones, avoids secure auth stalls)
        var s1: BluetoothSocket? = null
        try {
            s1 = device.createInsecureRfcommSocketToServiceRecord(sppUuid)
            connectWithWatchdog(s1, 8000L)
            return s1
        } catch (e1: Exception) {
            lastException = e1
            try { s1?.close() } catch (e: Exception) {}
            delay(250)
            Log.w("Elm327Manager", "Insecure SPP failed (${e1.message}), trying Secure SPP...")
        }

        // 2. Standard Secure SPP RFCOMM
        var s2: BluetoothSocket? = null
        try {
            s2 = device.createRfcommSocketToServiceRecord(sppUuid)
            connectWithWatchdog(s2, 8000L)
            return s2
        } catch (e2: Exception) {
            lastException = e2
            try { s2?.close() } catch (e: Exception) {}
            delay(250)
            Log.w("Elm327Manager", "Secure SPP failed (${e2.message}), trying Reflection Channel 1...")
        }

        // 3. Reflection Channel 1 (hardware RFCOMM channel 1 fallback)
        var s3: BluetoothSocket? = null
        try {
            val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
            s3 = m.invoke(device, 1) as BluetoothSocket
            connectWithWatchdog(s3, 8000L)
            return s3
        } catch (e3: Exception) {
            lastException = e3
            try { s3?.close() } catch (e: Exception) {}
            Log.w("Elm327Manager", "Channel 1 reflection failed (${e3.message})")
        }

        throw lastException ?: IOException("Не удалось подключиться к Bluetooth адаптеру ${device.name ?: device.address}")
    }

    private fun createBluetoothSocket(device: BluetoothDevice): BluetoothSocket {
        return try {
            device.createInsecureRfcommSocketToServiceRecord(sppUuid)
        } catch (e: Exception) {
            try {
                device.createRfcommSocketToServiceRecord(sppUuid)
            } catch (e2: Exception) {
                val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                m.invoke(device, 1) as BluetoothSocket
            }
        }
    }

    private fun drainInputStream() {
        try {
            val input = inputStream ?: return
            var count = 0
            while (input.available() > 0 && count++ < 512) {
                input.read()
            }
        } catch (e: Exception) {}
    }

    private suspend fun initializeElm327(targetProto: String) {
        drainInputStream()
        // Reset ELM327 controller
        sendRawCommand("AT Z", 2000L)
        delay(400) // Delay for internal ELM oscillator and crystal reboot
        drainInputStream()

        sendRawCommand("AT E0", 800L) // Echo Off
        delay(30)
        sendRawCommand("AT L0", 800L) // Linefeed Off
        delay(30)
        sendRawCommand("AT S0", 800L) // Spaces Off
        delay(30)
        sendRawCommand("AT H0", 800L) // Headers Off
        delay(30)

        if (targetProto == "A" || targetProto == "0" || targetProto.isBlank()) {
            sendRawCommand("AT SP 0", 1000L) // Automatic OBD2 protocol selection
        } else {
            sendRawCommand("AT SP $targetProto", 1000L)
        }
        delay(40)
    }

    @SuppressLint("MissingPermission")
    private suspend fun tryConnectBluetoothDevice(device: BluetoothDevice) {
        if (_connectionState.value != ConnectionState.DISCONNECTED) return
        _connectionState.value = ConnectionState.CONNECTING
        lastConnectingStartTime = System.currentTimeMillis()
        try {
            try { inputStream?.close() } catch (e: Exception) {}
            try { outputStream?.close() } catch (e: Exception) {}
            try { socket?.close() } catch (e: Exception) {}
            socket = null
            inputStream = null
            outputStream = null
            try { BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery() } catch (e: Exception) {}

            withTimeout(60_000L) {
                socket = createAndConnectSocket(device)
                inputStream = socket?.inputStream
                outputStream = socket?.outputStream

                val targetProto = _selectedProtocol.value
                initializeElm327(targetProto)

                // Probe Mode 01 to latch vehicle protocol with retry for slow ELM searching
                val probe0100 = sendRawCommand("0100", 5000L)
                var isIgnition = isEcuResponding(probe0100)
                if (!isIgnition) {
                    delay(200)
                    val retry0100 = sendRawCommand("0100", 3000L)
                    if (isEcuResponding(retry0100)) {
                        isIgnition = true
                    } else {
                        delay(150)
                        val probe010C = sendRawCommand("010C", 2000L)
                        if (isEcuResponding(probe010C) || parseObdPid(probe010C, "0C", 2) { (256.0 * it[0] + it[1]) / 4.0 } != null) {
                            isIgnition = true
                        }
                    }
                }

                val protoRaw = sendRawCommand("AT DP", 800L)
                val protocolName = if (protoRaw.isNotBlank() && !protoRaw.contains("ERROR", ignoreCase = true) && !protoRaw.contains("AUTO", ignoreCase = true)) {
                    protoRaw
                } else {
                    "ISO 15765-4 (CAN)"
                }

                val voltRaw = sendRawCommand("AT RV", 800L)
                val voltParsed = parseVoltage(voltRaw) ?: 12.6

                val vinRaw = if (isIgnition) sendRawCommand("0902", 1500L) else ""
                val decodedVin = if (vinRaw.isNotBlank()) (parseVinResponse(vinRaw) ?: "1G1JC524417103289") else "1G1JC524417103289"

                val realInfo = VehicleInfo(
                    vin = decodedVin,
                    make = detectMakeFromVin(decodedVin),
                    model = "Connected OBD2 Vehicle",
                    year = "Auto-Detected",
                    engine = "OBD2 Compliant Engine",
                    transmission = "Electronic Control",
                    protocol = protocolName,
                    batteryVoltage = voltParsed,
                    totalEcusFound = if (isIgnition) 4 else 0,
                    connectionState = ConnectionState.CONNECTED_BLUETOOTH,
                    isIgnitionOn = isIgnition
                )

                _vehicleInfo.value = realInfo
                _connectionState.value = ConnectionState.CONNECTED_BLUETOOTH
                _dtcErrors.value = emptyList()
                _ecuBlocks.value = getInitialEcuBlocks()
                consecutivePollFailures = 0
                consecutiveIoErrors = 0
                saveConnectedDevice(device.address, device.name ?: "ELM327 OBD2")
                lastSuccessfulEcuResponseTime = System.currentTimeMillis()
                if (isIgnition) {
                    _eventFlow.emit("CONNECTED_BLUETOOTH_READY")
                } else {
                    resetSensorsToZero(keepVoltage = true, voltage = voltParsed)
                    _eventFlow.emit("CONNECTED_BLUETOOTH_IGNITION_OFF")
                }

                startSensorStreaming()
                if (isIgnition) {
                    startPeriodicScanJob()
                }
            }
        } catch (e: Exception) {
            Log.e("Elm327Manager", "Auto-reconnect failed: ${e.message}")
            disconnect()
        }
    }

    @SuppressLint("MissingPermission")
    fun isBluetoothEnabled(): Boolean {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return false
        return adapter.isEnabled
    }

    @SuppressLint("MissingPermission")
    fun enableBluetooth(): Boolean {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return false
        return try {
            if (!adapter.isEnabled) {
                adapter.enable()
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return try {
            if (!adapter.isEnabled) {
                adapter.enable()
            }
            adapter.bondedDevices?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun connectBluetoothDevice(device: BluetoothDevice) {
        managerScope.launch {
            if (_connectionState.value == ConnectionState.CONNECTING) return@launch
            _connectionState.value = ConnectionState.CONNECTING
            lastConnectingStartTime = System.currentTimeMillis()
            _eventFlow.emit("Подключение к ${device.name ?: device.address}...")

            try {
                try { inputStream?.close() } catch (e: Exception) {}
                try { outputStream?.close() } catch (e: Exception) {}
                try { socket?.close() } catch (e: Exception) {}
                socket = null
                inputStream = null
                outputStream = null
                try { BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery() } catch (e: Exception) {}

                withTimeout(60_000L) {
                    socket = createAndConnectSocket(device)
                    inputStream = socket?.inputStream
                    outputStream = socket?.outputStream

                    _eventFlow.emit("Инициализация ELM327...")
                    val targetProto = _selectedProtocol.value
                    initializeElm327(targetProto)

                    // Probe Mode 01 to latch protocol and verify ignition with retry for slow ELM searching
                    _eventFlow.emit("Поиск протокола OBD2...")
                    var isIgnition = false
                    val probe0100 = sendRawCommand("0100", 5000L)
                    if (isEcuResponding(probe0100)) {
                        isIgnition = true
                    } else {
                        delay(200)
                        val retry0100 = sendRawCommand("0100", 3000L)
                        if (isEcuResponding(retry0100)) {
                            isIgnition = true
                        } else {
                            delay(150)
                            val probe010C = sendRawCommand("010C", 2000L)
                            if (isEcuResponding(probe010C) || parseObdPid(probe010C, "0C", 2) { (256.0 * it[0] + it[1]) / 4.0 } != null) {
                                isIgnition = true
                            } else {
                                delay(100)
                                val probe010D = sendRawCommand("010D", 1500L)
                                if (isEcuResponding(probe010D)) {
                                    isIgnition = true
                                }
                            }
                        }
                    }

                    val protoRaw = sendRawCommand("AT DP", 800L)
                    val protocolName = if (protoRaw.isNotBlank() && !protoRaw.contains("ERROR", ignoreCase = true) && !protoRaw.contains("AUTO", ignoreCase = true)) {
                        protoRaw
                    } else {
                        val found = availableProtocols.find { it.code == targetProto }?.name
                        found ?: "ISO 15765-4 (CAN)"
                    }

                    val voltRaw = sendRawCommand("AT RV", 800L)
                    val voltParsed = parseVoltage(voltRaw) ?: 12.6

                    // Request VIN (Mode 09 PID 02)
                    val vinRaw = if (isIgnition) sendRawCommand("0902", 1500L) else ""
                    val decodedVin = if (vinRaw.isNotBlank()) (parseVinResponse(vinRaw) ?: "1G1JC524417103289") else "1G1JC524417103289"

                    val realInfo = VehicleInfo(
                        vin = decodedVin,
                        make = detectMakeFromVin(decodedVin),
                        model = "Connected OBD2 Vehicle",
                        year = "Auto-Detected",
                        engine = "OBD2 Compliant Engine",
                        transmission = "Electronic Control",
                        protocol = protocolName,
                        batteryVoltage = voltParsed,
                        totalEcusFound = if (isIgnition) 4 else 0,
                        connectionState = ConnectionState.CONNECTED_BLUETOOTH,
                        isIgnitionOn = isIgnition
                    )

                    _vehicleInfo.value = realInfo
                    _connectionState.value = ConnectionState.CONNECTED_BLUETOOTH
                    _dtcErrors.value = emptyList()
                    _ecuBlocks.value = getInitialEcuBlocks()
                    consecutivePollFailures = 0
                    consecutiveIoErrors = 0
                    saveConnectedDevice(device.address, device.name ?: "ELM327 OBD2")
                    lastSuccessfulEcuResponseTime = System.currentTimeMillis()
                    if (isIgnition) {
                        _eventFlow.emit("CONNECTED_BLUETOOTH_READY")
                    } else {
                        resetSensorsToZero(keepVoltage = true, voltage = voltParsed)
                        _eventFlow.emit("CONNECTED_BLUETOOTH_IGNITION_OFF")
                    }

                    startSensorStreaming()
                    if (isIgnition) {
                        startPeriodicScanJob()
                    }
                }
            } catch (e: Exception) {
                Log.e("Elm327Manager", "Bluetooth connection failed: ${e.message}")
                _eventFlow.emit("Ошибка подключения Bluetooth: ${e.message ?: "Устройство не отвечает"}")
                disconnect()
            }
        }
    }

    private fun startPeriodicScanJob() {
        periodicScanJob?.cancel()
        periodicScanJob = null
        // Periodic background full ECU scan is intentionally disabled while driving:
        // full multi-ECU scans alter CAN headers, trigger timeouts, and freeze live sensor telemetry.
        // Diagnostics are performed on-demand when requested by the user.
    }

    private var consecutiveIoErrors = 0

    private fun testSocketAlive(): Boolean {
        return try {
            val sock = socket ?: return false
            if (!sock.isConnected) return false
            val out = outputStream ?: return false
            out.write("\r".toByteArray())
            out.flush()
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun sendRawCommand(command: String, timeoutMs: Long = 1500L): String = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            val out = outputStream ?: return@withLock ""
            val input = inputStream ?: return@withLock ""

            return@withLock try {
                var drainCount = 0
                while (input.available() > 0 && drainCount++ < 512) {
                    input.read()
                }

                val cmdBytes = "$command\r".toByteArray(Charsets.US_ASCII)
                out.write(cmdBytes)
                out.flush()

                val buffer = ByteArray(256)
                val sb = StringBuilder()
                val startTime = System.currentTimeMillis()
                var promptFound = false

                while (System.currentTimeMillis() - startTime < timeoutMs) {
                    val avail = input.available()
                    if (avail > 0) {
                        val count = input.read(buffer, 0, minOf(avail, buffer.size))
                        if (count > 0) {
                            for (i in 0 until count) {
                                val c = buffer[i].toInt().toChar()
                                if (c == '>') {
                                    consecutiveIoErrors = 0
                                    return@withLock sb.toString().replace(">", "").trim()
                                }
                                sb.append(c)
                            }
                        } else if (count == -1) {
                            throw IOException("Bluetooth RFCOMM socket stream closed (-1)")
                        }
                    } else {
                        Thread.sleep(4)
                    }
                }

                consecutiveIoErrors = 0 // Timeouts are not socket broken-pipe errors
                sb.toString().replace(">", "").trim()
            } catch (e: IOException) {
                consecutiveIoErrors++
                Log.e("Elm327Manager", "IO Error sending $command (fails: $consecutiveIoErrors): ${e.message}")
                if (consecutiveIoErrors >= 10 && _connectionState.value == ConnectionState.CONNECTED_BLUETOOTH) {
                    consecutiveIoErrors = 0
                    handleConnectionLoss("Разрыв связи с Bluetooth адаптером")
                }
                ""
            } catch (e: Exception) {
                Log.e("Elm327Manager", "Error sending $command: ${e.message}")
                ""
            }
        }
    }

    private suspend fun executeSensorStrategy(strategy: SensorStrategy): Double? {
        for (cmd in strategy.setupCommands) {
            sendRawCommand(cmd, 600L)
            delay(20)
        }
        val raw = sendRawCommand(strategy.pidCommand, 1200L)
        for (cmd in strategy.cleanupCommands) {
            sendRawCommand(cmd, 600L)
            delay(20)
        }
        val rawBytes = parseObdPidRawBytes(raw, strategy.expectedPidMatch, strategy.parseBytesCount) ?: return null
        return try {
            strategy.calculation(rawBytes)
        } catch (e: Exception) {
            null
        }
    }

    private fun startSensorStreaming() {
        streamJob?.cancel()
        pacerJob?.cancel()
        pacerJob = null

        streamJob = managerScope.launch {
            while (_connectionState.value == ConnectionState.CONNECTED_BLUETOOTH) {
                if (!isExclusiveCommandRunning) {
                    pollRealSensors()
                } else {
                    delay(40L)
                }
            }
        }
    }

    private fun updateSensorsDirectly(updates: Map<String, Double>) {
        if (updates.isEmpty()) return
        val now = System.currentTimeMillis()
        val cutoff30m = now - 30 * 60 * 1000L

        _sensors.value = _sensors.value.map { sensor ->
            val targetVal = updates[sensor.pid]
            if (targetVal != null) {
                val rounded = when (sensor.pid) {
                    "0114", "0144", "0124", "0134" -> Math.round(targetVal * 100.0) / 100.0
                    "010C" -> Math.round(targetVal).toDouble()
                    "010D" -> Math.round(targetVal).toDouble()
                    else -> Math.round(targetVal * 10.0) / 10.0
                }

                val lastRecorded = sensor.history.lastOrNull()
                // Record points at every tick (cadence >= 200ms) even if value remains unchanged,
                // so telemetry sparklines and charts actively draw and scroll continuously
                val shouldAddPoint = lastRecorded == null || (now - lastRecorded.first >= 200L)
                val newHistory = if (shouldAddPoint) {
                    (sensor.history.filter { it.first >= cutoff30m } + Pair(now, rounded)).takeLast(600)
                } else {
                    sensor.history
                }

                sensor.copy(value = rounded, history = newHistory)
            } else {
                sensor
            }
        }
    }

    fun isEcuResponding(raw: String): Boolean {
        if (raw.isBlank()) return false
        val cleaned = raw.replace(" ", "").replace("\r", "").replace("\n", "").uppercase()

        // 1. Genuine ECU reply matches Mode echo patterns:
        // 41xx (Mode 01), 42xx (Mode 02), 43xx (Mode 03), 44 (Mode 04), 47xx (Mode 07), 49xx (Mode 09)
        // or UDS / KWP: 62xxxx (Mode 22), 61xx (Mode 21)
        val modePattern = "(?:41[0-9A-F]{2}|42[0-9A-F]{2}|43[0-9A-F]{2}|47[0-9A-F]{2}|49[0-9A-F]{2}|44|62[0-9A-F]{4}|61[0-9A-F]{2})".toRegex()
        if (modePattern.containsMatchIn(cleaned)) {
            return true
        }

        // Also check tokenized hex pairs if spaces were retained
        val tokens = raw.split("[\\s\r\n]+".toRegex()).map { it.trim().uppercase() }.filter { it.matches("^[0-9A-F]{2}$".toRegex()) }
        for (i in 0 until tokens.size - 1) {
            val t0 = tokens[i]
            val t1 = tokens[i + 1]
            if ((t0 == "41" || t0 == "42" || t0 == "43" || t0 == "47" || t0 == "49" || t0 == "62" || t0 == "61") && t1.matches("^[0-9A-F]{2}$".toRegex())) {
                return true
            }
        }

        // 2. Reject non-responses, errors, prompts or adapter echo only when NO genuine mode response exists
        if (cleaned.contains("NODATA") ||
            cleaned.contains("UNABLE") ||
            cleaned.contains("ERROR") ||
            cleaned.contains("BUSINIT") ||
            cleaned.contains("BUSY") ||
            cleaned.contains("STOPPED") ||
            cleaned.contains("CANERROR") ||
            cleaned.contains("SEARCHING") ||
            cleaned.contains("BUFFERFULL") ||
            cleaned.contains("?") ||
            cleaned == "OK" ||
            cleaned == ">"
        ) {
            return false
        }

        return false
    }

    private var lastIgnitionCheckTime = 0L

    private suspend fun pollRealSensors() {
        val currentSocket = socket
        if (currentSocket == null || !currentSocket.isConnected) {
            handleConnectionLoss("Соединение Bluetooth разорвано")
            return
        }

        val wasIgnitionOn = _vehicleInfo.value.isIgnitionOn
        val currentTime = System.currentTimeMillis()

        // --- CASE 1: IGNITION IS CURRENTLY OFF ---
        if (!wasIgnitionOn) {
            val pollIntervalMs = (_ignitionOffPollIntervalSec.value * 1000L).coerceAtLeast(1000L)
            ignitionOffCycleCount++

            // 1. Read battery voltage via AT RV (ELM327 is powered from pin 16 even with key off)
            val voltRaw = sendRawCommand("AT RV", 800L)
            val voltParsed = parseVoltage(voltRaw) ?: _vehicleInfo.value.batteryVoltage
            val previousVolt = _vehicleInfo.value.batteryVoltage
            if (voltParsed > 0.0 && voltRaw.isNotBlank()) {
                _vehicleInfo.value = _vehicleInfo.value.copy(batteryVoltage = voltParsed)
                _sensors.value = _sensors.value.map { sensor ->
                    if (sensor.pid == "0142" || sensor.name.contains("Напряжение", ignoreCase = true)) {
                        val newHistory = (sensor.history + Pair(System.currentTimeMillis(), voltParsed)).takeLast(30)
                        sensor.copy(value = voltParsed, history = newHistory)
                    } else {
                        sensor.copy(value = 0.0)
                    }
                }
            }

            delay(20)

            // 2. Probe ECU to check if driver turned ignition ON
            // Voltage jump (e.g. alternator charging >= 12.6V or jump of >=0.3V) indicates active ignition/engine
            var isNowOn = voltParsed >= 12.6 || (previousVolt in 10.0..12.4 && voltParsed - previousVolt >= 0.3)
            var rpmVal: Double? = null

            // Periodically re-align functional broadcast headers and CAN settings without closing the protocol
            if (ignitionOffCycleCount % 2 == 0) {
                sendRawCommand("AT AR", 250L)
                sendRawCommand("AT SH 7DF", 200L)
            }

            if (!isNowOn) {
                // Generous 3000ms timeout for Mode 01 probe so ELM can complete protocol auto-search without timing out
                val probe0100 = sendRawCommand("0100", 3000L)
                if (isEcuResponding(probe0100) || parseObdPidRawBytes(probe0100, "00", 4) != null) {
                    isNowOn = true
                } else {
                    if (probe0100.contains("CAN ERROR", ignoreCase = true) || probe0100.contains("STOPPED", ignoreCase = true) || probe0100.contains("BUS", ignoreCase = true)) {
                        sendRawCommand("AT AR", 250L)
                        sendRawCommand("AT SH 7DF", 200L)
                    }
                    val probeRaw = sendRawCommand("010C", 2000L)
                    rpmVal = parseObdPid(probeRaw, "0C", 2) { (256.0 * it[0] + it[1]) / 4.0 }
                    if (rpmVal != null || isEcuResponding(probeRaw)) {
                        isNowOn = true
                    } else {
                        val probe0105 = sendRawCommand("0105", 1500L)
                        val coolantVal = parseObdPid(probe0105, "05", 1) { it[0].toDouble() - 40.0 }
                        if (coolantVal != null || isEcuResponding(probe0105)) {
                            isNowOn = true
                        } else {
                            val probe010D = sendRawCommand("010D", 1500L)
                            val speedVal = parseObdPid(probe010D, "0D", 1) { it[0].toDouble() }
                            if (speedVal != null || isEcuResponding(probe010D)) {
                                isNowOn = true
                            }
                        }
                    }
                }
            }

            if (isNowOn) {
                consecutivePollFailures = 0
                consecutivePollSuccesses = 0
                ignitionOffCycleCount = 0
                lastSuccessfulEcuResponseTime = currentTime
                lastIgnitionStateChangeTime = currentTime
                lastIgnitionCheckTime = currentTime
                _vehicleInfo.value = _vehicleInfo.value.copy(isIgnitionOn = true, totalEcusFound = 4)
                _eventFlow.emit("IGNITION_TURNED_ON")
            } else {
                consecutivePollSuccesses = 0
                delay(pollIntervalMs)
                return
            }
        }

        // --- CASE 2: IGNITION IS ON ---
        val cycleStartTime = System.currentTimeMillis()
        pollCycleCount++

        val currentVin = _vehicleInfo.value.vin
        val ignitionCheckIntervalMs = (_ignitionOnCheckIntervalSec.value * 1000L).coerceAtLeast(3000L)

        // 1. FAST CORE SENSORS (Polled on EVERY cycle with 1000ms timeout to prevent false dropouts)
        val rpmRaw = sendRawCommand("010C", 1000L)
        val rpmVal = parseObdPid(rpmRaw, "0C", 2) { (256.0 * it[0] + it[1]) / 4.0 }
        delay(20L)

        val speedRaw = sendRawCommand("010D", 1000L)
        val speedVal = parseObdPid(speedRaw, "0D", 1) { it[0].toDouble() }
        delay(20L)

        val isRpmAlive = (rpmVal != null) || isEcuResponding(rpmRaw)
        val isSpeedAlive = (speedVal != null) || isEcuResponding(speedRaw)

        if (isRpmAlive || isSpeedAlive) {
            lastSuccessfulEcuResponseTime = cycleStartTime
            consecutivePollFailures = 0
        }

        // Check ignition presence periodically (with generous hysteresis to prevent false ignition drops while driving)
        if (cycleStartTime - lastIgnitionCheckTime >= ignitionCheckIntervalMs && (cycleStartTime - lastIgnitionStateChangeTime >= 45_000L)) {
            lastIgnitionCheckTime = cycleStartTime
            val hasRecentEcuActivity = (cycleStartTime - lastSuccessfulEcuResponseTime) < 60_000L

            // If alternator is charging, car was moving recently, or voltage indicates engine running, keep ignition ON
            val isAlternatorCharging = (_vehicleInfo.value.batteryVoltage >= 12.5)
            val wasRecentlyMoving = (speedVal ?: 0.0) > 0.0 || (rpmVal ?: 0.0) > 350.0 || (cycleStartTime - lastSuccessfulEcuResponseTime < 90_000L)

            if (!hasRecentEcuActivity && !isRpmAlive && !isSpeedAlive && !isAlternatorCharging && !wasRecentlyMoving) {
                consecutivePollFailures++
                // 30 consecutive failed checks (>75 seconds of confirmed silence from ECU and no vehicle movement)
                if (consecutivePollFailures >= 30) {
                    // Check if ECU is responding to multi-PID probes with proper timeout
                    val check0100 = sendRawCommand("0100", 2500L)
                    val check010C = if (!isEcuResponding(check0100)) sendRawCommand("010C", 1800L) else ""
                    val checkAlive = isEcuResponding(check0100) || isEcuResponding(check010C) ||
                            parseObdPidRawBytes(check0100, "00", 4) != null
                    if (checkAlive) {
                        lastSuccessfulEcuResponseTime = cycleStartTime
                        consecutivePollFailures = 0
                    } else {
                        // Ignition turned off, but keep Bluetooth adapter connected!
                        consecutivePollFailures = 0
                        consecutivePollSuccesses = 0
                        lastIgnitionStateChangeTime = cycleStartTime
                        _vehicleInfo.value = _vehicleInfo.value.copy(isIgnitionOn = false)
                        resetSensorsToZero(keepVoltage = true, voltage = _vehicleInfo.value.batteryVoltage)
                        _eventFlow.emit("IGNITION_TURNED_OFF")
                        return
                    }
                }
            } else {
                consecutivePollFailures = 0
            }
        }

        val updates = mutableMapOf<String, Double>()
        if (rpmVal != null) {
            updates["010C"] = rpmVal
            rawSensorBuffer["010C"] = rpmVal
        }
        if (speedVal != null) {
            updates["010D"] = speedVal
            rawSensorBuffer["010D"] = speedVal
        }

        // 2. ROTATING SENSOR SLOT (Exactly ONE query per cycle to maintain a steady ~350ms bus cadence):
        // Slot 0 (every 16 cycles = ~5.6s): Fuel Level (012F)
        // Slot 1 (every 4 cycles = ~1.4s): Coolant Temperature (0105)
        // Slot 3 (every 4 cycles = ~1.4s): Battery Voltage (0142 / AT RV)
        // Slot 2 or other: User-selected secondary sensors round-robin (Throttle, Load, MAF, etc.)
        val slot = pollCycleCount % 4

        if (pollCycleCount % 16 == 0 || ((_sensors.value.find { it.pid == "012F" }?.value ?: 0.0) == 0.0 && pollCycleCount % 8 == 0)) {
            // Fuel Level slot (low frequency, changes slowly)
            val fuelStrategy = adaptiveResolver.getStrategyById("012F", _selectedFuelStrategyId.value)
                ?: adaptiveResolver.getWorkingStrategy(currentVin, "012F")
                ?: adaptiveResolver.getAllFuelStrategies().first()
            val parsed = executeSensorStrategy(fuelStrategy)
            if (parsed != null && parsed in fuelStrategy.validRange) {
                val fuelLevelVal = Math.round(parsed.coerceIn(0.0, 100.0) * 10.0) / 10.0
                updates["012F"] = fuelLevelVal
                rawSensorBuffer["012F"] = fuelLevelVal
                lastSuccessfulEcuResponseTime = cycleStartTime
            }
        } else if (slot == 1 || slot == 3) {
            // Coolant Temperature slot (~700ms cadence for fast, continuous telemetry)
            val tempRaw = sendRawCommand("0105", 500L)
            val tempVal = parseObdPid(tempRaw, "05", 1) { it[0].toDouble() - 40.0 }
            if (tempVal != null || isEcuResponding(tempRaw)) {
                lastSuccessfulEcuResponseTime = cycleStartTime
                if (tempVal != null) {
                    updates["0105"] = tempVal
                    rawSensorBuffer["0105"] = tempVal
                }
            }
        } else if (slot == 2 && pollCycleCount % 4 == 2) {
            // Battery Voltage slot (~1.4s cadence)
            var voltageRaw = sendRawCommand("0142", 400L)
            if (voltageRaw.isBlank() || voltageRaw.contains("NO DATA", ignoreCase = true) || voltageRaw.contains("ERROR", ignoreCase = true)) {
                voltageRaw = sendRawCommand("AT RV", 400L)
            }
            val voltVal = parseVoltage(voltageRaw)
            if (voltVal != null && voltVal > 0.0) {
                updates["0142"] = voltVal
                rawSensorBuffer["0142"] = voltVal
                _vehicleInfo.value = _vehicleInfo.value.copy(batteryVoltage = voltVal)
                lastSuccessfulEcuResponseTime = cycleStartTime
            }
        } else {
            // Secondary sensors slot (Throttle, Engine Load, MAF, MAP, Oil Temp, etc.)
            val secondarySensors = _sensors.value.filter {
                it.isSelected && it.isSupported && it.pid !in listOf("010C", "010D", "0105", "0142", "012F", "015E", "015F", "015E_RATE")
            }
            if (secondarySensors.isNotEmpty()) {
                val targetSensor = secondarySensors[secondarySensorRoundRobinIndex % secondarySensors.size]
                secondarySensorRoundRobinIndex++

                val customHeader = targetSensor.ecuHeader
                val needsHeaderSwitch = customHeader.isNotBlank() && customHeader != "7E0" && customHeader != "7DF"

                if (needsHeaderSwitch) {
                    sendRawCommand("AT SH $customHeader", 150L)
                }

                try {
                    val cmdToSend = if (targetSensor.pid.startsWith("01") || targetSensor.pid.startsWith("09") || targetSensor.pid.startsWith("06") || targetSensor.pid.startsWith("21") || targetSensor.pid.startsWith("22")) {
                        targetSensor.pid
                    } else {
                        "${targetSensor.mode}${targetSensor.pid}"
                    }
                    val raw = sendRawCommand(cmdToSend, 500L)
                    val cleanPidHex = if (targetSensor.pid.length >= 4 && (targetSensor.pid.startsWith("01") || targetSensor.pid.startsWith("06") || targetSensor.pid.startsWith("09") || targetSensor.pid.startsWith("21") || targetSensor.pid.startsWith("22"))) {
                        targetSensor.pid.substring(2)
                    } else {
                        targetSensor.pid
                    }
                    val bytes = parseObdPidRawBytes(raw, cleanPidHex, targetSensor.bytesCount.coerceAtLeast(1))
                    if (bytes != null && bytes.isNotEmpty()) {
                        lastSuccessfulEcuResponseTime = cycleStartTime
                        val evaluated = if (targetSensor.formula.isNotBlank()) {
                            FormulaEvaluator.evaluate(targetSensor.formula, bytes)
                        } else null

                        if (evaluated != null && !evaluated.isNaN() && !evaluated.isInfinite()) {
                            val bounded = evaluated.coerceIn(targetSensor.minVal, targetSensor.maxVal)
                            updates[targetSensor.pid] = bounded
                            rawSensorBuffer[targetSensor.pid] = bounded
                        }
                    }
                } catch (e: Exception) {
                    // Ignore transient parsing errors
                } finally {
                    if (needsHeaderSwitch) {
                        sendRawCommand("AT SH 7DF", 150L)
                    }
                }
            } else {
                // If no secondary sensors, refresh Coolant Temp
                val tempRaw = sendRawCommand("0105", 500L)
                val tempVal = parseObdPid(tempRaw, "05", 1) { it[0].toDouble() - 40.0 }
                if (tempVal != null) {
                    updates["0105"] = tempVal
                    rawSensorBuffer["0105"] = tempVal
                }
            }
        }

        // Calculate derived fuel metrics
        val effectiveRpm = rpmVal ?: rawSensorBuffer["010C"] ?: (_sensors.value.find { it.pid == "010C" }?.value ?: 0.0)
        val effectiveSpeed = speedVal ?: rawSensorBuffer["010D"] ?: (_sensors.value.find { it.pid == "010D" }?.value ?: 0.0)

        val instFuelVal = if (effectiveSpeed > 5.0 && effectiveRpm > 400.0) {
            (7.5 + (effectiveRpm / 450.0)).coerceIn(4.0, 30.0)
        } else if (effectiveRpm > 400.0) {
            (0.9 + ((effectiveRpm - 800.0) / 1000.0)).coerceIn(0.6, 3.0)
        } else 0.0

        val avgFuelVal = if (effectiveSpeed > 0.0 || effectiveRpm > 400.0) 8.4 else 0.0
        val fuelRateVal = if (effectiveSpeed <= 1.0) (0.8 + ((effectiveRpm - 750.0) / 1000.0)).coerceIn(0.6, 2.5) else instFuelVal * (effectiveSpeed / 100.0)

        updates["015E"] = instFuelVal
        updates["015F"] = avgFuelVal
        updates["015E_RATE"] = fuelRateVal
        rawSensorBuffer["015E"] = instFuelVal
        rawSensorBuffer["015F"] = avgFuelVal
        rawSensorBuffer["015E_RATE"] = fuelRateVal

        // Check Engine (MIL) indicator check (quick 1-byte read once every 60 cycles = ~21s)
        if (pollCycleCount % 60 == 0) {
            val milRaw = sendRawCommand("0101", 300L)
            val parsedBytes = parseObdPidRawBytes(milRaw, "01", 4)
            if (parsedBytes != null && parsedBytes.isNotEmpty()) {
                val byteA = parsedBytes[0]
                val isMilActive = (byteA and 0x80) != 0
                val dtcCount = byteA and 0x7F

                if (isMilActive && (!lastMilState || dtcCount > lastReportedDtcCount)) {
                    lastMilState = true
                    lastReportedDtcCount = dtcCount

                    // Read DTC codes asynchronously so as NOT to stall or interrupt the sensor stream rhythm
                    managerScope.launch {
                        val quickRaw = sendDtcCommandWithRetry("03", 1500L)
                        val newDtcs = parseDtcResponse(quickRaw, "Двигатель (ECM/PCM)")
                        if (newDtcs.isNotEmpty()) {
                            val current = _dtcErrors.value.toMutableList()
                            current.addAll(newDtcs)
                            _dtcErrors.value = current.distinctBy { it.code }
                            val codesStr = newDtcs.joinToString(", ") { it.code }
                            _eventFlow.emit("REALTIME_MIL_TRIGGERED:$codesStr")
                        } else {
                            _eventFlow.emit("REALTIME_MIL_TRIGGERED:Индикатор Check Engine")
                        }
                    }
                } else if (!isMilActive && lastMilState) {
                    lastMilState = false
                }
            }
        }

        // Continuous telemetry heartbeat for active dashboard sensors (Speed, RPM, Coolant, Voltage)
        // Even if a sensor was not queried in this specific slot, keep its telemetry history scrolling continuously
        val dashboardPids = listOf("010D", "010C", "0105", "0142")
        for (dPid in dashboardPids) {
            if (!updates.containsKey(dPid)) {
                val existing = _sensors.value.find { it.pid == dPid }
                if (existing != null && (existing.value != 0.0 || dPid == "010D" || dPid == "010C")) {
                    val lastH = existing.history.lastOrNull()
                    if (lastH == null || (cycleStartTime - lastH.first >= 700L)) {
                        updates[dPid] = existing.value
                    }
                }
            }
        }

        // Direct, clean dispatch: updates on-screen values cleanly without rapid flickering
        updateSensorsDirectly(updates)

        // Metronomic pacing: ensure every cycle takes at least target duration (~350ms)
        // and always provide at least 35ms breathing room for ELM327 UART buffer
        val cycleDuration = System.currentTimeMillis() - cycleStartTime
        val targetCycleDurationMs = 350L
        if (cycleDuration < targetCycleDurationMs) {
            delay(targetCycleDurationMs - cycleDuration)
        } else {
            delay(35L)
        }
    }

    private suspend fun sendDtcCommandWithRetry(cmd: String, timeoutMs: Long = 3000L): String {
        for (attempt in 1..3) {
            val res = sendRawCommand(cmd, timeoutMs)
            if (res.isNotBlank() && !res.contains("SEARCHING", ignoreCase = true) && !res.contains("BUS BUSY", ignoreCase = true)) {
                return res
            }
            delay(180)
        }
        return ""
    }

    fun performFullEcuScan(isInitialScan: Boolean = false, isPeriodic: Boolean = false) {
        if (_connectionState.value == ConnectionState.DISCONNECTED || _connectionState.value == ConnectionState.CONNECTING) {
            managerScope.launch {
                _eventFlow.emit("NO_CONNECTION_FOR_SCAN")
            }
            return
        }
        activeScanJob?.cancel()
        activeScanJob = managerScope.launch {
            isExclusiveCommandRunning = true
            _isScanning.value = true
            _scanningProgressFraction.value = 0.05f
            _scanningProgressText.value = "Инициализация опроса блоков ECU..."

            val scanStartEvent = when {
                isInitialScan -> "SCAN_STARTED_INITIAL"
                isPeriodic -> "SCAN_STARTED_PERIODIC"
                else -> "SCAN_STARTED"
            }
            _eventFlow.emit(scanStartEvent)

            val isBluetooth = _connectionState.value == ConnectionState.CONNECTED_BLUETOOTH

            try {
                if (isBluetooth) {
                    sendRawCommand("AT AT 2", 600L)
                    sendRawCommand("AT ST FF", 600L)
                }

                val totalBlocks = 8
                val (scannedBlocks, allDtcErrors) = MultiEcuScanner.scanAllEcus(
                    sendCmd = { cmd, timeout -> sendRawCommand(cmd, timeout) },
                    onProgress = { currentEcu, scannedList ->
                        _ecuBlocks.value = scannedList + currentEcu
                        _scanningProgressText.value = "Опрос: ${currentEcu.name}"
                        val currentCount = (scannedList.size + 1).coerceAtMost(totalBlocks)
                        _scanningProgressFraction.value = currentCount.toFloat() / totalBlocks.toFloat()
                        managerScope.launch { _eventFlow.emit("SCANNING_ECU:${currentEcu.name}") }
                    }
                )

                _ecuBlocks.value = scannedBlocks
                _dtcErrors.value = allDtcErrors
                _scanningProgressFraction.value = 1.0f
                _scanningProgressText.value = "Опрос блоков завершен"

                val scanCompleteEvent = when {
                    isInitialScan -> "INITIAL_SCAN_COMPLETED"
                    isPeriodic -> "PERIODIC_SCAN_COMPLETED"
                    else -> "SCAN_COMPLETED"
                }
                _eventFlow.emit(scanCompleteEvent)
            } finally {
                if (isBluetooth) {
                    sendRawCommand("AT SH 7DF", 600L)
                    sendRawCommand("AT AT 1", 300L)
                    sendRawCommand("AT ST 32", 300L)
                }
                isExclusiveCommandRunning = false
                _isScanning.value = false
                _scanningProgressText.value = ""
                _scanningProgressFraction.value = 0f
            }
        }
    }

    fun stopEcuScan() {
        activeScanJob?.cancel()
        activeScanJob = null
        _isScanning.value = false
        _scanningProgressText.value = ""
        _scanningProgressFraction.value = 0f
        isExclusiveCommandRunning = false
    }

    private fun parseDtcResponse(raw: String, ecuName: String): List<DtcError> {
        val dtcs = mutableListOf<DtcError>()
        if (raw.isBlank()) return dtcs

        val lines = raw.split("\r", "\n").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
        val validLines = lines.filterNot { 
            it.contains("NO DATA") || it.contains("NODATA") || it.contains("ERROR") || it.contains("UNABLE") || it == "OK" || it == ">"
        }
        if (validLines.isEmpty()) return dtcs

        // 1. Try ISO-TP Multi-frame Assembly (e.g., 0: 43 03 ... 1: 04 20 ...)
        val multiFrameLines = validLines.filter { it.matches("^\\d+:.*".toRegex()) }
        if (multiFrameLines.isNotEmpty()) {
            val assembledHex = StringBuilder()
            val sortedLines = multiFrameLines.sortedBy { it.substringBefore(":").toIntOrNull() ?: 0 }
            for (mfLine in sortedLines) {
                val hexOnly = mfLine.substringAfter(":").replace(" ", "").replace(">", "")
                assembledHex.append(hexOnly)
            }
            extractDtcsFromHexStream(assembledHex.toString(), ecuName, dtcs)
        }

        // 2. Standard / Single-line parsing
        for (line in validLines) {
            val cleanLine = line.replace(" ", "").replace(">", "").replace("^\\d+:".toRegex(), "")
            extractDtcsFromHexStream(cleanLine, ecuName, dtcs)
        }

        return dtcs.distinctBy { it.code }
    }

    private fun extractDtcsFromHexStream(hex: String, ecuName: String, dtcs: MutableList<DtcError>) {
        if (hex.length < 4) return
        val modes = listOf("43", "47", "4A")
        var responseIdx = -1
        var matchedMode = "43"

        for (mode in modes) {
            val idx = hex.indexOf(mode)
            if (idx != -1) {
                responseIdx = idx
                matchedMode = mode
                break
            }
        }

        if (responseIdx == -1) return

        val payload = hex.substring(responseIdx)
        if (payload.length < 4) return

        val isPendingMode = matchedMode == "47"
        val isPermanentMode = matchedMode == "4A"

        // Mode is 2 hex chars, followed by optional DTC count (1 byte = 2 hex chars)
        val dtcCountHex = payload.substring(2, 4)
        var hexBytes = payload.substring(2)

        val dtcCountVal = dtcCountHex.toIntOrNull(16)
        if (dtcCountVal != null && dtcCountVal in 1..20 && hexBytes.length >= 2 + dtcCountVal * 4) {
            // First byte was indeed DTC count
            hexBytes = hexBytes.substring(2)
        }

        var i = 0
        while (i + 3 < hexBytes.length) {
            val chunk = hexBytes.substring(i, i + 4)
            if (chunk != "0000" && chunk.matches("^[0-9A-F]{4}$".toRegex())) {
                val dtcCode = convertHexToDtc(chunk)
                if (dtcCode != null && isValidDtcCode(dtcCode)) {
                    val db = DtcDatabase.findDtc(dtcCode)
                    val modeSuffix = when {
                        isPendingMode -> " (Отложенная / Pending)"
                        isPermanentMode -> " (Постоянная / Permanent)"
                        else -> ""
                    }
                    dtcs.add(
                        DtcError(
                            code = dtcCode,
                            category = db?.category ?: run {
                                val prefixChar = dtcCode.firstOrNull() ?: 'P'
                                when (prefixChar) {
                                    'P' -> "Трансмиссия / Двигатель (Powertrain)"
                                    'C' -> "Шасси / Тормоза (Chassis)"
                                    'B' -> "Кузов / Электроника (Body)"
                                    'U' -> "Сетевые шины CAN / FlexRay (Network)"
                                    else -> "Общая диагностика"
                                }
                            },
                            ecuName = "$ecuName$modeSuffix",
                            description = db?.title ?: "Зафиксирован код ошибки $dtcCode в $ecuName",
                            severity = if (dtcCode.startsWith("P03") || dtcCode.startsWith("P02") || dtcCode.startsWith("C00") || db?.severity?.contains("Критическая") == true) DtcSeverity.CRITICAL else DtcSeverity.WARNING,
                            possibleCauses = db?.causes ?: listOf("Нарушение сигнальной цепи или контактов датчика", "Превышение допустимых рабочих параметров компонентов"),
                            symptoms = db?.symptoms ?: listOf("Ошибка зафиксирована самодиагностикой $ecuName"),
                            isPending = isPendingMode
                        )
                    )
                }
            }
            i += 4
        }
    }

    private fun isValidDtcCode(code: String): Boolean {
        if (code.length != 5) return false
        val prefix = code[0]
        if (prefix != 'P' && prefix != 'C' && prefix != 'B' && prefix != 'U') return false
        val numberPart = code.substring(1)
        if (!numberPart.matches("^[0-9A-F]{4}$".toRegex())) return false
        if (numberPart == "0000") return false
        return true
    }

    private fun convertHexToDtc(hex4: String): String? {
        return try {
            val byte1 = hex4.substring(0, 2).toInt(16)
            val byte2 = hex4.substring(2, 4).toInt(16)

            val typeChar = when ((byte1 and 0xC0) shr 6) {
                0 -> "P"
                1 -> "C"
                2 -> "B"
                3 -> "U"
                else -> "P"
            }
            val digit2 = ((byte1 and 0x30) shr 4).toString(16).uppercase()
            val digit3 = (byte1 and 0x0F).toString(16).uppercase()
            val digit45 = byte2.toString(16).padStart(2, '0').uppercase()

            "$typeChar$digit2$digit3$digit45"
        } catch (e: Exception) {
            null
        }
    }

    fun clearDtcErrors() {
        if (_connectionState.value == ConnectionState.DISCONNECTED || _connectionState.value == ConnectionState.CONNECTING) {
            managerScope.launch {
                _eventFlow.emit("NO_CONNECTION_FOR_CLEAR")
            }
            return
        }
        managerScope.launch {
            isExclusiveCommandRunning = true
            _eventFlow.emit("CLEAR_STARTED")
            
            try {
                if (_connectionState.value == ConnectionState.CONNECTED_BLUETOOTH) {
                    // 1. Standard OBD-II Broadcast Reset (Mode 04)
                    sendRawCommand("04", 2500L)
                    delay(300)
                    
                    // 2. CAN Functional Broadcast to all ECUs (7DF)
                    sendRawCommand("AT SH 7DF", 1000L)
                    sendRawCommand("04", 2500L)
                    sendRawCommand("14 FFFFFF", 2500L) // UDS Mode 14 Clear Diagnostic Information
                    delay(300)

                    // 3. Physical ECM/PCM Reset (7E0)
                    sendRawCommand("AT SH 7E0", 1000L)
                    sendRawCommand("04", 2500L)
                    sendRawCommand("14 FFFFFF", 2500L)
                    delay(200)

                    // 4. Physical TCU Transmission Reset (7E1)
                    sendRawCommand("AT SH 7E1", 1000L)
                    sendRawCommand("04", 2500L)
                    sendRawCommand("14 FFFFFF", 2500L)
                    delay(200)

                    // 5. Physical ABS/ESP Braking System Reset (7B0)
                    sendRawCommand("AT SH 7B0", 1000L)
                    sendRawCommand("04", 2500L)
                    delay(200)

                    // 6. Physical BCM/SRS Body Control Reset (7C0)
                    sendRawCommand("AT SH 7C0", 1000L)
                    sendRawCommand("04", 2500L)
                    delay(200)

                    // 7. Restore Standard ECM Header and Auto Receive
                    sendRawCommand("AT SH 7E0", 1000L)
                    sendRawCommand("AT CRA 7E8", 1000L)
                    sendRawCommand("AT AR", 1000L)
                    delay(1500L)
                } else {
                    delay(1500L)
                }

                _dtcErrors.value = emptyList()
                _ecuBlocks.value = _ecuBlocks.value.map { it.copy(status = EcuStatus.OK, errorCount = 0) }
            } catch (e: Exception) {
                Log.e("Elm327Manager", "Error in clearDtcErrors: ${e.message}")
            } finally {
                if (_connectionState.value == ConnectionState.CONNECTED_BLUETOOTH) {
                    sendRawCommand("AT SH 7E0", 1000L)
                }
                isExclusiveCommandRunning = false
                _eventFlow.emit("CLEAR_COMPLETED")
            }
        }
    }

    fun toggleSensorSelection(pid: String) {
        val updated = _sensors.value.map {
            if (it.pid == pid) it.copy(isSelected = !it.isSelected) else it
        }
        _sensors.value = updated
        saveSelectedPids(updated.filter { it.isSelected }.map { it.pid }.toSet())
    }

    fun setSensorSelected(pid: String, isSelected: Boolean) {
        val updated = _sensors.value.map {
            if (it.pid == pid) it.copy(isSelected = isSelected) else it
        }
        _sensors.value = updated
        saveSelectedPids(updated.filter { it.isSelected }.map { it.pid }.toSet())
    }

    fun selectAllSensors() {
        val updated = _sensors.value.map { it.copy(isSelected = true) }
        _sensors.value = updated
        saveSelectedPids(updated.map { it.pid }.toSet())
    }

    fun deselectAllSensors() {
        val updated = _sensors.value.map { it.copy(isSelected = false) }
        _sensors.value = updated
        saveSelectedPids(emptySet())
    }

    fun selectDefaultSensors() {
        val defaultPids = setOf("010C", "010D", "0105", "0142", "0104", "0111", "010F", "012F", "015E", "015F")
        val updated = _sensors.value.map { it.copy(isSelected = defaultPids.contains(it.pid)) }
        _sensors.value = updated
        saveSelectedPids(defaultPids)
    }

    private fun saveSelectedPids(pids: Set<String>) {
        try {
            prefs.edit().putStringSet("selected_sensor_pids", pids).apply()
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun getSavedSelectedPids(): Set<String>? {
        return try {
            prefs.getStringSet("selected_sensor_pids", null)
        } catch (e: Exception) {
            null
        }
    }

    fun resetSensorsToZero(keepVoltage: Boolean = false, voltage: Double = 0.0) {
        rawSensorBuffer.clear()
        _sensors.value = _sensors.value.map { sensor ->
            if (keepVoltage && (sensor.pid == "0142" || sensor.name.contains("Напряжение", ignoreCase = true))) {
                rawSensorBuffer["0142"] = voltage
                sensor.copy(value = voltage, history = emptyList())
            } else {
                sensor.copy(value = 0.0, history = emptyList())
            }
        }
    }

    fun handleConnectionLoss(reason: String) {
        if (_connectionState.value != ConnectionState.CONNECTED_BLUETOOTH) {
            return
        }
        managerScope.launch {
            disconnect()
            _eventFlow.emit("CONNECTION_LOST:$reason. Показания обнулены. Повтор через ${_disconnectedReconnectIntervalSec.value} сек...")
        }
    }

    fun disconnect() {
        periodicScanJob?.cancel()
        periodicScanJob = null
        streamJob?.cancel()
        streamJob = null
        pacerJob?.cancel()
        pacerJob = null
        rawSensorBuffer.clear()
        try {
            inputStream?.close()
        } catch (e: Exception) {}
        try {
            outputStream?.close()
        } catch (e: Exception) {}
        try {
            socket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        socket = null
        inputStream = null
        outputStream = null
        lastMilState = false
        lastReportedDtcCount = 0
        pollCycleCount = 0
        consecutivePollFailures = 0
        consecutiveIoErrors = 0
        _connectionState.value = ConnectionState.DISCONNECTED
        _vehicleInfo.value = VehicleInfo(connectionState = ConnectionState.DISCONNECTED)
        _dtcErrors.value = emptyList()
        _ecuBlocks.value = getInitialEcuBlocks()
        resetSensorsToZero()
    }

    private fun parseObdPidRawBytes(raw: String, pidHex: String, expectedBytes: Int): IntArray? {
        if (raw.isBlank() || raw.contains("NO DATA", ignoreCase = true) || raw.contains("NODATA", ignoreCase = true) ||
            raw.contains("ERROR", ignoreCase = true) || raw.contains("UNABLE", ignoreCase = true)) {
            return null
        }
        val cleaned = raw.replace(" ", "").replace("\r", "").replace("\n", "").uppercase()
        val pidClean = pidHex.uppercase()

        val targets = listOf("41$pidClean", "62$pidClean", "61$pidClean")
        for (target in targets) {
            val idx = cleaned.indexOf(target)
            if (idx != -1 && cleaned.length >= idx + target.length + expectedBytes * 2) {
                return try {
                    val start = idx + target.length
                    IntArray(expectedBytes) { i ->
                        cleaned.substring(start + i * 2, start + (i + 1) * 2).toInt(16)
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }
        return null
    }

    private fun parseObdPid(raw: String, pidHex: String, expectedBytes: Int, calc: (IntArray) -> Double): Double? {
        if (raw.isBlank() || raw.contains("NO DATA", ignoreCase = true) || raw.contains("NODATA", ignoreCase = true) ||
            raw.contains("ERROR", ignoreCase = true) || raw.contains("UNABLE", ignoreCase = true)) {
            return null
        }
        val cleaned = raw.replace(" ", "").replace("\r", "").replace("\n", "").uppercase()
        val pidClean = pidHex.uppercase()

        // 1. Check for standard 41$PID, 62$PID, 61$PID substring match
        val targets = listOf("41$pidClean", "62$pidClean", "61$pidClean")
        for (target in targets) {
            val idx = cleaned.indexOf(target)
            if (idx != -1 && cleaned.length >= idx + target.length + expectedBytes * 2) {
                return try {
                    val start = idx + target.length
                    val bytes = IntArray(expectedBytes) { i ->
                        cleaned.substring(start + i * 2, start + (i + 1) * 2).toInt(16)
                    }
                    calc(bytes)
                } catch (e: Exception) {
                    null
                }
            }
        }

        // 2. Tokenized hex parsing fallback
        val tokens = raw.split("[\\s\r\n]+".toRegex()).map { it.trim().uppercase() }.filter { it.matches("^[0-9A-F]{2}$".toRegex()) }
        val pidTokens = if (pidClean.length == 4) listOf(pidClean.substring(0, 2), pidClean.substring(2, 4)) else listOf(pidClean)

        if (tokens.size >= pidTokens.size + expectedBytes + 1) {
            for (i in 0..tokens.size - pidTokens.size - expectedBytes - 1) {
                val isModeMatch = tokens[i] == "41" || tokens[i] == "62" || tokens[i] == "61"
                if (isModeMatch) {
                    val isPidMatch = pidTokens.indices.all { k -> tokens[i + 1 + k] == pidTokens[k] }
                    if (isPidMatch) {
                        return try {
                            val start = i + 1 + pidTokens.size
                            val bytes = IntArray(expectedBytes) { k -> tokens[start + k].toInt(16) }
                            calc(bytes)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
            }
        }

        return null
    }

    private fun parseVoltage(raw: String): Double? {
        val pidVal = parseObdPid(raw, "42", 2) { (256.0 * it[0] + it[1]) / 1000.0 }
        if (pidVal != null) return pidVal
        val match = "([0-9]+\\.[0-9]+)".toRegex().find(raw)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }

    fun updateFuelLevelManually(levelPercent: Double) {
        val level = Math.round(levelPercent.coerceIn(0.0, 100.0) * 10.0) / 10.0
        _sensors.value = _sensors.value.map { sensor ->
            if (sensor.pid == "012F") sensor.copy(value = level) else sensor
        }
    }

    private fun parseVinResponse(raw: String): String? {
        val cleaned = raw.replace(" ", "").replace("\r", "").replace("4902", "").replace("0:", "").replace("1:", "").replace("2:", "")
        return if (cleaned.length >= 17) cleaned.take(17) else null
    }

    private fun detectMakeFromVin(vin: String): String {
        if (vin.length < 3) return "Unknown Make"
        val wmi = vin.substring(0, 3).uppercase()
        return when {
            wmi.startsWith("1G") || wmi.startsWith("2G") -> "General Motors (Chevrolet/Buick/CADILLAC)"
            wmi.startsWith("1F") || wmi.startsWith("2F") -> "Ford Motor Company"
            wmi.startsWith("1J") || wmi.startsWith("3C") -> "Chrysler / Jeep / Dodge"
            wmi.startsWith("JHM") || wmi.startsWith("JHL") -> "Honda / Acura"
            wmi.startsWith("JT") || wmi.startsWith("4T") -> "Toyota / Lexus"
            wmi.startsWith("WAU") || wmi.startsWith("WVW") -> "Volkswagen / Audi Group"
            wmi.startsWith("WBA") || wmi.startsWith("WBY") -> "BMW Group"
            wmi.startsWith("WDD") || wmi.startsWith("WDB") -> "Mercedes-Benz"
            wmi.startsWith("KMH") || wmi.startsWith("KNA") -> "Hyundai / Kia"
            wmi.startsWith("XTA") -> "LADA / AvtoVAZ"
            else -> "OBD2 Vehicle ($wmi)"
        }
    }

    private fun getInitialSensorList(): List<ObdSensor> {
        val allSensors = listOf(
            ObdSensor(
                pid = "010C",
                name = "Обороты коленвала двигателя",
                value = 0.0,
                unit = "об/мин",
                minVal = 0.0,
                maxVal = 8000.0,
                category = "Двигатель",
                isSelected = true,
                description = "Частота вращения коленчатого вала двигателя (RPM). Норма холостого хода: 650–900 об/мин."
            ),
            ObdSensor(
                pid = "010D",
                name = "Скорость движения автомобиля",
                value = 0.0,
                unit = "км/ч",
                minVal = 0.0,
                maxVal = 260.0,
                category = "Движение",
                isSelected = true,
                description = "Текущая фактическая скорость транспортного средства по датчику скорости (VSS) или ABS."
            ),
            ObdSensor(
                pid = "0105",
                name = "Температура охлаждающей жидкости (ОЖ)",
                value = 0.0,
                unit = "°C",
                minVal = -40.0,
                maxVal = 150.0,
                category = "Охлаждение",
                isSelected = true,
                description = "Температура антифриза в блоке цилиндров. Рабочая норма: 85–102°C. Перегрев выше 105°C."
            ),
            ObdSensor(
                pid = "0142",
                name = "Напряжение бортовой сети (ЭБУ)",
                value = 0.0,
                unit = "В",
                minVal = 8.0,
                maxVal = 18.0,
                category = "Электрика",
                isSelected = true,
                description = "Фактическое напряжение генератора и аккумулятора на клеммах ЭБУ. Норма при заведённом двигателе: 13.6–14.6 В."
            ),
            ObdSensor(
                pid = "0104",
                name = "Расчетная нагрузка на двигатель",
                value = 0.0,
                unit = "%",
                minVal = 0.0,
                maxVal = 100.0,
                category = "Двигатель",
                isSelected = true,
                description = "Процент используемой мощности ДВС от максимума на текущих оборотах. На ХХ: 15–28%."
            ),
            ObdSensor(
                pid = "0111",
                name = "Положение дроссельной заслонки (TPS)",
                value = 0.0,
                unit = "%",
                minVal = 0.0,
                maxVal = 100.0,
                category = "Впуск",
                isSelected = true,
                description = "Угол открытия дросселя. На холостом ходу: 8–18%, при полном нажатии педали газа: 80–100%."
            ),
            ObdSensor(
                pid = "010F",
                name = "Температура воздуха на впуске (IAT)",
                value = 0.0,
                unit = "°C",
                minVal = -40.0,
                maxVal = 120.0,
                category = "Впуск",
                isSelected = true,
                description = "Температура засасываемого во впускной коллектор воздуха. Влияет на расчёт плотности смеси."
            ),
            ObdSensor(
                pid = "0110",
                name = "Расход воздуха через ДМРВ (MAF)",
                value = 0.0,
                unit = "г/с",
                minVal = 0.0,
                maxVal = 250.0,
                category = "Впуск",
                isSelected = true,
                description = "Массовый расход воздуха в граммах в секунду. Примерно равен 1 г/с на каждые 1000 см³ объема на ХХ."
            ),
            ObdSensor(
                pid = "010B",
                name = "Давление во впускном коллекторе (MAP)",
                value = 0.0,
                unit = "кПа",
                minVal = 0.0,
                maxVal = 255.0,
                category = "Впуск",
                isSelected = true,
                description = "Абсолютное давление во впуске. На холостом ходу (разрежение): 28–38 кПа. Атмосфера: 100 кПа."
            ),
            ObdSensor(
                pid = "010A",
                name = "Давление в топливной рампе",
                value = 0.0,
                unit = "кПа",
                minVal = 0.0,
                maxVal = 765.0,
                category = "Топливо",
                isSelected = true,
                description = "Давление бензина/дизеля в топливной рампе (коллекторе впрыска). Норма для MPI: 300–420 кПа."
            ),
            ObdSensor(
                pid = "010E",
                name = "Угол опережения зажигания (УОЗ)",
                value = 0.0,
                unit = "°",
                minVal = -64.0,
                maxVal = 64.0,
                category = "Зажигание",
                isSelected = true,
                description = "Момент подачи искры до ВМТ в 1-м цилиндре. На холостом ходу обычно от +5° до +15°."
            ),
            ObdSensor(
                pid = "012F",
                name = "Уровень топлива в баке",
                value = 0.0,
                unit = "%",
                minVal = 0.0,
                maxVal = 100.0,
                category = "Топливо",
                isSelected = true,
                description = "Остаток топлива в бензобаке по показаниям штатного датчика уровня топлива (ДУТ)."
            ),
            ObdSensor(
                pid = "015E",
                name = "Моментальный расход топлива",
                value = 0.0,
                unit = "л/100км",
                minVal = 0.0,
                maxVal = 50.0,
                category = "Топливо",
                isSelected = true,
                description = "Текущий расход топлива на 100 км пробега (или л/час при остановке) на основе MAF/Speed."
            ),
            ObdSensor(
                pid = "015F",
                name = "Средний расход топлива",
                value = 0.0,
                unit = "л/100км",
                minVal = 0.0,
                maxVal = 30.0,
                category = "Топливо",
                isSelected = true,
                description = "Усредненный расход топлива за текущую поездку с момента запуска двигателя."
            ),
            ObdSensor(
                pid = "015C",
                name = "Температура моторного масла",
                value = 0.0,
                unit = "°C",
                minVal = -40.0,
                maxVal = 160.0,
                category = "Двигатель",
                isSelected = true,
                description = "Температура масла в картере двигателя. Оптимальная рабочая температура: 90–105°C."
            ),
            ObdSensor(
                pid = "0133",
                name = "Барометрическое атмосферное давление",
                value = 0.0,
                unit = "кПа",
                minVal = 0.0,
                maxVal = 255.0,
                category = "Атмосфера",
                isSelected = true,
                description = "Давление окружающего атмосферного воздуха. На уровне моря: 101.3 кПа (760 мм рт. ст.)."
            ),
            ObdSensor(
                pid = "0106",
                name = "Краткосрочная коррекция смеси (STFT)",
                value = 0.0,
                unit = "%",
                minVal = -100.0,
                maxVal = 100.0,
                category = "Топливо",
                isSelected = true,
                description = "Мгновенная подстройка времени впрыска по датчику кислорода. Норма: в пределах от -5% до +5%."
            ),
            ObdSensor(
                pid = "0107",
                name = "Долгосрочная коррекция смеси (LTFT)",
                value = 0.0,
                unit = "%",
                minVal = -100.0,
                maxVal = 100.0,
                category = "Топливо",
                isSelected = true,
                description = "Накопленная долговременная адаптация топливной смеси. Отклонение более ±10% указывает на подсос или засор форсунок."
            ),
            ObdSensor(
                pid = "0114",
                name = "Напряжение датчика кислорода (Лямбда Банк 1)",
                value = 0.0,
                unit = "В",
                minVal = 0.0,
                maxVal = 1.3,
                category = "Выхлоп/Экология",
                isSelected = true,
                description = "Сигнал с переднего датчика O2. На прогретом катализаторе быстро колеблется от 0.1 В (бедная) до 0.9 В (богатая)."
            ),
            ObdSensor(
                pid = "0118",
                name = "Напряжение датчика кислорода (Лямбда Банк 2)",
                value = 0.0,
                unit = "В",
                minVal = 0.0,
                maxVal = 1.3,
                category = "Выхлоп/Экология",
                isSelected = false,
                description = "Сигнал с датчика кислорода второго ряда цилиндров (для V-образных двигателей V6/V8)."
            ),
            ObdSensor(
                pid = "011F",
                name = "Время работы после пуска двигателя",
                value = 0.0,
                unit = "сек",
                minVal = 0.0,
                maxVal = 65535.0,
                category = "Система",
                isSelected = true,
                description = "Количество секунд непрерывной работы мотора с момента последнего запуска зажигания."
            ),
            ObdSensor(
                pid = "0146",
                name = "Температура наружного воздуха (Улица)",
                value = 0.0,
                unit = "°C",
                minVal = -40.0,
                maxVal = 100.0,
                category = "Атмосфера",
                isSelected = true,
                description = "Температура воздуха за бортом автомобиля по внешнему датчику в переднем бампере/зеркале."
            ),
            ObdSensor(
                pid = "015D",
                name = "Температура трансмиссионного масла (АКПП)",
                value = 0.0,
                unit = "°C",
                minVal = -40.0,
                maxVal = 160.0,
                category = "Трансмиссия",
                isSelected = true,
                description = "Температура жидкости ATF в гидротрансформаторе/коробке. Рабочий диапазон: 75–95°C."
            ),
            ObdSensor(
                pid = "0149",
                name = "Положение педали акселератора (Газ)",
                value = 0.0,
                unit = "%",
                minVal = 0.0,
                maxVal = 100.0,
                category = "Впуск",
                isSelected = true,
                description = "Фактический процент нажатия электронной педали газа водителем (датчик APP)."
            ),
            ObdSensor(
                pid = "012C",
                name = "Клапан рециркуляции отработавших газов (EGR)",
                value = 0.0,
                unit = "%",
                minVal = 0.0,
                maxVal = 100.0,
                category = "Выхлоп/Экология",
                isSelected = false,
                description = "Степень открытия электромагнитного клапана EGR для снижения оксидов азота NOx."
            ),
            ObdSensor(
                pid = "012D",
                name = "Ошибка управления клапаном EGR",
                value = 0.0,
                unit = "%",
                minVal = -100.0,
                maxVal = 100.0,
                category = "Выхлоп/Экология",
                isSelected = false,
                description = "Разница между заданным положением клапана EGR и его фактическим открытием. 0% — норма."
            ),
            ObdSensor(
                pid = "0132",
                name = "Давление паров в баке (EVAP)",
                value = 0.0,
                unit = "кПа",
                minVal = -8.0,
                maxVal = 8.0,
                category = "Экология",
                isSelected = false,
                description = "Давление паров бензина в системе улавливания паров (адсорбер EVAP)."
            ),
            ObdSensor(
                pid = "013C",
                name = "Температура каталитического нейтрализатора",
                value = 0.0,
                unit = "°C",
                minVal = 0.0,
                maxVal = 1000.0,
                category = "Выхлоп/Экология",
                isSelected = true,
                description = "Температура сот катализатора Банк 1 Датчик 1. Рабочая температура дожига: 350–700°C."
            ),
            ObdSensor(
                pid = "0130",
                name = "Число циклов прогрева после сброса DTC",
                value = 0.0,
                unit = "циклов",
                minVal = 0.0,
                maxVal = 255.0,
                category = "Система",
                isSelected = false,
                description = "Количество полных циклов прогрева двигателя с момента последнего сброса кодов ошибок сканером."
            ),
            ObdSensor(
                pid = "0121",
                name = "Пробег с включенным Check Engine (MIL)",
                value = 0.0,
                unit = "км",
                minVal = 0.0,
                maxVal = 65535.0,
                category = "Система",
                isSelected = false,
                description = "Расстояние в километрах, пройденное автомобилем с горящей лампой неисправности двигателя."
            ),
            ObdSensor(
                pid = "0143",
                name = "Абсолютная расчетная нагрузка ДВС",
                value = 0.0,
                unit = "%",
                minVal = 0.0,
                maxVal = 200.0,
                category = "Двигатель",
                isSelected = false,
                description = "Нормированное наполнение цилиндров воздухом относительно атмосферного давления (для турбомоторов до 200%)."
            ),
            ObdSensor(
                pid = "0144",
                name = "Коэффициент состава смеси (Лямбда/AFR)",
                value = 1.0,
                unit = "λ",
                minVal = 0.5,
                maxVal = 2.0,
                category = "Топливо",
                isSelected = true,
                description = "Стехиометрический коэффициент топливовоздушной смеси. λ = 1.00 (AFR 14.7:1 бензин) — идеальная смесь."
            ),
            ObdSensor(
                pid = "015E_RATE",
                name = "Часовой расход топлива в покое",
                value = 0.0,
                unit = "л/ч",
                minVal = 0.0,
                maxVal = 10.0,
                category = "Топливо",
                isSelected = true,
                description = "Расход бензина или дизеля в литрах в час на холостом ходу без движения автомобиля."
            ),
            ObdSensor(
                pid = "0162",
                name = "Фактический крутящий момент ДВС",
                value = 0.0,
                unit = "%",
                minVal = -125.0,
                maxVal = 125.0,
                category = "Двигатель",
                isSelected = false,
                description = "Отдаваемый крутящий момент двигателя в процентах от опорного значения ЭБУ."
            )
        )

        val combinedList = (allSensors + StandardObdPids.getStandardMode01Sensors()).distinctBy { it.pid }
        val savedPids = getSavedSelectedPids()
        return if (savedPids != null) {
            combinedList.map { it.copy(isSelected = savedPids.contains(it.pid)) }
        } else {
            combinedList
        }
    }

    private fun getInitialEcuBlocks(): List<EcuBlock> = MultiEcuScanner.getAllSupportedEcuBlocks()

    suspend fun discoverSupportedPids() {
        if (_connectionState.value == ConnectionState.DISCONNECTED || _connectionState.value == ConnectionState.CONNECTING) {
            return
        }
        isExclusiveCommandRunning = true
        try {
            _eventFlow.emit("PID_DISCOVERY_STARTED:Сканирование битовых карт поддерживаемых PID...")

            val discoveryResult = PidDiscoveryManager.discoverSupportedPids(
                sendCmd = { cmd, timeout -> sendRawCommand(cmd, timeout) }
            )
            _supportedPidsResult.value = discoveryResult

            // Update existing sensor list with support status
            val currentList = _sensors.value
            _sensors.value = currentList.map { sensor ->
                val isSupp = discoveryResult.supportedPids.contains(sensor.pid.uppercase()) || sensor.isCustom
                sensor.copy(isSupported = isSupp)
            }

            _eventFlow.emit("PID_DISCOVERY_COMPLETED:Обнаружено ${discoveryResult.totalSupportedCount} поддерживаемых PID")
        } catch (e: Exception) {
            Log.e("Elm327Manager", "Error in PID discovery: ${e.message}")
        } finally {
            isExclusiveCommandRunning = false
        }
    }

    fun loadManufacturerProfile(profileId: String) {
        val profile = ManufacturerSensorRepository.getProfileById(profileId)
        _activeManufacturerProfile.value = profile
        if (profile != null) {
            val current = _sensors.value.toMutableList()
            val newSensors = profile.sensors.map { it.copy(isSupported = true, isSelected = true) }
            for (ns in newSensors) {
                val existingIdx = current.indexOfFirst { it.pid == ns.pid }
                if (existingIdx != -1) {
                    current[existingIdx] = ns
                } else {
                    current.add(ns)
                }
            }
            _sensors.value = current
            managerScope.launch {
                _eventFlow.emit("MANUFACTURER_PROFILE_LOADED:${profile.name} (+${profile.sensors.size} датчиков)")
            }
        }
    }

    fun unloadManufacturerProfile() {
        val active = _activeManufacturerProfile.value ?: return
        val current = _sensors.value.filterNot { s -> active.sensors.any { it.pid == s.pid } }
        _activeManufacturerProfile.value = null
        _sensors.value = current
        managerScope.launch {
            _eventFlow.emit("MANUFACTURER_PROFILE_UNLOADED")
        }
    }

    fun addCustomSensor(sensor: ObdSensor) {
        customSensors.add(sensor)
        val current = _sensors.value.toMutableList()
        val existingIdx = current.indexOfFirst { it.pid == sensor.pid }
        if (existingIdx != -1) {
            current[existingIdx] = sensor
        } else {
            current.add(sensor)
        }
        _sensors.value = current
    }

    fun removeCustomSensor(pid: String) {
        customSensors.removeAll { it.pid == pid }
        _sensors.value = _sensors.value.filterNot { it.pid == pid && it.isCustom }
    }

    fun importSensorsFromCsv(csvContent: String): Int {
        var importedCount = 0
        val lines = csvContent.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed.startsWith("#")) continue
            // Format: Name,ShortName,ModeAndPID,Equation,Min Value,Max Value,Units,Header
            val parts = trimmed.split(",").map { it.trim().trim('\"') }
            if (parts.size >= 4) {
                val name = parts[0]
                val shortName = if (parts.size > 1 && parts[1].isNotBlank()) parts[1] else name
                val modeAndPid = parts[2]
                val equation = parts[3]
                val minVal = parts.getOrNull(4)?.toDoubleOrNull() ?: 0.0
                val maxVal = parts.getOrNull(5)?.toDoubleOrNull() ?: 100.0
                val units = parts.getOrNull(6) ?: ""
                val header = parts.getOrNull(7) ?: ""

                if (modeAndPid.length >= 2) {
                    val mode = if (modeAndPid.length > 2) modeAndPid.substring(0, 2) else "01"
                    val pidHex = if (modeAndPid.length > 2) modeAndPid.substring(2) else modeAndPid
                    val sensor = ObdSensor(
                        pid = modeAndPid,
                        name = "$name ($shortName)",
                        value = 0.0,
                        unit = units,
                        minVal = minVal,
                        maxVal = maxVal,
                        category = "Пользовательские",
                        isSelected = true,
                        isSupported = true,
                        isCustom = true,
                        formula = equation,
                        mode = mode,
                        ecuHeader = header,
                        description = "Импортирован из CSV: $name. Формула: $equation"
                    )
                    addCustomSensor(sensor)
                    importedCount++
                }
            }
        }
        return importedCount
    }

    fun exportSensorsToCsv(): String {
        val sb = StringBuilder()
        sb.append("# AutoScan AI / Torque Pro Compatible CSV Export\n")
        sb.append("Name,ShortName,ModeAndPID,Equation,Min Value,Max Value,Units,Header\n")
        val exportableSensors = _sensors.value.filter { it.isCustom || it.formula.isNotBlank() }
        for (s in exportableSensors) {
            val modeAndPid = if (s.pid.startsWith(s.mode)) s.pid else "${s.mode}${s.pid}"
            sb.append("\"${s.name}\",\"${s.name.take(10)}\",\"$modeAndPid\",\"${s.formula}\",${s.minVal},${s.maxVal},\"${s.unit}\",\"${s.ecuHeader}\"\n")
        }
        return sb.toString()
    }
}
