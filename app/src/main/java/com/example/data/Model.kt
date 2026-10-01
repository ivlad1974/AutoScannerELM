package com.example.data

data class VehicleInfo(
    val vin: String = "Адаптер не подключен",
    val make: String = "ELM327 Отключен",
    val model: String = "—",
    val year: String = "—",
    val engine: String = "—",
    val transmission: String = "—",
    val protocol: String = "—",
    val batteryVoltage: Double = 0.0,
    val totalEcusFound: Int = 0,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val isIgnitionOn: Boolean = false
)

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED_BLUETOOTH
}

data class ObdSensor(
    val pid: String,
    val name: String,
    val value: Double,
    val unit: String,
    val minVal: Double,
    val maxVal: Double,
    val category: String,
    val isSelected: Boolean = true,
    val history: List<Pair<Long, Double>> = emptyList(),
    val description: String = "",
    // Expanded properties for full library & Torque parity
    val shortName: String = "",
    val formula: String = "",
    val ecuHeader: String = "7E0",
    val mode: String = "01",
    val bytesCount: Int = 1,
    val isSupported: Boolean = true,
    val isCustom: Boolean = false,
    val profileName: String = "Стандартный OBD-II",
    val graphPeriodSec: Int = 120
) {
    val formattedValue: String
        get() = formatSensorValue(value, pid, unit)
}

fun formatSensorValue(value: Double, pid: String = "", unit: String = ""): String {
    val unitLower = unit.lowercase()
    val pidUpper = pid.uppercase()

    if (pidUpper == "0114") {
        return String.format(java.util.Locale.US, "%.2f", value)
    }

    val requiresDecimal = pidUpper == "0142" ||
            pidUpper == "015E" ||
            pidUpper == "015F" ||
            pidUpper == "0106" ||
            pidUpper == "0107" ||
            unitLower.contains("в") || unitLower.contains("v") ||
            unitLower.contains("л/100") || unitLower.contains("г/с") ||
            unitLower.contains("бар") || unitLower.contains("bar") ||
            (value in 0.001..10.0) ||
            (value % 1.0 != 0.0 && value < 100.0)

    return if (requiresDecimal) {
        String.format(java.util.Locale.US, "%.1f", value)
    } else {
        value.toInt().toString()
    }
}

fun intToRussianWords(n: Long, feminine: Boolean = false): String {
    if (n < 0) return "минус " + intToRussianWords(-n, feminine)
    if (n == 0L) return "ноль"

    val units = arrayOf(
        "", "один", "два", "три", "четыре", "пять",
        "шесть", "семь", "восемь", "девять", "десять",
        "одиннадцать", "двенадцать", "тринадцать", "четырнадцать", "пятнадцать",
        "шестнадцать", "семнадцать", "восемнадцать", "девятнадцать"
    )
    val tens = arrayOf(
        "", "", "двадцать", "тридцать", "сорок", "пятьдесят",
        "шестьдесят", "семьдесят", "восемьдесят", "девяносто"
    )
    val hundreds = arrayOf(
        "", "сто", "двести", "триста", "четыреста", "пятьсот",
        "шестьсот", "семьсот", "восемьсот", "девятьсот"
    )

    if (n < 20) {
        if (feminine) {
            if (n == 1L) return "одна"
            if (n == 2L) return "две"
        }
        return units[n.toInt()]
    }
    if (n < 100) {
        val rem = (n % 10).toInt()
        val t = tens[(n / 10).toInt()]
        if (rem == 0) return t
        val lastWord = if (feminine) {
            if (rem == 1) "одна" else if (rem == 2) "две" else units[rem]
        } else {
            units[rem]
        }
        return "$t $lastWord"
    }
    if (n < 1000) {
        val h = hundreds[(n / 100).toInt()]
        val rem = n % 100
        return if (rem == 0L) h else "$h ${intToRussianWords(rem, feminine)}"
    }
    if (n < 1_000_000) {
        val thousands = n / 1000
        val rem = n % 1000
        val thWord = when {
            thousands % 100 in 11..19 -> "тысяч"
            thousands % 10 == 1L -> "тысяча"
            thousands % 10 in 2..4 -> "тысячи"
            else -> "тысяч"
        }
        val thStr = intToRussianWords(thousands, feminine = true)
        return if (rem == 0L) "$thStr $thWord" else "$thStr $thWord ${intToRussianWords(rem, feminine)}"
    }
    return n.toString()
}

fun tenthsToRussianWords(digit: Int): String {
    return when (digit) {
        1 -> "одна десятая"
        2 -> "две десятых"
        3 -> "три десятых"
        4 -> "четыре десятых"
        5 -> "пять десятых"
        6 -> "шесть десятых"
        7 -> "семь десятых"
        8 -> "восемь десятых"
        9 -> "девять десятых"
        else -> "ноль десятых"
    }
}

fun hundredthsToRussianWords(val100: Int): String {
    val rem100 = val100 % 100
    val rem10 = val100 % 10
    val words = intToRussianWords(val100.toLong(), feminine = true)
    val decl = when {
        rem100 in 11..19 -> "сотых"
        rem10 == 1 -> "сотая"
        else -> "сотых"
    }
    return "$words $decl"
}

fun thousandthsToRussianWords(val1000: Int): String {
    val rem100 = val1000 % 100
    val rem10 = val1000 % 10
    val words = intToRussianWords(val1000.toLong(), feminine = true)
    val decl = when {
        rem100 in 11..19 -> "тысячных"
        rem10 == 1 -> "тысячная"
        else -> "тысячных"
    }
    return "$words $decl"
}

fun formatDecimalNumberToRussianWords(rawInt: Long, rawFrac: String, signPrefix: String = ""): String {
    val absInt = kotlin.math.abs(rawInt)
    val intWord = intToRussianWords(absInt, feminine = false)
    val strippedFrac = rawFrac.trimEnd('0')

    if (strippedFrac.isEmpty()) {
        return "$signPrefix$intWord"
    }

    return when (strippedFrac.length) {
        1 -> {
            val digit = strippedFrac.toIntOrNull() ?: 0
            val tenthWord = tenthsToRussianWords(digit)
            "$signPrefix$intWord и $tenthWord"
        }
        2 -> {
            val val100 = strippedFrac.toIntOrNull() ?: 0
            val hundredthWord = hundredthsToRussianWords(val100)
            "$signPrefix$intWord и $hundredthWord"
        }
        3 -> {
            val val1000 = strippedFrac.toIntOrNull() ?: 0
            val thousandthWord = thousandthsToRussianWords(val1000)
            "$signPrefix$intWord и $thousandthWord"
        }
        else -> {
            val fracDbl = ("0.$strippedFrac").toDoubleOrNull() ?: 0.0
            val rounded = kotlin.math.round(fracDbl * 100.0) / 100.0
            val fracStr = String.format(java.util.Locale.US, "%.2f", rounded).substringAfter(".")
            formatDecimalNumberToRussianWords(absInt, fracStr, signPrefix)
        }
    }
}

fun degreeDeclension(n: Long): String {
    val absN = kotlin.math.abs(n)
    val rem100 = absN % 100
    val rem10 = absN % 10
    if (rem100 in 11..19) return "градусов"
    return when (rem10) {
        1L -> "градус"
        2L, 3L, 4L -> "градуса"
        else -> "градусов"
    }
}

fun declineRussianWord(number: Double, one: String, twoFour: String, fiveMany: String): String {
    val absVal = kotlin.math.abs(number)
    val isFractional = (absVal % 1.0) >= 0.001
    if (isFractional) {
        return twoFour // e.g. 14.2 вольта, 8.5 литра, 3.4 метра в секунду, 0.8 бара
    }
    val rounded = absVal.toLong()
    val rem100 = rounded % 100
    val rem10 = rounded % 10
    if (rem100 in 11..19) return fiveMany
    return when (rem10) {
        1L -> one
        2L, 3L, 4L -> twoFour
        else -> fiveMany
    }
}

fun declineUnitByPrecedingWord(prevWordOrNumber: String, one: String, twoFour: String, fiveMany: String): String {
    val clean = prevWordOrNumber.trim().lowercase().replace(",", ".")
    val num = clean.toDoubleOrNull()
    if (num != null) {
        return declineRussianWord(num, one, twoFour, fiveMany)
    }
    if (clean.endsWith("один") || clean.endsWith("одна") || clean == "1") return one
    if (clean.endsWith("два") || clean.endsWith("две") || clean.endsWith("три") || clean.endsWith("четыре") ||
        clean.endsWith("десятых") || clean.endsWith("сотых") || clean.endsWith("тысячных")) return twoFour
    return fiveMany
}

/**
 * Formats sensor reading specifically for Russian voice/TTS output:
 * Rule from user:
 * "градусы можно вообще только целую часть говорить"
 * "все показания озвучивать только целыми числами, кроме чисел меньше 10 целых, либо напряжения.
 * при озвучивании показаний датчиков, необходимо говорить десятые например так: 15.6 «пятнадцать и шесть десятых»"
 */
fun formatSensorValueForSpeech(value: Double, pid: String = "", unit: String = ""): String {
    val unitLower = unit.lowercase()
    val pidUpper = pid.uppercase()

    val isTemperature = pidUpper == "0105" || pidUpper == "010F" ||
            unitLower.contains("°") || unitLower.contains("градус")

    val isVoltage = !isTemperature && (
            pidUpper == "0142" ||
            unitLower.contains("в") || unitLower.contains("v") ||
            unitLower.contains("вольт")
    )

    val absVal = kotlin.math.abs(value)
    val isLessThanTen = absVal < 10.0

    // Temperature is ALWAYS voiced as whole integer per user rule:
    // "градусы можно вообще только целую часть говорить"
    if (isTemperature) {
        val rounded = kotlin.math.round(value).toLong()
        return intToRussianWords(rounded)
    }

    // Rule: All other readings >= 10 are voiced as whole integers, EXCEPT voltage or values < 10.
    if (!isVoltage && !isLessThanTen) {
        val rounded = kotlin.math.round(value).toLong()
        return intToRussianWords(rounded)
    }

    // Numbers < 10 or voltage: voice with tenths
    val sign = if (value < 0) "минус " else ""
    val roundedTotalTenths = kotlin.math.round(absVal * 10.0).toLong()
    val intPart = roundedTotalTenths / 10
    val tenthsDigit = (roundedTotalTenths % 10).toInt()

    if (tenthsDigit == 0) {
        return "$sign${intToRussianWords(intPart)}"
    }

    return "$sign${intToRussianWords(intPart)} и ${tenthsToRussianWords(tenthsDigit)}"
}

data class DtcError(
    val code: String,
    val category: String, // Powertrain, Chassis, Body, Network
    val ecuName: String, // Engine ECU, Transmission TCU, ABS/ESP, Airbag SRS
    val description: String,
    val severity: DtcSeverity,
    val possibleCauses: List<String> = emptyList(),
    val symptoms: List<String> = emptyList(),
    val isPending: Boolean = false
)

enum class DtcSeverity {
    CRITICAL, // High severity (e.g. engine misfire damaging catalytic converter, oil pressure)
    WARNING,  // Moderate (sensor out of range, lean condition)
    MINOR     // Informational / Pending
}

data class EcuBlock(
    val id: String,
    val name: String,
    val description: String,
    val status: EcuStatus = EcuStatus.NOT_SCANNED,
    val errorCount: Int = 0,
    val header: String = "7E0",
    val txHeader: String = "7E0",
    val rxHeader: String = "7E8",
    val supportedPidCount: Int = 0,
    val dtcCodes: List<String> = emptyList()
)

data class SupportedPidsResult(
    val supportedPids: Set<String> = emptySet(),
    val totalSupportedCount: Int = 0,
    val mode09SupportedPids: Set<String> = emptySet(),
    val mode06SupportedTests: Set<String> = emptySet(),
    val discoveredHeaders: List<String> = listOf("7E0")
)

data class Mode06TestItem(
    val testId: String,
    val name: String,
    val component: String,
    val value: Double = 0.0,
    val minLimit: Double? = null,
    val maxLimit: Double? = null,
    val unit: String = "",
    val isPassed: Boolean = true
)

enum class EcuStatus {
    NOT_SCANNED,
    SCANNING,
    OK,
    HAS_ERRORS,
    UNRESPONSIVE
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: ChatSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isVoiceSpoken: Boolean = false,
    val relatedDtcCodes: List<String> = emptyList()
)

enum class ChatSender {
    USER,
    AI,
    SYSTEM
}

enum class AlertCondition {
    GREATER_THAN, // Превышение ( > )
    LESS_THAN     // Падение ( < )
}

data class SensorAlert(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sensorPid: String,
    val sensorName: String,
    val condition: AlertCondition,
    val thresholdValue: Double,
    val unit: String,
    val customMessage: String = "",
    var isEnabled: Boolean = true,
    var lastTriggeredTime: Long = 0L,
    val repeatIntervalSeconds: Int = 30 // Повторное оповещение каждые 30 секунд по умолчанию
)

data class SensorLogSession(
    val id: Long = 0,
    val fileName: String,
    val timestamp: Long,
    val recordCount: Int,
    val durationSeconds: Long
)

enum class TileDisplayStyle {
    DIGITAL,      // Крупная цифра + шкала прогресса
    GAUGE_HUD,    // Круговой полукруглый HUD прибор со стрелкой
    GRAPH_WAVE,   // Живой волновой график в реальном времени
    BAR_THERMAL   // Линейный бар / термометр со статусными зонами
}

data class DashboardTileConfig(
    val slotIndex: Int,
    val sensorPid: String,
    val displayStyle: TileDisplayStyle = TileDisplayStyle.GRAPH_WAVE,
    val graphTimeRangeMinutes: Int = 5
)
