package com.example.data.logging

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.ObdSensor
import com.example.data.SensorLogSession
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SensorFileLogger(private val context: Context) {

    // 1. Separate log session triggered by Voice command (or manual button)
    // Writes all sensors that are selected in settings into a separate file.
    private var voiceLogFile: File? = null
    private var voiceFileWriter: FileWriter? = null
    private var isVoiceLoggingActive = false
    private var voiceRecordCount = 0
    private var voiceStartTimeMs = 0L
    private var lastVoiceLogWriteMs = 0L
    private var voiceHeaderPids: List<String> = emptyList()

    fun startVoiceLogging(settingsSensors: List<ObdSensor>): SensorLogSession? {
        val selectedSensors = settingsSensors.filter { it.isSelected }.ifEmpty { settingsSensors }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "AutoScan_VoiceLog_$timeStamp.csv"

        val logsDir = File(context.getExternalFilesDir(null), "SensorLogs")
        if (!logsDir.exists()) {
            logsDir.mkdirs()
        }

        val file = File(logsDir, fileName)
        return try {
            voiceFileWriter?.close()
            voiceLogFile = file
            voiceFileWriter = FileWriter(file, true)

            // Write CSV Header for all sensors selected in settings
            val header = "Timestamp," + selectedSensors.joinToString(",") { it.name.replace(",", " ") } + "\n"
            voiceFileWriter?.write(header)
            voiceFileWriter?.flush()

            isVoiceLoggingActive = true
            voiceRecordCount = 0
            voiceStartTimeMs = System.currentTimeMillis()
            voiceHeaderPids = selectedSensors.map { it.pid }
            lastVoiceLogWriteMs = 0L

            SensorLogSession(
                fileName = fileName,
                timestamp = voiceStartTimeMs,
                recordCount = 0,
                durationSeconds = 0
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun logVoiceSensors(sensors: List<ObdSensor>) {
        if (!isVoiceLoggingActive || voiceFileWriter == null) return

        val now = System.currentTimeMillis()
        if (now - lastVoiceLogWriteMs < 500L) return

        val selected = sensors.filter { it.pid in voiceHeaderPids || it.isSelected }
        if (selected.isEmpty()) return

        val timeFormatted = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(now))
        val lineValues = if (voiceHeaderPids.isNotEmpty()) {
            voiceHeaderPids.map { pid ->
                selected.find { it.pid == pid }?.value ?: 0.0
            }
        } else {
            selected.map { it.value }
        }

        val line = timeFormatted + "," + lineValues.joinToString(",") { it.toString() } + "\n"

        try {
            voiceFileWriter?.write(line)
            voiceFileWriter?.flush()
            voiceRecordCount++
            lastVoiceLogWriteMs = now
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopVoiceLogging(): SensorLogSession? {
        if (!isVoiceLoggingActive && voiceFileWriter == null) return null

        val duration = (System.currentTimeMillis() - voiceStartTimeMs) / 1000
        val file = voiceLogFile
        val fileName = file?.name ?: "VoiceLog.csv"

        try {
            voiceFileWriter?.flush()
            voiceFileWriter?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        voiceFileWriter = null
        isVoiceLoggingActive = false
        val count = voiceRecordCount
        voiceRecordCount = 0
        voiceHeaderPids = emptyList()

        return if (file != null) {
            SensorLogSession(
                fileName = fileName,
                timestamp = voiceStartTimeMs,
                recordCount = count,
                durationSeconds = duration
            )
        } else null
    }

    fun isVoiceLogging(): Boolean = isVoiceLoggingActive

    // Aliases for manual UI logging
    fun startLogging(sensors: List<ObdSensor>): SensorLogSession? = startVoiceLogging(sensors)
    fun logCurrentSensors(sensors: List<ObdSensor>) = logVoiceSensors(sensors)
    fun stopLogging(): SensorLogSession? = stopVoiceLogging()
    fun isLogging(): Boolean = isVoiceLoggingActive

    fun getLogFiles(): List<File> {
        val logsDir = File(context.getExternalFilesDir(null), "SensorLogs")
        return logsDir.listFiles()?.filter { it.extension == "csv" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun getLogDurationText(file: File): String {
        return try {
            var firstTimeMs: Long? = null
            var lastTimeMs: Long? = null
            var rowCount = 0

            val timeFormats = listOf(
                SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()),
                SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            )

            file.bufferedReader().useLines { lines ->
                val it = lines.iterator()
                if (it.hasNext()) it.next() // Skip header
                while (it.hasNext()) {
                    val line = it.next().trim()
                    if (line.isNotEmpty()) {
                        rowCount++
                        val timeStr = line.substringBefore(",")
                        var parsed: Long? = null
                        for (fmt in timeFormats) {
                            try {
                                parsed = fmt.parse(timeStr)?.time
                                if (parsed != null) break
                            } catch (e: Exception) {}
                        }
                        if (parsed != null) {
                            if (firstTimeMs == null) firstTimeMs = parsed
                            lastTimeMs = parsed
                        }
                    }
                }
            }

            val durationSec = if (firstTimeMs != null && lastTimeMs != null && lastTimeMs!! >= firstTimeMs!!) {
                (lastTimeMs!! - firstTimeMs!!) / 1000L
            } else {
                rowCount.toLong()
            }

            when {
                durationSec >= 3600 -> "${durationSec / 3600} ч ${(durationSec % 3600) / 60} мин"
                durationSec >= 60 -> "${durationSec / 60} мин ${durationSec % 60} сек"
                durationSec > 0 -> "$durationSec сек"
                else -> "< 10 сек"
            }
        } catch (e: Exception) {
            "0 сек"
        }
    }

    fun deleteLogFile(file: File): Boolean {
        return try {
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteLogFileByName(fileName: String): Boolean {
        val logs = getLogFiles()
        val match = logs.find { it.name == fileName }
        return if (match != null) deleteLogFile(match) else false
    }

    fun shareLogFile(file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Поделиться ЛОГ-файлом датчиков"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Continuous background logging for the 4 Dashboard Sensors for every trip
    private var continuousLogFile: File? = null
    private var continuousFileWriter: FileWriter? = null
    private var continuousStartTimeMs = 0L
    private var continuousRecordCount = 0
    private var lastContinuousLogWriteMs = 0L
    private var continuousHeaderPids: List<String> = emptyList()

    fun logTripContinuously(dashboardSensors: List<ObdSensor>): SensorLogSession? {
        if (dashboardSensors.isEmpty()) return null

        val now = System.currentTimeMillis()
        if (now - lastContinuousLogWriteMs < 1000L && continuousFileWriter != null) {
            return null
        }

        val currentPids = dashboardSensors.map { it.pid }

        // Start new continuous file if needed or after 30 min
        if (continuousFileWriter == null || continuousHeaderPids != currentPids || (now - continuousStartTimeMs > 30 * 60 * 1000L)) {
            closeContinuousLogging()
            startContinuousLogging(dashboardSensors)
        }

        val timeFormatted = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(now))
        val line = timeFormatted + "," + dashboardSensors.joinToString(",") { it.value.toString() } + "\n"

        try {
            continuousFileWriter?.write(line)
            continuousFileWriter?.flush()
            continuousRecordCount++
            lastContinuousLogWriteMs = now
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun logContinuously(sensors: List<ObdSensor>): SensorLogSession? = logTripContinuously(sensors)

    private fun startContinuousLogging(dashboardSensors: List<ObdSensor>) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "AutoScan_Trip_$timeStamp.csv"
        val logsDir = File(context.getExternalFilesDir(null), "SensorLogs").apply { if (!exists()) mkdirs() }
        val file = File(logsDir, fileName)
        try {
            continuousLogFile = file
            continuousFileWriter = FileWriter(file, true)
            val header = "Timestamp," + dashboardSensors.joinToString(",") { it.name.replace(",", " ") } + "\n"
            continuousFileWriter?.write(header)
            continuousFileWriter?.flush()
            continuousStartTimeMs = System.currentTimeMillis()
            continuousRecordCount = 0
            continuousHeaderPids = dashboardSensors.map { it.pid }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun closeContinuousLogging(): SensorLogSession? {
        if (continuousFileWriter == null) return null
        val duration = (System.currentTimeMillis() - continuousStartTimeMs) / 1000
        val file = continuousLogFile
        val fileName = file?.name ?: "TripLog.csv"
        try {
            continuousFileWriter?.flush()
            continuousFileWriter?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        continuousFileWriter = null
        continuousLogFile = null
        val count = continuousRecordCount
        continuousRecordCount = 0
        continuousHeaderPids = emptyList()

        return if (file != null && count > 0) {
            SensorLogSession(
                fileName = fileName,
                timestamp = continuousStartTimeMs,
                recordCount = count,
                durationSeconds = duration
            )
        } else null
    }

    /**
     * Returns existing real trip and voice log files without generating synthetic demo files.
     */
    fun ensureSampleTripLogsExist(): List<File> {
        return getLogFiles()
    }
}
