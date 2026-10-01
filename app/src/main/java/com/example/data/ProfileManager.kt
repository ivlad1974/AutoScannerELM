package com.example.data

import android.content.Context
import android.os.Environment
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileReader
import java.io.FileWriter

data class AppProfileData(
    val profileVersion: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val isVoiceEnabled: Boolean = true,
    val appTheme: String = "DARK_SPORT",
    val savedDeviceAddress: String? = null,
    val savedDeviceName: String? = null,
    val alerts: List<SensorAlert> = emptyList(),
    val chatMessages: List<ChatMessage> = emptyList(),
    val vehicleInfo: VehicleInfo = VehicleInfo(),
    val tileConfigs: List<DashboardTileConfig> = emptyList(),
    val isHandsFreeEnabled: Boolean = false,
    val wakePhrase: String = "Автоскан",
    val readyResponse: String = "Слушаю вас",
    val sensorGraphPeriods: Map<String, Int> = emptyMap()
)

object ProfileManager {

    private const val PREFS_NAME = "obd2_app_profile_prefs"
    const val DEFAULT_PROFILE_FILENAME = "obd2_profile_backup.json"
    const val AUTO_EXPORT_FILENAME = "obd2_auto_export_backup.json"

    fun recordUserExportDir(context: Context, dirPath: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("last_user_export_dir", dirPath).apply()
    }

    fun autoExportProfileToFile(context: Context, profile: AppProfileData): File? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val customDirPath = prefs.getString("last_user_export_dir", null)
            val targetDir = if (customDirPath != null && File(customDirPath).exists()) {
                File(customDirPath)
            } else {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            }
            if (!targetDir.exists()) targetDir.mkdirs()

            // Created right next to the file created by the user, overwrites itself and does NOT multiply
            val file = File(targetDir, AUTO_EXPORT_FILENAME)
            val json = serializeToJson(profile)
            FileWriter(file, false).use { writer ->
                writer.write(json)
            }

            // Also keep synchronized auto-export copy in Downloads if targetDir is different
            val dlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (targetDir.absolutePath != dlDir.absolutePath) {
                try {
                    if (!dlDir.exists()) dlDir.mkdirs()
                    val dlFile = File(dlDir, AUTO_EXPORT_FILENAME)
                    FileWriter(dlFile, false).use { writer -> writer.write(json) }
                } catch (ignored: Exception) {}
            }

            file
        } catch (e: Exception) {
            Log.e("ProfileManager", "Failed to auto-export profile: ${e.message}")
            null
        }
    }

    fun saveToPrefs(context: Context, profile: AppProfileData) {
        val json = serializeToJson(profile)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("app_profile_json", json).apply()
        
        // Save to internal app storage file
        try {
            val file = File(context.filesDir, DEFAULT_PROFILE_FILENAME)
            FileWriter(file).use { writer ->
                writer.write(json)
            }
        } catch (e: Exception) {
            Log.e("ProfileManager", "Error saving internal profile file: ${e.message}")
        }

        // Save to public Downloads directory for disk access
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val file = File(downloadsDir, DEFAULT_PROFILE_FILENAME)
            FileWriter(file).use { writer ->
                writer.write(json)
            }
        } catch (e: Exception) {
            Log.e("ProfileManager", "Error saving public downloads profile backup: ${e.message}")
        }
    }

    fun loadFromPrefs(context: Context): AppProfileData? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString("app_profile_json", null)
            ?: loadFromInternalFile(context)
            ?: loadFromPublicDownloadsFile()
            ?: return null

        return deserializeFromJson(json)
    }

    private fun loadFromInternalFile(context: Context): String? {
        return try {
            val file = File(context.filesDir, DEFAULT_PROFILE_FILENAME)
            if (file.exists()) {
                FileReader(file).use { reader -> reader.readText() }
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun loadFromPublicDownloadsFile(): String? {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, DEFAULT_PROFILE_FILENAME)
            if (file.exists()) {
                FileReader(file).use { reader -> reader.readText() }
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun exportProfileToFile(context: Context, profile: AppProfileData): File? {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            recordUserExportDir(context, downloadsDir.absolutePath)
            val file = File(downloadsDir, DEFAULT_PROFILE_FILENAME)
            val json = serializeToJson(profile)
            FileWriter(file, false).use { writer ->
                writer.write(json)
            }
            file
        } catch (e: Exception) {
            Log.e("ProfileManager", "Failed to export profile file: ${e.message}")
            null
        }
    }

    fun saveProfileToUri(context: Context, uri: android.net.Uri, profile: AppProfileData): Boolean {
        return try {
            val json = serializeToJson(profile)
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(json.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            Log.e("ProfileManager", "Failed to write profile to Uri: ${e.message}")
            false
        }
    }

    fun loadProfileFromUri(context: Context, uri: android.net.Uri): AppProfileData? {
        return try {
            val json = context.contentResolver.openInputStream(uri)?.use { isStream ->
                isStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } ?: return null
            deserializeFromJson(json)
        } catch (e: Exception) {
            Log.e("ProfileManager", "Failed to read profile from Uri: ${e.message}")
            null
        }
    }

    fun importProfileFromFile(file: File): AppProfileData? {
        return try {
            if (!file.exists()) return null
            val json = FileReader(file).use { reader -> reader.readText() }
            deserializeFromJson(json)
        } catch (e: Exception) {
            Log.e("ProfileManager", "Failed to import profile file: ${e.message}")
            null
        }
    }

    fun serializeToJson(profile: AppProfileData): String {
        val root = JSONObject()
        root.put("version", profile.profileVersion)
        root.put("timestamp", profile.timestamp)
        root.put("isVoiceEnabled", profile.isVoiceEnabled)
        root.put("appTheme", profile.appTheme)
        root.put("isHandsFreeEnabled", profile.isHandsFreeEnabled)
        root.put("wakePhrase", profile.wakePhrase)
        root.put("readyResponse", profile.readyResponse)
        if (profile.savedDeviceAddress != null) root.put("savedDeviceAddress", profile.savedDeviceAddress)
        if (profile.savedDeviceName != null) root.put("savedDeviceName", profile.savedDeviceName)

        // Alerts
        val alertsArray = JSONArray()
        profile.alerts.forEach { alert ->
            val alertObj = JSONObject().apply {
                put("id", alert.id)
                put("sensorPid", alert.sensorPid)
                put("sensorName", alert.sensorName)
                put("condition", alert.condition.name)
                put("thresholdValue", alert.thresholdValue)
                put("unit", alert.unit)
                put("customMessage", alert.customMessage)
                put("isEnabled", alert.isEnabled)
                put("repeatIntervalSeconds", alert.repeatIntervalSeconds)
            }
            alertsArray.put(alertObj)
        }
        root.put("alerts", alertsArray)

        // Chat Messages (Retain only last 7 days, delete older messages)
        val cutoffWeekMs = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000L
        val chatArray = JSONArray()
        profile.chatMessages.filter { it.timestamp >= cutoffWeekMs }.forEach { msg ->
            val msgObj = JSONObject().apply {
                put("id", msg.id)
                put("sender", msg.sender.name)
                put("text", msg.text)
                put("timestamp", msg.timestamp)
            }
            chatArray.put(msgObj)
        }
        root.put("chatMessages", chatArray)

        // Tile Configs
        val tileConfigsArray = JSONArray()
        profile.tileConfigs.forEach { cfg ->
            val cfgObj = JSONObject().apply {
                put("slotIndex", cfg.slotIndex)
                put("sensorPid", cfg.sensorPid)
                put("displayStyle", cfg.displayStyle.name)
                put("graphTimeRangeMinutes", cfg.graphTimeRangeMinutes)
            }
            tileConfigsArray.put(cfgObj)
        }
        root.put("tileConfigs", tileConfigsArray)

        // Vehicle info
        val vObj = JSONObject().apply {
            put("vin", profile.vehicleInfo.vin)
            put("make", profile.vehicleInfo.make)
            put("model", profile.vehicleInfo.model)
            put("year", profile.vehicleInfo.year)
            put("engine", profile.vehicleInfo.engine)
            put("transmission", profile.vehicleInfo.transmission)
            put("protocol", profile.vehicleInfo.protocol)
        }
        root.put("vehicleInfo", vObj)

        // Sensor graph periods
        val periodsObj = JSONObject()
        profile.sensorGraphPeriods.forEach { (pid, sec) ->
            periodsObj.put(pid, sec)
        }
        root.put("sensorGraphPeriods", periodsObj)

        return root.toString(2)
    }

    fun deserializeFromJson(jsonStr: String): AppProfileData? {
        return try {
            val root = JSONObject(jsonStr)
            val isVoiceEnabled = root.optBoolean("isVoiceEnabled", true)
            val appTheme = root.optString("appTheme", "DARK_SPORT")
            val isHandsFreeEnabled = root.optBoolean("isHandsFreeEnabled", false)
            val wakePhrase = root.optString("wakePhrase", "Автоскан")
            val readyResponse = root.optString("readyResponse", "Слушаю вас")
            val savedDeviceAddress = if (root.has("savedDeviceAddress") && !root.isNull("savedDeviceAddress")) root.optString("savedDeviceAddress") else null
            val savedDeviceName = if (root.has("savedDeviceName") && !root.isNull("savedDeviceName")) root.optString("savedDeviceName") else null

            // Tile Configs
            val tileConfigs = mutableListOf<DashboardTileConfig>()
            val tilesArray = root.optJSONArray("tileConfigs")
            if (tilesArray != null) {
                for (i in 0 until tilesArray.length()) {
                    val tObj = tilesArray.optJSONObject(i) ?: continue
                    val slotIndex = tObj.optInt("slotIndex", i)
                    val sensorPid = tObj.optString("sensorPid", "010D")
                    val styleStr = tObj.optString("displayStyle", "DIGITAL")
                    val style = try { TileDisplayStyle.valueOf(styleStr) } catch (e: Exception) { TileDisplayStyle.DIGITAL }
                    val graphTimeRangeMinutes = tObj.optInt("graphTimeRangeMinutes", 5)
                    tileConfigs.add(DashboardTileConfig(slotIndex, sensorPid, style, graphTimeRangeMinutes))
                }
            }

            // Alerts
            val alerts = mutableListOf<SensorAlert>()
            val alertsArray = root.optJSONArray("alerts")
            if (alertsArray != null) {
                for (i in 0 until alertsArray.length()) {
                    val alertObj = alertsArray.optJSONObject(i) ?: continue
                    val condStr = alertObj.optString("condition", "GREATER_THAN")
                    val condition = try { AlertCondition.valueOf(condStr) } catch (e: Exception) { AlertCondition.GREATER_THAN }
                    alerts.add(
                        SensorAlert(
                            id = alertObj.optString("id", java.util.UUID.randomUUID().toString()),
                            sensorPid = alertObj.optString("sensorPid", "0105"),
                            sensorName = alertObj.optString("sensorName", "Датчик"),
                            condition = condition,
                            thresholdValue = alertObj.optDouble("thresholdValue", 100.0),
                            unit = alertObj.optString("unit", ""),
                            customMessage = alertObj.optString("customMessage", ""),
                            isEnabled = alertObj.optBoolean("isEnabled", true),
                            repeatIntervalSeconds = alertObj.optInt("repeatIntervalSeconds", 30)
                        )
                    )
                }
            }

            // Chat Messages
            val chatMessages = mutableListOf<ChatMessage>()
            val chatArray = root.optJSONArray("chatMessages")
            if (chatArray != null) {
                for (i in 0 until chatArray.length()) {
                    val msgObj = chatArray.optJSONObject(i) ?: continue
                    val senderStr = msgObj.optString("sender", "SYSTEM")
                    val sender = try { ChatSender.valueOf(senderStr) } catch (e: Exception) { ChatSender.SYSTEM }
                    chatMessages.add(
                        ChatMessage(
                            id = msgObj.optString("id", java.util.UUID.randomUUID().toString()),
                            sender = sender,
                            text = msgObj.optString("text", ""),
                            timestamp = msgObj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Vehicle info
            val vObj = root.optJSONObject("vehicleInfo")
            val vehicleInfo = if (vObj != null) {
                VehicleInfo(
                    vin = vObj.optString("vin", "—"),
                    make = vObj.optString("make", "ELM327 Отключен"),
                    model = vObj.optString("model", "—"),
                    year = vObj.optString("year", "—"),
                    engine = vObj.optString("engine", "—"),
                    transmission = vObj.optString("transmission", "—"),
                    protocol = vObj.optString("protocol", "—")
                )
            } else VehicleInfo()

            // Sensor graph periods
            val sensorGraphPeriods = mutableMapOf<String, Int>()
            val periodsObj = root.optJSONObject("sensorGraphPeriods")
            if (periodsObj != null) {
                val keys = periodsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    sensorGraphPeriods[k] = periodsObj.optInt(k, 120)
                }
            }

            AppProfileData(
                isVoiceEnabled = isVoiceEnabled,
                appTheme = appTheme,
                savedDeviceAddress = savedDeviceAddress,
                savedDeviceName = savedDeviceName,
                alerts = alerts,
                chatMessages = chatMessages,
                vehicleInfo = vehicleInfo,
                tileConfigs = tileConfigs,
                isHandsFreeEnabled = isHandsFreeEnabled,
                wakePhrase = wakePhrase,
                readyResponse = readyResponse,
                sensorGraphPeriods = sensorGraphPeriods
            )
        } catch (e: Exception) {
            Log.e("ProfileManager", "Failed to parse json profile: ${e.message}")
            null
        }
    }
}
