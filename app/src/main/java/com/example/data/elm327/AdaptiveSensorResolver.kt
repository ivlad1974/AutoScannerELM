package com.example.data.elm327

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

data class SensorStrategy(
    val id: String,
    val name: String,
    val setupCommands: List<String> = emptyList(),
    val pidCommand: String,
    val cleanupCommands: List<String> = emptyList(),
    val expectedModeResponse: String, // e.g. "41", "62", "61"
    val expectedPidMatch: String,     // e.g. "2F", "1127", "29", "01"
    val parseBytesCount: Int,
    val calculation: (IntArray) -> Double?,
    val validRange: ClosedFloatingPointRange<Double> = 0.0..100.0
)

class AdaptiveSensorResolver(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("obd_adaptive_resolver", Context.MODE_PRIVATE)
    private val memoryCache = mutableMapOf<String, SensorStrategy>()
    private val unsupportedSensors = mutableSetOf<String>()

    private val strategiesMap: Map<String, List<SensorStrategy>> = mapOf(
        // FUEL LEVEL (012F)
        "012F" to listOf(
            SensorStrategy(
                id = "STD_012F",
                name = "Стандартный OBD2 (Mode 01 PID 2F)",
                pidCommand = "012F",
                expectedModeResponse = "41",
                expectedPidMatch = "2F",
                parseBytesCount = 1,
                calculation = { bytes -> (bytes[0] * 100.0) / 255.0 },
                validRange = 0.0..100.0
            ),
            SensorStrategy(
                id = "CAN_7E0_012F",
                name = "CAN Заголовок 7E0 (Engine ECM Mode 01 2F)",
                setupCommands = listOf("AT SH 7E0"),
                pidCommand = "012F",
                cleanupCommands = listOf("AT SH 7DF"),
                expectedModeResponse = "41",
                expectedPidMatch = "2F",
                parseBytesCount = 1,
                calculation = { bytes -> (bytes[0] * 100.0) / 255.0 },
                validRange = 0.0..100.0
            ),
            SensorStrategy(
                id = "CAN_7E2_012F",
                name = "CAN Заголовок 7E2 (Панель приборов / Cluster)",
                setupCommands = listOf("AT SH 7E2"),
                pidCommand = "012F",
                cleanupCommands = listOf("AT SH 7DF"),
                expectedModeResponse = "41",
                expectedPidMatch = "2F",
                parseBytesCount = 1,
                calculation = { bytes -> (bytes[0] * 100.0) / 255.0 },
                validRange = 0.0..100.0
            ),
            SensorStrategy(
                id = "GM_UDS_221127",
                name = "GM / Opel / Chevrolet (UDS Mode 22 DID 1127)",
                pidCommand = "221127",
                expectedModeResponse = "62",
                expectedPidMatch = "1127",
                parseBytesCount = 1,
                calculation = { bytes -> (bytes[0] * 100.0) / 255.0 },
                validRange = 0.0..100.0
            ),
            SensorStrategy(
                id = "FORD_UDS_22012F",
                name = "Ford / Mazda (UDS Mode 22 DID 012F)",
                pidCommand = "22012F",
                expectedModeResponse = "62",
                expectedPidMatch = "012F",
                parseBytesCount = 1,
                calculation = { bytes -> (bytes[0] * 100.0) / 255.0 },
                validRange = 0.0..100.0
            ),
            SensorStrategy(
                id = "FORD_UDS_22162E",
                name = "Ford Focus / Kuga (Mode 22 DID 162E)",
                pidCommand = "22162E",
                expectedModeResponse = "62",
                expectedPidMatch = "162E",
                parseBytesCount = 2,
                calculation = { bytes -> ((bytes[0] * 256.0 + bytes[1]) / 100.0).coerceIn(0.0, 100.0) },
                validRange = 0.0..100.0
            ),
            SensorStrategy(
                id = "TOYOTA_2129",
                name = "Toyota / Lexus / Scion (Mode 21 PID 29)",
                pidCommand = "2129",
                expectedModeResponse = "61",
                expectedPidMatch = "29",
                parseBytesCount = 1,
                calculation = { bytes -> (bytes[0] * 100.0) / 255.0 },
                validRange = 0.0..100.0
            ),
            SensorStrategy(
                id = "HYUNDAI_KIA_2101",
                name = "Hyundai / Kia (Mode 21 PID 01)",
                pidCommand = "2101",
                expectedModeResponse = "61",
                expectedPidMatch = "01",
                parseBytesCount = 1,
                calculation = { bytes -> (bytes[0] * 100.0) / 255.0 },
                validRange = 0.0..100.0
            ),
            SensorStrategy(
                id = "RENAULT_LADA_222004",
                name = "Renault / Lada Vesta / XRAY (BCM UDS 222004)",
                setupCommands = listOf("AT SH 760"),
                pidCommand = "222004",
                cleanupCommands = listOf("AT SH 7DF"),
                expectedModeResponse = "62",
                expectedPidMatch = "2004",
                parseBytesCount = 1,
                calculation = { bytes -> (bytes[0] * 100.0) / 255.0 },
                validRange = 0.0..100.0
            ),
            SensorStrategy(
                id = "NISSAN_760_2101",
                name = "Nissan / Infiniti (BCM Заголовок 760 Mode 21 01)",
                setupCommands = listOf("AT SH 760"),
                pidCommand = "2101",
                cleanupCommands = listOf("AT SH 7DF"),
                expectedModeResponse = "61",
                expectedPidMatch = "01",
                parseBytesCount = 1,
                calculation = { bytes -> (bytes[0] * 100.0) / 255.0 },
                validRange = 0.0..100.0
            ),
            SensorStrategy(
                id = "VAG_UDS_22F42F",
                name = "VAG (VW / Skoda / Audi / Seat UDS 22F42F)",
                pidCommand = "22F42F",
                expectedModeResponse = "62",
                expectedPidMatch = "F42F",
                parseBytesCount = 1,
                calculation = { bytes -> (bytes[0] * 100.0) / 255.0 },
                validRange = 0.0..100.0
            ),
            SensorStrategy(
                id = "MITSUBISHI_2129",
                name = "Mitsubishi (Mode 21 PID 29)",
                pidCommand = "2129",
                expectedModeResponse = "61",
                expectedPidMatch = "29",
                parseBytesCount = 1,
                calculation = { bytes -> (bytes[0] * 100.0) / 255.0 },
                validRange = 0.0..100.0
            )
        ),

        // ENGINE OIL TEMPERATURE (015C)
        "015C" to listOf(
            SensorStrategy(
                id = "STD_015C",
                name = "Стандартный OBD2 (015C)",
                pidCommand = "015C",
                expectedModeResponse = "41",
                expectedPidMatch = "5C",
                parseBytesCount = 1,
                calculation = { bytes -> bytes[0].toDouble() - 40.0 },
                validRange = -40.0..180.0
            ),
            SensorStrategy(
                id = "VAG_221154",
                name = "VAG UDS Mode 22 (221154)",
                pidCommand = "221154",
                expectedModeResponse = "62",
                expectedPidMatch = "1154",
                parseBytesCount = 1,
                calculation = { bytes -> bytes[0].toDouble() - 40.0 },
                validRange = -40.0..180.0
            ),
            SensorStrategy(
                id = "BMW_22D001",
                name = "BMW UDS Mode 22 (22D001)",
                pidCommand = "22D001",
                expectedModeResponse = "62",
                expectedPidMatch = "D001",
                parseBytesCount = 1,
                calculation = { bytes -> bytes[0].toDouble() - 48.0 },
                validRange = -40.0..180.0
            ),
            SensorStrategy(
                id = "TOYOTA_2129_OIL",
                name = "Toyota Mode 21 (2129)",
                pidCommand = "2129",
                expectedModeResponse = "61",
                expectedPidMatch = "29",
                parseBytesCount = 1,
                calculation = { bytes -> bytes[0].toDouble() - 40.0 },
                validRange = -40.0..180.0
            )
        ),

        // TRANSMISSION TEMPERATURE (015D)
        "015D" to listOf(
            SensorStrategy(
                id = "STD_015D",
                name = "Стандартный OBD2 (015D)",
                pidCommand = "015D",
                expectedModeResponse = "41",
                expectedPidMatch = "5D",
                parseBytesCount = 1,
                calculation = { bytes -> bytes[0].toDouble() - 40.0 },
                validRange = -40.0..180.0
            ),
            SensorStrategy(
                id = "GM_221940",
                name = "GM / Opel UDS Mode 22 (221940)",
                pidCommand = "221940",
                expectedModeResponse = "62",
                expectedPidMatch = "1940",
                parseBytesCount = 1,
                calculation = { bytes -> bytes[0].toDouble() - 40.0 },
                validRange = -40.0..180.0
            ),
            SensorStrategy(
                id = "TOYOTA_2182",
                name = "Toyota AT Fluid Mode 21 (2182)",
                pidCommand = "2182",
                expectedModeResponse = "61",
                expectedPidMatch = "82",
                parseBytesCount = 1,
                calculation = { bytes -> bytes[0].toDouble() - 40.0 },
                validRange = -40.0..180.0
            ),
            SensorStrategy(
                id = "FORD_221E1C",
                name = "Ford UDS Mode 22 (221E1C)",
                pidCommand = "221E1C",
                expectedModeResponse = "62",
                expectedPidMatch = "1E1C",
                parseBytesCount = 2,
                calculation = { bytes -> ((bytes[0] * 256.0 + bytes[1]) / 16.0) - 40.0 },
                validRange = -40.0..180.0
            ),
            SensorStrategy(
                id = "NISSAN_2101_AT",
                name = "Nissan Mode 21 (2101)",
                pidCommand = "2101",
                expectedModeResponse = "61",
                expectedPidMatch = "01",
                parseBytesCount = 1,
                calculation = { bytes -> bytes[0].toDouble() - 40.0 },
                validRange = -40.0..180.0
            )
        ),

        // FUEL PRESSURE (010A)
        "010A" to listOf(
            SensorStrategy(
                id = "STD_010A",
                name = "Стандартный OBD2 (010A)",
                pidCommand = "010A",
                expectedModeResponse = "41",
                expectedPidMatch = "0A",
                parseBytesCount = 1,
                calculation = { bytes -> bytes[0].toDouble() * 3.0 },
                validRange = 0.0..800.0
            ),
            SensorStrategy(
                id = "STD_0159",
                name = "Прямой впрыск High-Pressure (0159)",
                pidCommand = "0159",
                expectedModeResponse = "41",
                expectedPidMatch = "59",
                parseBytesCount = 2,
                calculation = { bytes -> (bytes[0] * 256.0 + bytes[1]) * 10.0 },
                validRange = 0.0..250000.0
            ),
            SensorStrategy(
                id = "VAG_221160",
                name = "VAG UDS Mode 22 (221160)",
                pidCommand = "221160",
                expectedModeResponse = "62",
                expectedPidMatch = "1160",
                parseBytesCount = 2,
                calculation = { bytes -> (bytes[0] * 256.0 + bytes[1]) / 10.0 },
                validRange = 0.0..10000.0
            )
        )
    )

    fun getWorkingStrategy(vin: String, pid: String): SensorStrategy? {
        val cacheKey = "${vin}_$pid"
        memoryCache[cacheKey]?.let { return it }

        val savedId = prefs.getString("strategy_${cacheKey}", null)
        if (savedId != null) {
            val list = strategiesMap[pid] ?: emptyList()
            val match = list.find { it.id == savedId }
            if (match != null) {
                memoryCache[cacheKey] = match
                return match
            }
        }
        return null
    }

    fun saveWorkingStrategy(vin: String, pid: String, strategy: SensorStrategy) {
        val cacheKey = "${vin}_$pid"
        memoryCache[cacheKey] = strategy
        unsupportedSensors.remove(cacheKey)
        prefs.edit().putString("strategy_${cacheKey}", strategy.id).apply()
        Log.i("AdaptiveSensorResolver", "✓ Запомнен рабочий метод для $pid ($vin): ${strategy.name} [${strategy.id}]")
    }

    fun markSensorUnsupported(vin: String, pid: String) {
        val cacheKey = "${vin}_$pid"
        unsupportedSensors.add(cacheKey)
    }

    fun isSensorMarkedUnsupported(vin: String, pid: String): Boolean {
        return unsupportedSensors.contains("${vin}_$pid")
    }

    fun getCandidateStrategies(pid: String): List<SensorStrategy> {
        return strategiesMap[pid] ?: emptyList()
    }

    fun getAllFuelStrategies(): List<SensorStrategy> {
        return strategiesMap["012F"] ?: emptyList()
    }

    fun getStrategyById(pid: String, strategyId: String): SensorStrategy? {
        return strategiesMap[pid]?.find { it.id == strategyId }
    }

    fun setManualStrategy(vin: String, pid: String, strategyId: String): Boolean {
        val strat = getStrategyById(pid, strategyId) ?: return false
        saveWorkingStrategy(vin, pid, strat)
        return true
    }

    fun clearCacheForVehicle(vin: String) {
        val keysToRemove = prefs.all.keys.filter { it.startsWith("strategy_${vin}_") }
        val editor = prefs.edit()
        keysToRemove.forEach { editor.remove(it) }
        editor.apply()
        memoryCache.clear()
        unsupportedSensors.clear()
    }
}
