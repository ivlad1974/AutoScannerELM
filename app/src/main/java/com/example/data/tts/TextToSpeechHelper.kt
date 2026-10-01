package com.example.data.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import com.example.data.intToRussianWords
import com.example.data.tenthsToRussianWords
import com.example.data.degreeDeclension
import com.example.data.formatDecimalNumberToRussianWords
import com.example.data.declineRussianWord
import com.example.data.declineUnitByPrecedingWord

class TextToSpeechHelper(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isVoiceEnabled = MutableStateFlow(true)
    val isVoiceEnabled: StateFlow<Boolean> = _isVoiceEnabled.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var playbackMonitorJob: Job? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("ru", "RU"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = Locale.US
            }
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    startPlaybackMonitor()
                }

                override fun onDone(utteranceId: String?) {
                    // Check if TTS engine is still physically playing audio before clearing state
                    scope.launch {
                        delay(150L)
                        val busy = try { tts?.isSpeaking == true } catch (e: Exception) { false }
                        if (!busy) {
                            _isSpeaking.value = false
                            playbackMonitorJob?.cancel()
                        }
                    }
                }

                override fun onError(utteranceId: String?) {
                    scope.launch {
                        delay(150L)
                        val busy = try { tts?.isSpeaking == true } catch (e: Exception) { false }
                        if (!busy) {
                            _isSpeaking.value = false
                            playbackMonitorJob?.cancel()
                        }
                    }
                }
            })
            isInitialized = true
        } else {
            Log.e("TextToSpeechHelper", "TTS Initialization failed")
        }
    }

    private fun startPlaybackMonitor() {
        playbackMonitorJob?.cancel()
        playbackMonitorJob = scope.launch {
            var silentChecks = 0
            while (isActive) {
                delay(120L)
                val engineBusy = try { tts?.isSpeaking == true } catch (e: Exception) { false }
                if (engineBusy) {
                    silentChecks = 0
                    if (!_isSpeaking.value) {
                        _isSpeaking.value = true
                    }
                } else {
                    silentChecks++
                    // Require at least 2 consecutive idle checks (~240ms) to prevent flickering
                    if (silentChecks >= 2) {
                        _isSpeaking.value = false
                        break
                    }
                }
            }
        }
    }

    fun setVoiceEnabled(enabled: Boolean) {
        _isVoiceEnabled.value = enabled
        if (!enabled) {
            stop()
        }
    }

    fun speak(text: String, overrideToggle: Boolean = false) {
        if (!isInitialized) return
        if (!_isVoiceEnabled.value && !overrideToggle) return

        stopInternalTts()
        _isSpeaking.value = true

        val cleanedText = normalizeTextForSpeech(text)
        if (cleanedText.isBlank()) {
            _isSpeaking.value = false
            return
        }

        startPlaybackMonitor()

        // Speak utterance
        tts?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, null, "AutoScanSpeechId")
    }

    fun normalizeTextForSpeech(text: String): String = Companion.normalizeTextForSpeech(text)

    companion object {
        private fun replaceUnit(
            text: String,
            unitPattern: String,
            one: String,
            twoFour: String,
            fiveMany: String
        ): String {
            val regex = Regex("(?i)(?:(?<=[\\s,;:(]|^)([а-яА-Яa-zA-Z0-9.,]+)\\s*)?(?:$unitPattern)(?![a-zA-Zа-яА-Я0-9])")
            return regex.replace(text) { m ->
                val prev = m.groupValues[1].trim()
                if (prev.isNotEmpty()) {
                    val decl = declineUnitByPrecedingWord(prev, one, twoFour, fiveMany)
                    "$prev $decl"
                } else {
                    fiveMany
                }
            }
        }

        fun normalizeTextForSpeech(text: String): String {
            if (text.isBlank()) return ""

            var result = text

            // 1. Remove markdown links: [label](url) -> label
            result = result.replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1")

            // 2. Remove code blocks ```...``` and inline backticks
            result = result.replace(Regex("```[a-zA-Z]*\\s*([\\s\\S]*?)```"), "$1")
            result = result.replace("`", "")

            // 2b. Clean markdown bullets and formatting first so math signs (*, +, -) are not confused with markdown
            result = result.replace(Regex("(?m)^\\s*[-*+•–—]+\\s*"), "")
            result = result.replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
            result = result.replace(Regex("(?<![a-zA-Z0-9])\\*([^\\s*]+)\\*(?![a-zA-Z0-9])"), "$1")

            // 3. Normalize all dash and minus variations into standard '-' before any processing:
            // Converts en-dash (–), em-dash (—), minus sign (− U+2212), figure dash (‒), non-breaking hyphen (‑), and tilde (~)
            result = result
                .replace('–', '-')
                .replace('—', '-')
                .replace('−', '-')
                .replace('‒', '-')
                .replace('‑', '-')
                .replace('~', '-')

            // 3b. Remove grouping spaces inside thousands: e.g. "7 500" -> "7500", "10 000" -> "10000"
            result = result.replace(Regex("(?<=\\d)\\s+(?=\\d{3}\\b)"), "")

            // 4. CRITICAL: Handle RANGES BEFORE temperature, math, or minus-sign detection!
            // When TTS encounters ranges like "22-24", "22 - 24", "22-24°C", "7500-10000", it must NEVER say "минус".
            // It must say "от ... до ...", e.g. "от 22 до 24".

            // 4a. Ranges with temperature or degree symbol: e.g. "22-24°C", "22 - 24 °C", "22-24 градуса", "от 22 до 24°"
            val tempRangeRegex = Regex(
                "(?:(?:в диапазоне|в пределах|от)\\s+)?(\\d+(?:[.,]\\d+)?)\\s*(?:°\\s*[CСcс]?|градус[а-яА-Я]*)?\\s*-\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:°\\s*[CСcс]?|градус[а-яА-Я]*(?:\\s+Цельсия)?)",
                RegexOption.IGNORE_CASE
            )
            result = tempRangeRegex.replace(result) { m ->
                val fromVal = m.groupValues[1]
                val toVal = m.groupValues[2]
                val fromLong = fromVal.toDoubleOrNull()?.let { kotlin.math.round(it).toLong() } ?: 0L
                val toLong = toVal.toDoubleOrNull()?.let { kotlin.math.round(it).toLong() } ?: 0L
                val decl = degreeDeclension(toLong)
                val hasCelsius = Regex("°\\s*[CcСс]|Цельси", RegexOption.IGNORE_CASE).containsMatchIn(m.value)
                val suffix = if (hasCelsius) " Цельсия" else ""
                " от ${intToRussianWords(fromLong)} до ${intToRussianWords(toLong)} $decl$suffix "
            }

            // 4b. Subtraction in formulas ONLY when explicitly followed by equals sign: e.g. "10 - 4 = 6" -> "10 минус 4 равно 6"
            result = result.replace(Regex("(\\d+(?:[.,]\\d+)?)\\s*-\\s*(\\d+(?:[.,]\\d+)?)(?=\\s*=)"), "$1 минус $2")

            // 4c. Explicit ranges with words: "диапазон 22-24", "в диапазоне 22-24", "в пределах 22-24", "от 22-24"
            result = result.replace(
                Regex("(?i)\\b(?:диапазон(?:е)?|в диапазоне|в пределах)\\s+(?:от\\s+)?(\\d+(?:[.,]\\d+)?)\\s*-\\s*(\\d+(?:[.,]\\d+)?)"),
                " в диапазоне от $1 до $2 "
            )
            result = result.replace(
                Regex("(?i)\\bот\\s+(\\d+(?:[.,]\\d+)?)\\s*-\\s*(\\d+(?:[.,]\\d+)?)"),
                " от $1 до $2 "
            )

            // 4d. General numeric ranges between two numbers: "22-24", "22 - 24", "13.8 - 14.5", "2.0-2.4", "7500-10000", "30-45", "1-2"
            // Replaces "-" with "до" with leading "от", ensuring no minus is ever voiced for ranges!
            result = result.replace(
                Regex("(?<![a-zA-Zа-яА-Я0-9])(\\d+(?:[.,]\\d+)?)\\s*-\\s*(\\d+(?:[.,]\\d+)?)(?![a-zA-Zа-яА-Я0-9])"),
                " от $1 до $2 "
            )

            // 4e. OBD-II DTC ranges: e.g. "P0300 - P0304" -> "P0300 до P0304"
            result = result.replace(Regex("([A-Z]\\d{4})\\s*-\\s*([A-Z]\\d{4})"), "$1 до $2")

            // 4f. Clean duplicate "от": e.g. "от от 22 до 24" -> "от 22 до 24"
            result = result.replace(Regex("(?i)\\bот\\s+от\\b"), "от")

            // 5. Comparison operators before numbers: >= 20, <= 15, > 5, < 10
            result = result
                .replace(Regex(">=\\s*(-?\\d+)"), " более или равно $1")
                .replace(Regex("<=\\s*(-?\\d+)"), " менее или равно $1")
                .replace(Regex(">\\s*(-?\\d+)"), " более $1")
                .replace(Regex("<\\s*(-?\\d+)"), " менее $1")

            // 6. Formulas and arithmetic operations (*, /, +, =) for spoken comprehension:
            // Multiplication: 5 * 10, A * B, 5 × 10 -> "умножить на"
            result = result
                .replace(Regex("(\\d+(?:[.,]\\d+)?)\\s*[*×✕✖]\\s*(\\d+(?:[.,]\\d+)?)"), "$1 умножить на $2")
                .replace(Regex("([a-zA-Zа-яА-Я0-9]+)\\s*[*×✕✖]\\s*([a-zA-Zа-яА-Я0-9]+)"), "$1 умножить на $2")

            // Division in formulas: 100 / 5, P / S -> "разделить на"
            result = result
                .replace(Regex("(\\d+(?:[.,]\\d+)?)\\s*/\\s*(\\d+(?:[.,]\\d+)?)"), "$1 разделить на $2")
                .replace(Regex("([a-zA-Z])\\s*/\\s*([a-zA-Z0-9])"), "$1 разделить на $2")
                .replace(Regex("([0-9])\\s*/\\s*([a-zA-Z])"), "$1 разделить на $2")

            // Addition in formulas: 2 + 2 -> 2 плюс 2
            result = result
                .replace(Regex("(\\d+(?:[.,]\\d+)?)\\s*\\+\\s*(\\d+(?:[.,]\\d+)?)"), "$1 плюс $2")
                .replace(Regex("([a-zA-Zа-яА-Я0-9]+)\\s*\\+\\s*([a-zA-Zа-яА-Я0-9]+)"), "$1 плюс $2")

            // Equals in formulas: 2 + 2 = 4 -> 2 плюс 2 равно 4
            result = result.replace(Regex("([a-zA-Zа-яА-Я0-9]+)\\s*=\\s*([a-zA-Zа-яА-Я0-9]+)"), "$1 равно $2")

            // 7. COMPREHENSIVE UNITS OF MEASUREMENT with proper Russian grammatical declension:
            // Handles both digit-preceded units (e.g. "5 м/с", "14.2 В") and word-preceded units (e.g. "двадцать пять м/с", "восемьсот об/мин")

            // 7a. Скорость ветра, газов, датчиков: м/с (метров в секунду)
            result = replaceUnit(result, "м\\s*/\\s*с(?:ек)?|m\\s*/\\s*s", "метр в секунду", "метра в секунду", "метров в секунду")

            // 7b. Скорость автомобиля: км/ч (километров в час)
            result = replaceUnit(result, "км\\s*/\\s*ч(?:ас(?:а)?)?|km\\s*/\\s*h", "километр в час", "километра в час", "километров в час")

            // 7c. Обороты двигателя: об/мин, rpm (оборотов в минуту)
            result = replaceUnit(result, "об\\s*/\\s*мин(?:\\.)?|об\\s*\\.\\s*мин(?:\\.)?|rpm|об\\s+мин", "оборот в минуту", "оборота в минуту", "оборотов в минуту")

            // 7d. Расход топлива: л/100км (литров на сто километров)
            result = replaceUnit(result, "л\\s*/\\s*100\\s*км|l\\s*/\\s*100\\s*km", "литр на сто километров", "литра на сто километров", "литров на сто километров")

            // 7e. Расход топлива: л/ч (литров в час)
            result = replaceUnit(result, "л\\s*/\\s*ч(?:ас(?:а)?)?|l\\s*/\\s*h", "литр в час", "литра в час", "литров в час")

            // 7f. Расход воздуха ДМРВ: г/с (грамм в секунду)
            result = replaceUnit(result, "г\\s*/\\s*с(?:ек)?|g\\s*/\\s*s", "грамм в секунду", "грамма в секунду", "грамм в секунду")

            // 7g. Давление: кПа (килопаскалей), МПа (мегапаскалей)
            result = replaceUnit(result, "кПа|кпа|kpa|kPa", "килопаскаль", "килопаскаля", "килопаскалей")
            result = replaceUnit(result, "МПа|мпа|mpa|MPa", "мегапаскаль", "мегапаскаля", "мегапаскалей")

            // 7h. Давление: бар (бар, бара)
            result = replaceUnit(result, "бар(?:а)?|bar", "бар", "бара", "бар")

            // 7i. Давление: psi (фунтов на квадратный дюйм)
            result = replaceUnit(result, "psi|PSI|пси", "фунт на квадратный дюйм", "фунта на квадратный дюйм", "фунтов на квадратный дюйм")

            // 7j. Напряжение: В, V, вольт (вольт, вольта)
            result = result.replace(Regex("(?i)(?<![a-zA-Zа-яА-Я0-9])([а-яА-Яa-zA-Z0-9.,]+)\\s*(?:В|V|вольт(?:а)?)(?![a-zA-Zа-яА-Я0-9])")) { m ->
                val prev = m.groupValues[1]
                val prevLower = prev.lowercase()
                // Only treat as volts if preceded by digits or numeric words
                val isNumeric = prev.matches(Regex(".*[0-9].*")) ||
                    prevLower.endsWith("один") || prevLower.endsWith("одна") ||
                    prevLower.endsWith("два") || prevLower.endsWith("две") ||
                    prevLower.endsWith("три") || prevLower.endsWith("четыре") ||
                    prevLower.endsWith("пять") || prevLower.endsWith("шесть") ||
                    prevLower.endsWith("семь") || prevLower.endsWith("восемь") ||
                    prevLower.endsWith("девять") || prevLower.endsWith("десять") ||
                    prevLower.endsWith("дцать") || prevLower.endsWith("десят") ||
                    prevLower.endsWith("сто") || prevLower.endsWith("сорок") ||
                    prevLower.endsWith("девяносто") || prevLower.endsWith("десятых") ||
                    prevLower.endsWith("сотых") || prevLower.endsWith("тысячных")
                if (isNumeric) {
                    val decl = declineUnitByPrecedingWord(prev, "вольт", "вольта", "вольт")
                    "$prev $decl"
                } else {
                    m.value
                }
            }

            // 7k. Милливольты: мВ, mV
            result = result.replace(Regex("(?i)(\\d+(?:[.,]\\d+)?)\\s*(?:мВ|mV|милливольт(?:а)?)(?![a-zA-Zа-яА-Я0-9])"), "$1 милливольт")

            // 7l. Сила тока: А, ампер; Емкость АКБ: Ач, Ah
            result = result.replace(Regex("(?i)(\\d+(?:[.,]\\d+)?)\\s*(?:Ач|А·ч|А\\*ч|Ah|A·h|A\\*h)\\b"), "$1 ампер-часов")
            result = result.replace(Regex("(?i)(\\d+(?:[.,]\\d+)?)\\s*(?:А|A|ампер(?:а)?)(?![a-zA-Zа-яА-Я0-9])")) { m ->
                val prev = m.groupValues[1]
                val decl = declineUnitByPrecedingWord(prev, "ампер", "ампера", "ампер")
                "$prev $decl"
            }

            // 7m. Мощность: кВт, Вт, л.с.
            result = result.replace(Regex("(?i)(\\d+(?:[.,]\\d+)?)\\s*(?:л\\.?\\s*с\\.?|hp|HP)(?![a-zA-Zа-яА-Я0-9])")) { m ->
                val prev = m.groupValues[1]
                val decl = declineUnitByPrecedingWord(prev, "лошадиная сила", "лошадиные силы", "лошадиных сил")
                "$prev $decl"
            }
            result = result.replace(Regex("(?i)(\\d+(?:[.,]\\d+)?)\\s*(?:кВт|kW)(?![a-zA-Zа-яА-Я0-9])")) { m ->
                val prev = m.groupValues[1]
                val decl = declineUnitByPrecedingWord(prev, "киловатт", "киловатта", "киловатт")
                "$prev $decl"
            }
            result = result.replace(Regex("(?i)(\\d+(?:[.,]\\d+)?)\\s*(?:Вт|W)(?![a-zA-Zа-яА-Я0-9])")) { m ->
                val prev = m.groupValues[1]
                val decl = declineUnitByPrecedingWord(prev, "ватт", "ватта", "ватт")
                "$prev $decl"
            }

            // 7n. Крутящий момент: Нм
            result = result.replace(Regex("(?i)(\\d+(?:[.,]\\d+)?)\\s*(?:Нм|Н·м|Н\\*м|Nm)(?![a-zA-Zа-яА-Я0-9])")) { m ->
                val prev = m.groupValues[1]
                val decl = declineUnitByPrecedingWord(prev, "ньютон-метр", "ньютон-метра", "ньютон-метров")
                "$prev $decl"
            }

            // 7o. Размеры: мм, см, км, м
            result = result.replace(Regex("(?i)(\\d+(?:[.,]\\d+)?)\\s*(?:мм|mm)(?![a-zA-Zа-яА-Я0-9])")) { m ->
                val prev = m.groupValues[1]
                val decl = declineUnitByPrecedingWord(prev, "миллиметр", "миллиметра", "миллиметров")
                "$prev $decl"
            }
            result = result.replace(Regex("(?i)(\\d+(?:[.,]\\d+)?)\\s*(?:см|cm)(?![a-zA-Zа-яА-Я0-9])")) { m ->
                val prev = m.groupValues[1]
                val decl = declineUnitByPrecedingWord(prev, "сантиметр", "сантиметра", "сантиметров")
                "$prev $decl"
            }
            result = result.replace(Regex("(?i)(\\d+(?:[.,]\\d+)?)\\s*(?:км|km)(?![a-zA-Zа-яА-Я0-9])")) { m ->
                val prev = m.groupValues[1]
                val decl = declineUnitByPrecedingWord(prev, "километр", "километра", "километров")
                "$prev $decl"
            }
            result = result.replace(Regex("(?i)(?<=\\d)\\s*м(?![a-zA-Zа-яА-Я0-9/])")) { m ->
                " метров"
            }

            // 7p. Объем: л (литров)
            result = result.replace(Regex("(?i)(\\d+(?:[.,]\\d+)?)\\s*л(?![a-zA-Zа-яА-Я0-9/])")) { m ->
                val prev = m.groupValues[1]
                val decl = declineUnitByPrecedingWord(prev, "литр", "литра", "литров")
                "$prev $decl"
            }

            // 7q. Время: сек, мин, ч
            result = result.replace(Regex("(?i)(\\d+(?:[.,]\\d+)?)\\s*(?:сек(?:унд[а-я]*)?|с\\.)(?![a-zA-Zа-яА-Я0-9/])")) { m ->
                val prev = m.groupValues[1]
                val decl = declineUnitByPrecedingWord(prev, "секунда", "секунды", "секунд")
                "$prev $decl"
            }
            result = result.replace(Regex("(?i)(\\d+(?:[.,]\\d+)?)\\s*(?:мин(?:ут[а-я]*)?|мин\\.)(?![a-zA-Zа-яА-Я0-9/])")) { m ->
                val prev = m.groupValues[1]
                val decl = declineUnitByPrecedingWord(prev, "минута", "минуты", "минут")
                "$prev $decl"
            }
            result = result.replace(Regex("(?i)(\\d+(?:[.,]\\d+)?)\\s*(?:ч(?:ас[а-я]*)?|ч\\.)(?![a-zA-Zа-яА-Я0-9/])")) { m ->
                val prev = m.groupValues[1]
                val decl = declineUnitByPrecedingWord(prev, "час", "часа", "часов")
                "$prev $decl"
            }

            // 7r. Проценты: %
            result = result.replace(Regex("(?i)(\\d+(?:[.,]\\d+)?)\\s*%")) { m ->
                val prev = m.groupValues[1]
                val decl = declineUnitByPrecedingWord(prev, "процент", "процента", "процентов")
                "$prev $decl"
            }.replace("%", " процентов")

            // 7s. Температура: Always speak ONLY whole integer part per user request:
            // "градусы можно вообще только целую часть говорить"
            val tempDegreeRegex = Regex(
                "(?:^|(?<=[^a-zA-Zа-яА-Я0-9]))([+-]?)\\s*(\\d+)(?:[.,](\\d+))?\\s*(?:°\\s*[CСcс]?\\b|°|градус[а-яА-Я]*(?:\\s+Цельсия)?)",
                RegexOption.IGNORE_CASE
            )
            result = tempDegreeRegex.replace(result) { match ->
                val sign = match.groupValues[1]
                val intStr = match.groupValues[2]
                val fracStr = match.groupValues[3]
                var rawInt = intStr.toLongOrNull() ?: 0L
                if (fracStr.isNotEmpty()) {
                    val tenths = fracStr.take(1).toIntOrNull() ?: 0
                    if (tenths >= 5) {
                        rawInt += 1
                    }
                }
                val prefix = when (sign) {
                    "-" -> "минус "
                    "+" -> "плюс "
                    else -> ""
                }
                val words = intToRussianWords(rawInt)
                val decl = degreeDeclension(rawInt)
                val hasCelsius = Regex("°\\s*[CcСс]|Цельси", RegexOption.IGNORE_CASE).containsMatchIn(match.value)
                val suffix = if (hasCelsius) " Цельсия" else ""
                " $prefix$words $decl$suffix "
            }
            // Fallback for standalone degree symbol
            result = result
                .replace(Regex("(?i)°\\s*[CС]\\b"), " градусов Цельсия")
                .replace("°", " градусов")

            // 7t. Прочие единицы: λ, дБ, Ом
            result = result
                .replace("λ", " лямбда")
                .replace(Regex("(?i)(?<![a-zA-Zа-яА-Я0-9])(?:дБ|dB)(?![a-zA-Zа-яА-Я0-9])"), " децибел")
                .replace(Regex("(?i)(?<![a-zA-Zа-яА-Я0-9])(?:кОм|kOhm)(?![a-zA-Zа-яА-Я0-9])"), " килоом")
                .replace(Regex("(?i)(?<![a-zA-Zа-яА-Я0-9])(?:Ом|Ohm)(?![a-zA-Zа-яА-Я0-9])"), " ом")

            // 7u. Автомобильные аббревиатуры
            result = result
                .replace(Regex("(?i)(?<![a-zA-Zа-яА-Я0-9])ДВС(?![a-zA-Zа-яА-Я0-9])"), "двигателя")
                .replace(Regex("(?i)(?<![a-zA-Zа-яА-Я0-9])АКБ(?![a-zA-Zа-яА-Я0-9])"), "аккумулятора")
                .replace(Regex("(?i)(?<![a-zA-Zа-яА-Я0-9])ЭБУ(?![a-zA-Zа-яА-Я0-9])"), "электронного блока управления")
                .replace(Regex("(?i)(?<![a-zA-Zа-яА-Я0-9])ОЖ(?![a-zA-Zа-яА-Я0-9])"), "охлаждающей жидкости")
                .replace(Regex("(?i)(?<![a-zA-Zа-яА-Я0-9])ДТОЖ(?![a-zA-Zа-яА-Я0-9])"), "датчика температуры охлаждающей жидкости")
                .replace(Regex("(?i)(?<![a-zA-Zа-яА-Я0-9])ДМРВ(?![a-zA-Zа-яА-Я0-9])"), "датчика массового расхода воздуха")
                .replace(Regex("(?i)(?<![a-zA-Zа-яА-Я0-9])ДПДЗ(?![a-zA-Zа-яА-Я0-9])"), "датчика положения дроссельной заслонки")
                .replace(Regex("(?i)(?<![a-zA-Zа-яА-Я0-9])ДПКВ(?![a-zA-Zа-яА-Я0-9])"), "датчика положения коленвала")
                .replace(Regex("(?i)(?<![a-zA-Zа-яА-Я0-9])ГРМ(?![a-zA-Zа-яА-Я0-9])"), "газораспределительного механизма")

            // 8. Slashes in options: "и/или", "вкл/выкл", "впуск/выпуск" -> replace '/' with ' или '
            result = result.replace(Regex("(?<=[а-яА-Яa-zA-Z0-9])\\s*/\\s*(?=[а-яА-Яa-zA-Z0-9])"), " или ")

            // 9. Genuine negative and positive numbers (e.g. "-5", "+10"):
            // Must ONLY match if preceded by whitespace, start of line, comma, colon, or parenthesis
            // Will NEVER match a range because ranges were already converted to "от N1 до N2"!
            result = result.replace(Regex("(?:^|(?<=[\\s,;:(]))\\+\\s*(\\d+(?:[.,]\\d+)?)"), " плюс $1")
            result = result.replace(Regex("(?:^|(?<=[\\s,;:(]))-\\s*(\\d+(?:[.,]\\d+)?)"), " минус $1")

            // 10. Natural Russian pronunciation for ALL decimal numbers:
            // 14.2 -> "четырнадцать и две десятых", 0.85 -> "ноль и восемьдесят пять сотых"
            val decimalRegex = Regex("(?<![a-zA-Zа-яА-Я0-9])([+-]?\\d+)[.,](\\d+)(?![a-zA-Zа-яА-Я0-9])")
            result = decimalRegex.replace(result) { match ->
                val rawFullInt = match.groupValues[1]
                val rawFrac = match.groupValues[2]
                val isNeg = rawFullInt.startsWith("-")
                val isPos = rawFullInt.startsWith("+")
                val cleanInt = rawFullInt.trimStart('+', '-')
                val rawInt = cleanInt.toLongOrNull() ?: 0L
                val prefix = if (isNeg) "минус " else if (isPos) "плюс " else ""
                formatDecimalNumberToRussianWords(rawInt, rawFrac, prefix)
            }

            // 11. Compound hyphenated words: keep '-' only if strictly between letters without spaces
            result = result.replace(Regex("\\s+[-–—]+\\s+"), ", ")
            result = result.replace(Regex("[-–—]{2,}"), ", ")
            result = result.replace(Regex("(?<![a-zA-Zа-яА-Я0-9])[-–—]|[-–—](?![a-zA-Zа-яА-Я0-9])"), " ")

            // 12. Strip leftover formatting and punctuation characters that should not be spoken literally
            result = result
                .replace("*", "")
                .replace("/", " ")
                .replace("\\", " ")
                .replace("+", " ")
                .replace("#", "")
                .replace("_", " ")
                .replace("~", "")
                .replace("|", ", ")
                .replace("=", " ")
                .replace("<", " ")
                .replace(">", " ")
                .replace("^", " ")
                .replace("@", " ")
                .replace("&", " и ")
                .replace("`", "")
                .replace("•", "")
                .replace("·", "")
                .replace("✓", "")
                .replace("✔", "")
                .replace("✕", "")
                .replace("✖", "")
                .replace("✗", "")
                .replace("⚡", "")

            // 13. Remove brackets and quotation marks
            result = result
                .replace("[", " ")
                .replace("]", " ")
                .replace("{", " ")
                .replace("}", " ")
                .replace("(", ", ")
                .replace(")", ", ")
                .replace("«", "")
                .replace("»", "")
                .replace("“", "")
                .replace("”", "")
                .replace("\"", "")
                .replace("'", "")

            // 14. Remove all unicode emojis & symbols
            result = result.replace(Regex("[\\p{So}\\p{Cn}\\uD83C-\\uDBFF\\uDC00-\\uDFFF\\u2600-\\u27BF]"), "")

            // 15. Clean up known verbal emoji descriptions
            result = result
                .replace(Regex("(?i)белая\\s+галочка"), "")
                .replace(Regex("(?i)тяжелая\\s+галочка"), "")
                .replace(Regex("(?i)белая\\s+тяжелая\\s+галочка"), "")
                .replace(Regex("(?i)понял[!.]"), "")

            // 16. Normalize punctuation: remove duplicate commas/periods, clean spacing
            result = result
                .replace(Regex("[,\\s]*,+"), ", ")
                .replace(Regex("\\s*\\.+"), ". ")
                .replace(Regex("\\s+"), " ")
                .trim()

            return result
        }
    }

    private fun stopInternalTts() {
        try {
            tts?.stop()
            tts?.playSilentUtterance(1, TextToSpeech.QUEUE_FLUSH, "silence_flush")
            tts?.stop()
        } catch (e: Exception) {
            Log.e("TextToSpeechHelper", "Error stopping TTS: ${e.message}")
        }
    }

    fun stop() {
        playbackMonitorJob?.cancel()
        stopInternalTts()
        _isSpeaking.value = false
    }

    fun shutdown() {
        playbackMonitorJob?.cancel()
        stop()
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("TextToSpeechHelper", "Error shutting down TTS: ${e.message}")
        }
    }
}
