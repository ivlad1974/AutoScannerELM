package com.example.data.logging

import androidx.compose.ui.graphics.Color
import java.io.File
import java.util.Locale
import kotlin.random.Random

data class LogDataPoint(
    val timestampMs: Long,
    val timeLabel: String,
    val value: Double
)

data class LogSensorSeries(
    val sensorName: String,
    val unit: String,
    val color: Color,
    val points: List<LogDataPoint>
) {
    val minValue: Double = points.minOfOrNull { it.value } ?: 0.0
    val maxValue: Double = points.maxOfOrNull { it.value } ?: 0.0
    val avgValue: Double = if (points.isNotEmpty()) points.map { it.value }.average() else 0.0
}

data class ParsedLogSession(
    val fileName: String,
    val totalDurationSeconds: Long,
    val totalRecords: Int,
    val seriesList: List<LogSensorSeries>
)

object LogDataParser {

    private val sensorColorPalette = listOf(
        Color(0xFF00E5FF), // Cyan
        Color(0xFF00E676), // Neon Green
        Color(0xFFFFD600), // Amber
        Color(0xFFFF5252), // Coral Red
        Color(0xFFE040FB), // Magenta/Purple
        Color(0xFF448AFF), // Royal Blue
        Color(0xFFFF9100), // Deep Orange
        Color(0xFF1DE9B6)  // Teal
    )

    fun parseCsvFile(file: File): ParsedLogSession? {
        return try {
            if (!file.exists()) return null
            val rawLines = file.readLines().map { it.replace("\uFEFF", "").trim() }.filter { it.isNotBlank() }
            if (rawLines.size < 2) return null

            val firstLine = rawLines.first()
            val delimiter = if (firstLine.contains(";")) ";" else if (firstLine.contains("\t")) "\t" else ","

            val header = firstLine.split(delimiter).map { it.replace("\"", "").trim() }
            if (header.size < 2) return null

            val sensorNames = header.drop(1)
            // Filter out any repeated header lines from append-mode logging
            val dataRows = rawLines.drop(1).filter { line ->
                val firstCol = line.split(delimiter).firstOrNull()?.replace("\"", "")?.trim()
                firstCol != null && !firstCol.equals("Timestamp", ignoreCase = true) && !firstCol.equals("Время", ignoreCase = true)
            }
            if (dataRows.isEmpty()) return null

            // Pre-split rows into columns for fast synchronized processing
            val parsedRows = dataRows.map { row ->
                row.split(delimiter).map { it.replace("\"", "").trim() }
            }

            val seriesData = mutableListOf<LogSensorSeries>()

            sensorNames.forEachIndexed { sensorIdx, rawSensorName ->
                val sensorName = rawSensorName.ifBlank { "Датчик ${sensorIdx + 1}" }
                val colIdx = sensorIdx + 1

                // Check if this sensor has ANY recorded numeric values in the file
                var hasRecordedValues = false
                var firstValidVal: Double? = null

                for (cols in parsedRows) {
                    val cell = cols.getOrNull(colIdx) ?: ""
                    if (cell.isNotBlank() && cell != "--" && cell != "null" && !cell.equals("NaN", ignoreCase = true) && cell != "None" && cell != "N/A") {
                        val d = cell.toDoubleOrNull()
                        if (d != null && d.isFinite()) {
                            hasRecordedValues = true
                            if (firstValidVal == null) firstValidVal = d
                        }
                    }
                }

                // If sensor was not recorded, hide it!
                if (!hasRecordedValues || firstValidVal == null) {
                    return@forEachIndexed
                }

                val initialVal: Double = firstValidVal!!
                val points = ArrayList<LogDataPoint>(parsedRows.size)
                var lastValidVal: Double = initialVal

                parsedRows.forEachIndexed { rowIdx, cols ->
                    val timeStr = cols.getOrNull(0)?.ifBlank { null } ?: String.format(Locale.US, "%02d:%02d", rowIdx / 60, rowIdx % 60)
                    val cell = cols.getOrNull(colIdx) ?: ""

                    if (cell.isNotBlank() && cell != "--" && cell != "null" && !cell.equals("NaN", ignoreCase = true) && cell != "None" && cell != "N/A") {
                        val d = cell.toDoubleOrNull()
                        if (d != null && d.isFinite()) {
                            lastValidVal = d
                        }
                    }

                    points.add(
                        LogDataPoint(
                            timestampMs = rowIdx * 500L,
                            timeLabel = timeStr,
                            value = lastValidVal
                        )
                    )
                }

                val unit = detectUnit(sensorName)
                val color = sensorColorPalette[seriesData.size % sensorColorPalette.size]
                seriesData.add(
                    LogSensorSeries(
                        sensorName = sensorName,
                        unit = unit,
                        color = color,
                        points = points
                    )
                )
            }

            if (seriesData.isEmpty()) return null

            val totalDuration = if (dataRows.size > 1) (dataRows.size * 500L) / 1000L else 0L

            ParsedLogSession(
                fileName = file.name,
                totalDurationSeconds = totalDuration,
                totalRecords = dataRows.size,
                seriesList = seriesData
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun detectUnit(sensorName: String): String {
        val lower = sensorName.lowercase()
        return when {
            lower.contains("оборот") || lower.contains("rpm") -> "об/мин"
            lower.contains("скорост") || lower.contains("speed") -> "км/ч"
            lower.contains("температур") || lower.contains("temp") -> "°C"
            lower.contains("напряжен") || lower.contains("volt") -> "В"
            lower.contains("расход") || lower.contains("fuel") -> "л/100км"
            lower.contains("дроссел") || lower.contains("уровень") || lower.contains("%") -> "%"
            lower.contains("давлен") || lower.contains("bar") -> "бар"
            else -> ""
        }
    }
}
