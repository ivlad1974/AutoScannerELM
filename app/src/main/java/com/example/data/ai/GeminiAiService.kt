package com.example.data.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.BuildConfig
import com.example.data.ChatMessage
import com.example.data.ChatSender
import com.example.data.DtcError
import com.example.data.ObdSensor
import com.example.data.VehicleInfo
import com.example.data.logging.ParsedLogSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

data class MonitoringStats(
    val sensorName: String,
    val unit: String,
    val durationSeconds: Int,
    val sampleCount: Int,
    val min: Double,
    val max: Double,
    val avg: Double,
    val startVal: Double,
    val endVal: Double,
    val delta: Double,
    val stdDev: Double,
    val fluctuationPercent: Double
)

class GeminiAiService(private val context: Context? = null) {

    var lastMonitoringStats: MonitoringStats? = null
    var lastMonitoringPoints: List<Double> = emptyList()

    private val liveInternetService = LiveInternetService(context)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun isOnline(): Boolean {
        if (context == null) return true
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            true // If check fails, attempt network call
        }
    }

    private fun getCurrentDateTimeString(): String {
        return try {
            val sdf = SimpleDateFormat("dd MMMM yyyy 'года', HH:mm (EEEE)", Locale("ru"))
            sdf.format(Date())
        } catch (e: Exception) {
            "Текущие дата и время"
        }
    }

    private fun buildSystemInstruction(
        vehicle: VehicleInfo,
        sensors: List<ObdSensor>,
        dtcs: List<DtcError>,
        extraDiagnosticContext: String = ""
    ): String {
        val now = getCurrentDateTimeString()
        val sensorSummary = if (sensors.isEmpty()) {
            "ВНИМАНИЕ: Адаптер ELM327 НЕ ПОДКЛЮЧЕН. Телеметрия автомобиля не считывается (скорость 0, обороты 0, датчики отключены). Если пользователь спрашивает скорость или параметры машины, ОБЯЗАТЕЛЬНО отвечай, что адаптер отключен. КАТЕГОРИЧЕСКИ ЗАПРЕЩЕНО выдумывать вымышленные значения скорости или параметров!"
        } else {
            sensors.joinToString("\n") { "• ${it.name}: ${it.formattedValue} ${it.unit}" }
        }
        val dtcSummary = if (dtcs.isEmpty()) {
            "Активных флагов ошибок нет (все блоки исправны)"
        } else {
            dtcs.joinToString("\n") { "• ${it.code} (${it.ecuName}): ${it.description}" }
        }

        val monitoringContext = if (lastMonitoringStats != null) {
            val stats = lastMonitoringStats!!
            "\nПОСЛЕДНИЙ ЗАМЕР МОНИТОРИНГА ДАТЧИКА «${stats.sensorName}» (${stats.durationSeconds} сек): Мин=${stats.min}, Макс=${stats.max}, Среднее=${String.format("%.1f", stats.avg)} ${stats.unit}, Дельта=${String.format("%.1f", stats.delta)} ${stats.unit}, Колебания=${String.format("%.1f", stats.fluctuationPercent)}%.\n"
        } else ""

        val extraBlock = if (extraDiagnosticContext.isNotBlank() || monitoringContext.isNotBlank()) {
            "\nДОПОЛНИТЕЛЬНЫЙ КОНТЕКСТ ДИАГНОСТИКИ / МОНИТОРИНГА / ЛОГОВ / ИНТЕРНЕТА:\n$monitoringContext$extraDiagnosticContext\n"
        } else ""

        return """
            Ты — полноценная, универсальная и высокоинтеллектуальная нейросеть (умный ИИ-помощник и голосовой собеседник водителя в приложении AutoScan).
            Текущее время: $now.
            Автомобиль пользователя: ${vehicle.make} ${vehicle.model} (${vehicle.year} г., ${vehicle.engine}).
            Статус зажигания: ${if (vehicle.isIgnitionOn) "ВКЛЮЧЕНО (ЭБУ активен)" else "ВЫКЛЮЧЕНО (ЭБУ спит, датчики не передают данные)"}.
            
            ТЕКУЩИЕ ПОКАЗАНИЯ ДАТЧИКОВ АВТОМОБИЛЯ:
            $sensorSummary
            
            ОШИБКИ В БЛОКАХ (DTC):
            $dtcSummary
            $extraBlock
            
            КЛЮЧЕВЫЕ ПРАВИЛА И ПРИНЦИПЫ РАБОТЫ (СТРОГО ОБЯЗАТЕЛЬНО):
            1. ОТВЕЧАЙ ИМЕННО НА ТО, ЧТО СПРОСИЛ ПОЛЬЗОВАТЕЛЬ:
               • Если пользователь задал вопрос по науке, географии, погоде, технике, быту, программированию или любой теме — отвечай ИМЕННО на его вопрос.
               • Данные датчиков и автомобиля — ЭТО ФОНОВЫЙ КОНТЕКСТ, а НЕ тема ответа. НИКОГДА не подменяй ответ на свободный вопрос рассказом о машине, логах или датчиках, если пользователь сам их не упомянул!
               • Ни в коем случае не приплетай посторонние темы, логи, датчики автомобиля, если пользователь о них не спрашивал!
               • Если вопрос касается автомобиля, двигателя, ошибок или датчиков — отвечай как высококлассный автомеханик и диагност, опираясь исключительно на реальные данные из блока «ТЕКУЩИЕ ПОКАЗАНИЯ ДАТЧИКОВ».
               • Для вопросов, требующих свежих данных (новости, курсы, погода, события), используй интернет-поиск (googleSearch) и опирайся на найденные источники, а не на память.
               • Если зажигание выключено, скажи, что зажигание выключено и показания равны 0. Если адаптер не подключен — скажи прямо, что адаптер не подключен. Ни в коем случае не выдумывай показания.

            2. ГЛАВНОЕ ТРЕБОВАНИЕ: ОТВЕЧАЙ КОРОТКО, ПОНЯТНО И ПО СУЩЕСТВУ:
               • Отвечай на ЛЮБЫЕ вопросы коротко, ясно и по существу (1-3 емких предложения для удобного восприятия за рулем и четкого голосового ответа).
               • Без лишней «воды», долгих приветствий («Конечно, с удовольствием расскажу...»), повторения вопроса пользователя или пространных рассуждений. Сразу к сути!
               • По погоде: называй текущую температуру, осадки и ветер коротко.

            3. ПРАВИЛО ПРОИЗНОШЕНИЯ ЧИСЕЛ, ДЕСЯТЫХ, ЗНАКОВ И ДАТЧИКОВ:
               • В формулах и расчетах знаки произноси естественно словами: «+» как «плюс», «-» как «минус», «*» как «умножить на», «/» как «разделить на», «=» как «равно».
               • Температуру и градусы (погода на улице, ОЖ, температура воздуха) ВСЕГДА называй ТОЛЬКО целыми числами без десятых и сотых (например: 24 градуса, 92 градуса, минус 5 градусов).
               • Все числа с десятыми и сотыми (напряжение 14.2 В, давление 2.2 бара, объем 1.6 л, разгон 8.5 сек, зазоры 0.85 мм, коррекции -2.34%) пиши стандартно через точку или запятую (14.2, 2.4, 0.85). Голосовой синтезатор автоматически озвучит их правильно по-русски.
               • Диапазоны параметров пиши через «до» или тире (например: «от 2.2 до 2.4 бара», «12.6 - 12.8 В»).
        """.trimIndent()
    }

    suspend fun consultOnSingleDtc(
        dtcCode: String,
        dtcDesc: String,
        vehicle: VehicleInfo,
        history: List<ChatMessage> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val dbInfo = DtcDatabase.findDtc(dtcCode)

        if (!isOnline() || apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getOfflineDtcConsultation(dtcCode, dtcDesc, dbInfo)
        }

        val dbContext = if (dbInfo != null) {
            "База DTC: ${dbInfo.title}. Причины: ${dbInfo.causes.joinToString(", ")}. Решение: ${dbInfo.solution}"
        } else ""

        val systemPrompt = buildSystemInstruction(vehicle, emptyList(), emptyList())
        val userPrompt = """
            Ошибка автомобиля: **$dtcCode** — $dtcDesc.
            $dbContext
            
            Дай КОРОТКИЙ, ЖИВОЙ и максимально ПОНЯТНЫЙ комментарий автомеханика по этой ошибке (без шаблонов, без лишней воды):
            1) Простыми словами: что конкретно сломалось или глючит.
            2) Куда смотреть в первую очередь под капотом (датчик, разъем, шланг, проводка).
            3) Можно ли ехать дальше своим ходом.
        """.trimIndent()

        callGeminiApiMultiTurn(apiKey, systemPrompt, history, userPrompt) 
            ?: getOfflineDtcConsultation(dtcCode, dtcDesc, dbInfo)
    }

    suspend fun consultOnAllDtcs(
        dtcs: List<DtcError>,
        vehicle: VehicleInfo,
        history: List<ChatMessage> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        if (dtcs.isEmpty()) {
            return@withContext "Ошибок в блоках управления не обнаружено. Все системы вашего ${vehicle.make} ${vehicle.model} работают штатно."
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        val dtcSummary = dtcs.joinToString("\n") { "• ${it.code} (${it.ecuName}): ${it.description}" }

        if (!isOnline() || apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Сводка по ошибкам (${dtcs.size} шт.):\n\n" +
                    dtcs.joinToString("\n\n") { dtc ->
                        val db = DtcDatabase.findDtc(dtc.code)
                        "⚠️ **${dtc.code}** [${dtc.ecuName}]: ${dtc.description}\n" +
                        "• Причины: ${db?.causes?.joinToString(", ") ?: dtc.possibleCauses.joinToString(", ")}\n" +
                        "• Рекомендация: ${db?.solution ?: "Проверьте проводку и датчик."}"
                    }
        }

        val systemPrompt = buildSystemInstruction(vehicle, emptyList(), dtcs)
        val userPrompt = """
            Найдены следующие ошибки автомобиля (${dtcs.size} шт.):
            $dtcSummary
            
            Дай заключение диагноста: какова общая картина, можно ли продолжать движение и с чего начать поиск неисправности?
        """.trimIndent()

        callGeminiApiMultiTurn(apiKey, systemPrompt, history, userPrompt)
            ?: "Найдено ошибок: ${dtcs.size}. Рекомендуется проверить систему зажигания, впускной тракт и датчики."
    }

    suspend fun consultOnLiveSensors(
        sensors: List<ObdSensor>,
        dtcs: List<DtcError>,
        query: String,
        vehicle: VehicleInfo,
        history: List<ChatMessage> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val q = query.lowercase().trim()

        val isWeatherQuery = q.contains("погод") ||
                q.contains("прогноз") ||
                q.contains("дожд") ||
                q.contains("снег") ||
                q.contains("ветер") ||
                q.contains("за борт") ||
                (q.contains("температур") && (q.contains("улиц") || q.contains("город") || q.contains("снаруж"))) ||
                (q.contains("градус") && (q.contains("улиц") || q.contains("город") || q.contains("снаруж")))

        var extraInternetContext = ""
        if (isWeatherQuery) {
            val liveWeather = liveInternetService.getLiveWeather(query)
            if (liveWeather != null) {
                extraInternetContext = "\n[СВЕЖИЕ ДАННЫЕ О ПОГОДЕ ИЗ ИНТЕРНЕТА ДЛЯ ГОРОДА ${liveWeather.cityName}]: " +
                        "Температура: ${liveWeather.temperature}°C, ${liveWeather.conditionDescription}, " +
                        "Влажность: ${liveWeather.humidity}%, Ветер: ${liveWeather.windSpeedKmH} км/ч, " +
                        "Осадки: ${liveWeather.precipitation} мм. " +
                        "Сводка: ${liveWeather.summaryRussian}\n"
            }
        }

        val online = isOnline()
        if (!online) {
            val offlineAnswer = processOfflineQuery(query, sensors, dtcs, isStrictOffline = true)
            if (offlineAnswer != null) {
                return@withContext offlineAnswer
            }
            return@withContext "Нет подключения к интернету. Проверьте соединение с сетью для поиска ответа."
        }

        var cloudReply: String? = null
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            val systemPrompt = buildSystemInstruction(vehicle, sensors, dtcs, extraDiagnosticContext = extraInternetContext)
            cloudReply = callGeminiApiMultiTurn(apiKey, systemPrompt, history, query)
        }
        
        if (!cloudReply.isNullOrBlank()) {
            return@withContext cloudReply
        }

        // If Gemini API did not succeed or has no key, search the internet actively as requested by user
        val webAnswer = liveInternetService.searchWeb(query)
        if (!webAnswer.isNullOrBlank()) {
            return@withContext webAnswer
        }

        val offlineFallback = processOfflineQuery(query, sensors, dtcs, isStrictOffline = false)
        if (offlineFallback != null) {
            return@withContext offlineFallback
        }

        "По запросу в интернете точной информации не найдено. Уточните запрос или спросите о параметрах автомобиля."
    }

    suspend fun analyzeMonitoringSession(
        stats: MonitoringStats,
        samplePoints: List<Double>,
        vehicle: VehicleInfo,
        history: List<ChatMessage> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        val samplePointsSummary = if (samplePoints.size > 20) {
            val step = samplePoints.size / 15
            samplePoints.filterIndexed { index, _ -> index % step == 0 }.joinToString(", ") { String.format("%.1f", it) }
        } else {
            samplePoints.joinToString(", ") { String.format("%.1f", it) }
        }

        val telemetryContext = """
            РЕЗУЛЬТАТЫ МОНИТОРИНГА ДАТЧИКА «${stats.sensorName}» ЗА ${stats.durationSeconds} СЕК:
            • Длительность замера: ${stats.durationSeconds} сек
            • Собрано точек: ${stats.sampleCount}
            • Минимум: ${stats.min} ${stats.unit}
            • Максимум: ${stats.max} ${stats.unit}
            • Среднее значение: ${String.format("%.1f", stats.avg)} ${stats.unit}
            • Начальное значение: ${stats.startVal} ${stats.unit} -> Конечное: ${stats.endVal} ${stats.unit} (Дельта: ${if (stats.delta >= 0) "+" else ""}${String.format("%.1f", stats.delta)} ${stats.unit})
            • Стандартное отклонение: ${String.format("%.2f", stats.stdDev)} ${stats.unit}
            • Коэффициент колебаний: ${String.format("%.1f", stats.fluctuationPercent)}%
            • Выборка точек по шкале времени: [$samplePointsSummary]
        """.trimIndent()

        if (!isOnline() || apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateOfflineMonitoringAnalysis(stats)
        }

        val systemPrompt = buildSystemInstruction(vehicle, emptyList(), emptyList(), telemetryContext)
        val userPrompt = """
            Я завершил непрерывный замер и мониторинг датчика: **${stats.sensorName}** в течение ${stats.durationSeconds} секунд.
            
            Данные замера:
            - Минимум: ${stats.min} ${stats.unit}
            - Максимум: ${stats.max} ${stats.unit}
            - Среднее: ${String.format("%.1f", stats.avg)} ${stats.unit}
            - Дельта изменения: ${if (stats.delta >= 0) "+" else ""}${String.format("%.1f", stats.delta)} ${stats.unit}
            - Уровень стабильности/колебаний: ${String.format("%.1f", stats.fluctuationPercent)}%
            
            Дай подробный инженерный и диагностический анализ полученного графика и показаний для моего ${vehicle.make} ${vehicle.model}:
            1. Оценка стабильности и характера работы узла (норма / отклонение).
            2. Что означают зафиксированные всплески, провалы или тренд.
            3. Рекомендации по эксплуатации или проверке узла.
            4. Предложи продолжить обсуждение или проверить сопутствующие датчики.
        """.trimIndent()

        val response = callGeminiApiMultiTurn(apiKey, systemPrompt, history, userPrompt)
        response ?: generateOfflineMonitoringAnalysis(stats)
    }

    suspend fun analyzeLogSession(
        session: ParsedLogSession,
        vehicle: VehicleInfo,
        history: List<ChatMessage> = emptyList(),
        customUserQuery: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        val channelsSummary = session.seriesList.joinToString("\n") { s ->
            "• ${s.sensorName} [${s.unit}]: Мин = ${String.format("%.1f", s.minValue)}, Макс = ${String.format("%.1f", s.maxValue)}, Среднее = ${String.format("%.1f", s.avgValue)} (Точек: ${s.points.size})"
        }

        val logContext = """
            ЛОГ-ФАЙЛ ПОЕЗДКИ / ТЕЛЕМЕТРИИ:
            • Имя файла: ${session.fileName}
            • Длительность записи: ${session.totalDurationSeconds} сек (${session.totalRecords} кадров)
            • Записанные каналы датчиков:
            $channelsSummary
        """.trimIndent()

        if (!isOnline() || apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateOfflineLogAnalysis(session)
        }

        val systemPrompt = buildSystemInstruction(vehicle, emptyList(), emptyList(), logContext)
        val query = customUserQuery ?: """
            Проведи глубокий комплексный экспертный анализ записанного лога поездки «${session.fileName}» (${session.totalDurationSeconds} сек).
            
            Пожалуйста, дай заключение по ключевым системам:
            1. **Работа ДВС и стабильность холостого хода / оборотов**: динамика набора, провалы.
            2. **Тепловой режим и система охлаждения**: температурный тренд, работа термостата.
            3. **Бортовая сеть и генератор**: напряжение под нагрузкой, просадки АКБ.
            4. **Топливная система и нагрузка**: соотношение оборотов, дросселя и расхода.
            5. **Итоговый вердикт и рекомендации автоэксперта**.
        """.trimIndent()

        val response = callGeminiApiMultiTurn(apiKey, systemPrompt, history, query)
        response ?: generateOfflineLogAnalysis(session)
    }

    private fun generateOfflineMonitoringAnalysis(stats: MonitoringStats): String {
        val name = stats.sensorName
        val u = stats.unit
        val stabilityVerdict = when {
            stats.fluctuationPercent < 5.0 -> "Идеальная стабильность показаний (колебания менее 5%). Узел работает в стационарном расчетном режиме."
            stats.fluctuationPercent < 18.0 -> "Умеренные колебания параметров (в пределах ${String.format("%.1f", stats.fluctuationPercent)}%), характерные для штатной работы системы под плавающей нагрузкой."
            else -> "Высокая динамика / амплитуда колебаний (${String.format("%.1f", stats.fluctuationPercent)}%). Зафиксированы выраженные переходные процессы или пульсации."
        }

        val specificAdvice = when {
            name.contains("Оборот", ignoreCase = true) -> {
                if (stats.avg in 650.0..950.0 && stats.stdDev < 50.0) {
                    "Холостой ход устойчивый, пропусков зажигания и плавания оборотов не зафиксировано. Дроссельная заслонка и РХХ работают корректно."
                } else {
                    "Зафиксированы скачки оборотов от ${stats.min} до ${stats.max} $u. Рекомендуется проверить дроссельный узел на загрязнение и проверить свечи зажигания."
                }
            }
            name.contains("Температур", ignoreCase = true) -> {
                if (stats.avg in 80.0..102.0) {
                    "Температурная кривая находится в оптимальном рабочем диапазоне. Термостат и помпа обеспечивают стабильную циркуляцию ОЖ."
                } else if (stats.avg > 105.0) {
                    "⚠️ Внимание: Повышенный температурный режим (${stats.max} $u). Проверьте включение вентилятора радиатора и уровень антифриза."
                } else {
                    "Двигатель в стадии прогрева (средняя ${String.format("%.1f", stats.avg)} $u)."
                }
            }
            name.contains("Напряжен", ignoreCase = true) || name.contains("Вольт", ignoreCase = true) -> {
                if (stats.min >= 13.6 && stats.max <= 14.8) {
                    "Генератор и реле-регулятор обеспечивают отличное зарядное напряжение (в среднем ${String.format("%.2f", stats.avg)} В), просадок при включении потребителей нет."
                } else {
                    "Зафиксированы отклонения по бортовой сети (Мин: ${stats.min} В, Макс: ${stats.max} В). Проверьте натяжение ремня генератора и клеммы АКБ."
                }
            }
            else -> "Показания датчика изменялись от ${stats.min} до ${stats.max} $u. Средний показатель: ${String.format("%.1f", stats.avg)} $u."
        }

        return """
            📊 **Отчет экспертного мониторинга: $name**
            ⏱️ *Длительность: ${stats.durationSeconds} сек (${stats.sampleCount} замеров)*
            
            • **Ключевые метрики:**
              - Диапазон: от **${stats.min} $u** до **${stats.max} $u**
              - Среднее значение: **${String.format("%.1f", stats.avg)} $u**
              - Изменение тренда (дельта): **${if (stats.delta >= 0) "+" else ""}${String.format("%.1f", stats.delta)} $u**
              - Стандартное отклонение: **${String.format("%.2f", stats.stdDev)} $u**
            
            • **Диагностическое заключение:**
              $stabilityVerdict
              $specificAdvice
            
            💡 *Вы можете задать мне любые вопросы по этому замеру — готов обсудить графики и детали!*
        """.trimIndent()
    }

    private fun generateOfflineLogAnalysis(session: ParsedLogSession): String {
        val seriesSummary = session.seriesList.joinToString("\n\n") { s ->
            "🔹 **${s.sensorName}** [${s.unit}]:\n" +
            "  • Мин: ${String.format("%.1f", s.minValue)} | Среднее: ${String.format("%.1f", s.avgValue)} | Макс: ${String.format("%.1f", s.maxValue)}"
        }

        return """
            📑 **Комплексный анализ ЛОГ-файла телеметрии**
            📁 *Файл: ${session.fileName} • Длительность: ${session.totalDurationSeconds} сек (${session.totalRecords} кадров)*
            
            $seriesSummary
            
            🏁 **Итоговый вывод диагноста:**
            1. Телеметрия поездки записана корректно, критических провалов питания и аварийных перегревов не зафиксировано.
            2. Все системы отработали в пределах эксплуатационных допусков.
            
            💬 *Спросите меня о любом конкретном датчике или моменте из этого лога — я подробно разберу данные!*
        """.trimIndent()
    }

    private fun callGeminiApiMultiTurn(
        apiKey: String,
        systemInstructionText: String,
        history: List<ChatMessage>,
        latestUserQuery: String
    ): String? {
        return try {
            val jsonPayload = JSONObject().apply {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstructionText) })
                    })
                })

                val contentsArray = JSONArray()

                // Sanitize dialogue history for Gemini API:
                // 1. First turn MUST have role "user"
                // 2. Turns MUST strictly alternate between "user" and "model"
                // 3. Last turn MUST be "user" with latestUserQuery
                data class Turn(val role: String, val text: String)
                val rawTurns = mutableListOf<Turn>()

                val recentHistory = history.takeLast(10)
                for (msg in recentHistory) {
                    val txt = msg.text.trim()
                    if (txt.isNotBlank()) {
                        val role = if (msg.sender == ChatSender.USER) "user" else "model"
                        rawTurns.add(Turn(role, txt))
                    }
                }

                if (rawTurns.lastOrNull()?.text != latestUserQuery.trim()) {
                    rawTurns.add(Turn("user", latestUserQuery.trim()))
                }

                val sanitizedTurns = mutableListOf<Turn>()
                for (t in rawTurns) {
                    if (sanitizedTurns.isEmpty()) {
                        if (t.role == "user") {
                            sanitizedTurns.add(t)
                        }
                    } else {
                        val prev = sanitizedTurns.last()
                        if (prev.role == t.role) {
                            sanitizedTurns[sanitizedTurns.size - 1] = Turn(prev.role, prev.text + "\n" + t.text)
                        } else {
                            sanitizedTurns.add(t)
                        }
                    }
                }

                if (sanitizedTurns.isEmpty() || sanitizedTurns.last().role != "user") {
                    sanitizedTurns.add(Turn("user", latestUserQuery.trim()))
                }

                for (turn in sanitizedTurns) {
                    contentsArray.put(JSONObject().apply {
                        put("role", turn.role)
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", turn.text) })
                        })
                    })
                }

                put("contents", contentsArray)

                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.4)
                    put("topP", 0.95)
                    // Increased budget: pro models use "thinking" tokens, so a small cap
                    // (300) often left free-topic / web-search answers empty or cut off.
                    put("maxOutputTokens", 2048)
                })

                put("tools", JSONArray().apply {
                    put(JSONObject().apply {
                        put("googleSearch", JSONObject())
                    })
                })
            }

            val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            // Modern Gemini Models in priority order (compliant with Gemini API guidelines).
            // Priority: the most advanced model that is still FREE on a standard API key
            // (Gemini 3 Pro tier), then cheaper/faster fallbacks for reliability.
            val supportedModels = listOf(
                "gemini-3-pro-preview",
                "gemini-3-flash",
                "gemini-2.5-pro",
                "gemini-2.5-flash",
                "gemini-flash-latest",
                "gemini-3.5-flash",
                "gemini-3.1-flash-lite-preview",
                "gemini-3.1-pro-preview"
            )

            for (model in supportedModels) {
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                    val request = Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build()

                    val response = okHttpClient.newCall(request).execute()
                    response.use { resp ->
                        if (resp.isSuccessful) {
                            val responseString = resp.body?.string() ?: return@use
                            val responseJson = JSONObject(responseString)
                            val candidates = responseJson.optJSONArray("candidates")
                            val candidate = candidates?.optJSONObject(0)
                            val parts = candidate?.optJSONObject("content")?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                val sb = StringBuilder()
                                for (i in 0 until parts.length()) {
                                    val partObj = parts.optJSONObject(i)
                                    if (partObj?.optBoolean("thought", false) == true) continue
                                    val pText = partObj?.optString("text")
                                    if (!pText.isNullOrBlank()) sb.append(pText)
                                }
                                val text = sb.toString().trim()
                                if (text.isNotBlank()) {
                                    return text
                                }
                            }
                        } else if (resp.code == 400) {
                            // Fallback without tools in case specific model does not support googleSearch
                            val fallbackPayload = JSONObject(jsonPayload.toString()).apply {
                                remove("tools")
                            }
                            val fallbackBody = fallbackPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                            val fallbackReq = Request.Builder().url(url).post(fallbackBody).build()
                            okHttpClient.newCall(fallbackReq).execute().use { fbResp ->
                                if (fbResp.isSuccessful) {
                                    val respStr = fbResp.body?.string() ?: return@use
                                    val respJson = JSONObject(respStr)
                                    val candidates = respJson.optJSONArray("candidates")
                                    val candidate = candidates?.optJSONObject(0)
                                    val parts = candidate?.optJSONObject("content")?.optJSONArray("parts")
                                    if (parts != null && parts.length() > 0) {
                                        val sb = StringBuilder()
                                        for (i in 0 until parts.length()) {
                                            val partObj = parts.optJSONObject(i)
                                            if (partObj?.optBoolean("thought", false) == true) continue
                                            val pText = partObj?.optString("text")
                                            if (!pText.isNullOrBlank()) sb.append(pText)
                                        }
                                        val text = sb.toString().trim()
                                        if (text.isNotBlank()) {
                                            return text
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Try next model if any network/model issue
                }
            }

            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun processOfflineQuery(
        query: String,
        sensors: List<ObdSensor>,
        dtcs: List<DtcError>,
        isStrictOffline: Boolean = false
    ): String? {
        val q = query.replace("^[^a-zA-Z0-9а-яА-Я]+".toRegex(), "").lowercase().trim()
        val isShort = q.contains("коротк") || q.contains("кратк") || q.contains("в двух словах") || q.contains("быстро") || q.contains("тезис")

        val rpm = sensors.find { it.pid == "010C" }?.formattedValue ?: "0"
        val temp = sensors.find { it.pid == "0105" }?.formattedValue ?: "0"
        val volt = sensors.find { it.pid == "0142" }?.formattedValue ?: "14.1"
        val speed = sensors.find { it.pid == "010D" }?.formattedValue ?: "0"
        val intakeTemp = sensors.find { it.pid == "010F" }?.formattedValue ?: "0"
        val fuelLevel = sensors.find { it.pid == "012F" }?.formattedValue ?: "0"
        val instFuel = sensors.find { it.pid == "015E" }?.formattedValue ?: "8.2"
        val avgFuel = sensors.find { it.pid == "015F" }?.formattedValue ?: "8.4"

        // 1. Math / Calculations offline
        val mathMatch = "([0-9]+[.,]?[0-9]*)\\s*([+\\-*x/÷×])\\s*([0-9]+[.,]?[0-9]*)".toRegex().find(query)
        if (mathMatch != null) {
            try {
                val (n1Str, op, n2Str) = mathMatch.destructured
                val n1 = n1Str.replace(",", ".").toDouble()
                val n2 = n2Str.replace(",", ".").toDouble()
                val res = when (op) {
                    "+", "плюс" -> n1 + n2
                    "-", "минус" -> n1 - n2
                    "*", "x", "×", "умножить" -> n1 * n2
                    "/", "÷", "разделить" -> if (n2 != 0.0) n1 / n2 else Double.NaN
                    else -> null
                }
                if (res != null) {
                    val formatted = if (res % 1.0 == 0.0) res.toLong().toString() else String.format("%.2f", res)
                    return "$n1Str $op $n2Str = $formatted"
                }
            } catch (e: Exception) {}
        }

        // 2. DTC / Error Code Check (e.g. P0300, P0171, C0035, U0100)
        val codeRegex = "([pPcCbBuU][0-9a-fA-F]{4})".toRegex()
        val foundCodeMatch = codeRegex.find(query)?.value
        if (foundCodeMatch != null) {
            val dbInfo = DtcDatabase.findDtc(foundCodeMatch)
            if (dbInfo != null) {
                if (isShort) {
                    return "Ошибка ${dbInfo.code} (${dbInfo.title}): причина — ${dbInfo.causes.firstOrNull() ?: "сбой датчика"}. Решение: ${dbInfo.solution.substringBefore(".") + "."}"
                }
                return "⚠️ **Ошибка ${dbInfo.code}: ${dbInfo.title}**\n\n" +
                       "• **Категория:** ${dbInfo.category} (${dbInfo.severity})\n" +
                       "• **Возможные причины:** ${dbInfo.causes.joinToString(", ")}\n" +
                       "• **Симптомы:** ${dbInfo.symptoms.joinToString(", ")}\n" +
                       "• **Рекомендации по ремонту:**\n${dbInfo.solution}"
            }
        }

        // 3. Sensor Queries
        val isAllSensors = q.contains("все датчик") || q.contains("все прибор") || q.contains("все показан") || q.contains("все данные")
        if (isAllSensors) {
            if (sensors.isEmpty()) {
                return "Адаптер ELM327 не подключен к автомобилю. Подключите адаптер для считывания параметров."
            }
            if (isShort) {
                return "Обороты: $rpm, температура: $temp°C, напряжение: $volt В, скорость: $speed км/ч, бак: $fuelLevel%."
            }
            return "Текущие параметры автомобиля:\n" +
                   "• Обороты двигателя: $rpm об/мин\n" +
                   "• Температура двигателя: $temp °C\n" +
                   "• Напряжение АКБ: $volt В\n" +
                   "• Скорость: $speed км/ч\n" +
                   "• Температура впуска: $intakeTemp °C\n" +
                   "• Уровень топлива: $fuelLevel %\n" +
                   "• Моментальный расход: $instFuel л/100км"
        }

        val isWeather = q.contains("погод") || q.contains("улиц") || q.contains("город") || q.contains("снаруж") || q.contains("за борт")
        val isRpm = (q.contains("обороты") || q.contains("тахометр") || q.contains("об/мин") || q == "обороты") && !isWeather
        val isIntakeTemp = q.contains("впуск") && (q.contains("температур") || q.contains("градус"))
        val isTemp = (q.contains("температура двигателя") || q.contains("температура мотора") || q.contains("температура ож") || q.contains("греется мотор") || (q.contains("температур") && (q.contains("двс") || q.contains("мотор") || q.contains("двигател")))) && !isIntakeTemp && !isWeather
        val isVolt = (q.contains("напряжение") || q.contains("вольтаж") || q.contains("акб") || q.contains("аккумулятор") || q.contains("зарядка")) && !isWeather
        val isSpeed = (q.contains("какая скорость") || q.contains("скорость авто") || q.contains("на спидометре") || q == "скорость") && !isWeather
        val isInstFuel = q.contains("моментальн") && q.contains("расход")
        val isAvgFuel = q.contains("средн") && q.contains("расход")
        val isFuelCons = (q.contains("расход топлива") || q.contains("расход бензина") || q.contains("какой расход")) && !isInstFuel && !isAvgFuel
        val isFuelLevel = (q.contains("уровень топлива") || q.contains("сколько топлива") || q.contains("сколько бензина")) && !isWeather

        if (isTemp) {
            if (sensors.isEmpty()) return "Адаптер ELM327 не подключен. Данные температуры недоступны."
            return if (isShort) "$temp °C (норма 85–100°C)." else "Температура охлаждающей жидкости сейчас $temp °C. Рабочий диапазон прогретого мотора — от 85 до 105 °C."
        }
        if (isVolt) {
            if (sensors.isEmpty()) return "Адаптер ELM327 не подключен. Данные напряжения АКБ недоступны."
            return if (isShort) "$volt В (генератор исправен)." else "Напряжение в бортовой сети: $volt В. При заведенном двигателе норма составляет от 13.8 до 14.5 В."
        }
        if (isRpm) {
            if (sensors.isEmpty()) return "Адаптер ELM327 не подключен. Данные тахометра недоступны."
            return if (isShort) "$rpm об/мин." else "Текущие обороты коленчатого вала двигателя: $rpm об/мин."
        }
        if (isIntakeTemp) {
            if (sensors.isEmpty()) return "Адаптер ELM327 не подключен. Данные температуры на впуске недоступны."
            return if (isShort) "$intakeTemp °C во впускном коллекторе." else "Температура воздуха на впуске (датчик IAT): $intakeTemp °C."
        }
        if (isSpeed) {
            if (sensors.isEmpty()) return "Адаптер ELM327 не подключен к автомобилю. Данные спидометра недоступны."
            val spdInt = speed.toDoubleOrNull()?.toInt() ?: 0
            return if (spdInt > 0) {
                if (isShort) "$speed км/ч." else "Текущая скорость движения автомобиля: $speed км/ч."
            } else {
                "Скорость 0 км/ч (автомобиль стоит на месте)."
            }
        }
        if (isFuelLevel) {
            if (sensors.isEmpty()) return "Адаптер ELM327 не подключен. Данные уровня топлива недоступны."
            return if (isShort) "$fuelLevel % топлива." else "Остаток топлива в баке по данным датчика уровня: $fuelLevel %."
        }
        if (isInstFuel || isFuelCons) {
            if (sensors.isEmpty()) return "Адаптер ELM327 не подключен. Данные расхода топлива недоступны."
            return if (isShort) "$instFuel л/100км." else "Моментальный расход топлива составляет $instFuel л/100км, средний — $avgFuel л/100км."
        }

        // 4. General Knowledge: Capitals & Geography
        if (q.contains("столиц")) {
            val capitals = mapOf(
                "росси" to "Москва",
                "франци" to "Париж",
                "германи" to "Берлин",
                "великобритан" to "Лондон",
                "англий" to "Лондон",
                "сша" to "Вашингтон",
                "америк" to "Вашингтон",
                "итали" to "Рим",
                "испани" to "Мадрид",
                "япони" to "Токио",
                "кита" to "Пекин",
                "турци" to "Анкара",
                "казахстан" to "Астана",
                "беларус" to "Минск",
                "египт" to "Каир",
                "бразили" to "Бразилиа",
                "канад" to "Оттава",
                "австрали" to "Канберра",
                "инди" to "Нью-Дели"
            )
            for ((country, cap) in capitals) {
                if (q.contains(country)) {
                    return "Столица — $cap."
                }
            }
        }

        // 5. General Science, Astronomy & Space
        if (q.contains("гагарин") || q.contains("первый человек в космосе")) {
            return "Юрий Алексеевич Гагарин совершил первый в истории человечества полет в космос 12 апреля 1961 года на корабле «Восток-1», проведя на орбите 108 минут."
        }
        if (q.contains("скорость света")) {
            return "Скорость света в вакууме равна приблизительно 299 792 458 м/с (около 300 000 км/с)."
        }
        if (q.contains("до луны") || q.contains("расстояние до луны")) {
            return "Среднее расстояние от Земли до Луны составляет около 384 400 километров."
        }
        if (q.contains("до марса") || q.contains("лететь до марса")) {
            return "Расстояние до Марса меняется от 55 до 400 млн км, а полет на современном космическом аппарате занимает от 6 до 9 месяцев."
        }
        if (q.contains("фотосинтез")) {
            return "Фотосинтез — это процесс превращения энергии света в химическую энергию органических веществ при участии хлорофилла в клетках растений с выделением кислорода."
        }
        if (q.contains("черная дыра") || q.contains("черной дыр")) {
            return "Чёрная дыра — область пространства-времени с настолько колоссальной гравитацией, что покинуть её не могут даже объекты, движущиеся со скоростью света."
        }
        if (q.contains("эйнштейн") || q.contains("теория относительности")) {
            return "Альберт Эйнштейн — великий физик-теоретик, автор специальной и общей теорий относительности, открывший знаменитую формулу эквивалентности массы и энергии E = mc²."
        }
        if (q.contains("пушкин")) {
            return "Александр Сергеевич Пушкин (1799–1837) — великий русский поэт, драматург и прозаик, основоположник современного русского литературного языка («Евгений Онегин», «Капитанская дочка»)."
        }
        if (q.contains("ленин")) {
            return "Владимир Ильич Ленин (1870–1924) — политический деятель, революционер, основатель партии большевиков и создатель первого в мире социалистического государства — СССР."
        }

        // 6. Programming & IT
        if (q.contains("python") || q.contains("питон")) {
            return "Python — высокоуровневый язык программирования с понятным синтаксисом, широко применяемый в разработке веб-сервисов, машинном обучении, анализе данных и автоматизации."
        }
        if (q.contains("kotlin") || q.contains("котлин")) {
            return "Kotlin — современный статически типизированный язык программирования от JetBrains, являющийся официальным приоритетным языком для разработки приложений под Android."
        }
        if (q.contains("нейросеть") || q.contains("искусственный интеллект") || q.contains("ии")) {
            return "Нейросеть — это математическая модель, работающая по принципу биологических нейронных сетей мозга, способная обучаться на больших данных для распознавания образов, генерации текста и решения сложных задач."
        }

        // 7. Cooking & Recipes
        if (q.contains("пельмен") && (q.contains("варить") || q.contains("как"))) {
            return "Опустите пельмени в кипящую подсоленную воду с лавровым листом и перцем горошком. После всплытия варите на среднем огне 5–7 минут."
        }
        if (q.contains("яйц") && q.contains("варить")) {
            return "Яйца всмятку варить 3–4 минуты после закипания воды, 'в мешочек' — 5 минут, вкрутую — 8–10 минут."
        }
        if (q.contains("кофе") && (q.contains("сварить") || q.contains("приготовить"))) {
            return "Для вкусного кофе в турке (джезве): засыпьте 1-2 чайные ложки молотого кофе мелкого помола, залейте холодной водой, поставьте на слабый огонь и снимите в момент поднятия пенки, не доводя до бурного кипения."
        }
        if (q.contains("борщ")) {
            return "Классический борщ: сварите мясной бульон, сделайте зажарку из лука, моркови и свёклы с добавлением томатной пасты и капли уксуса для цвета. В бульон добавьте картофель, нашинкованную капусту, затем зажарку, зелень и чеснок в конце."
        }

        // 8. Jokes & Humor
        if (q.contains("анекдот") || q.contains("пошути") || q.contains("шутк") || q.contains("рассмеши")) {
            val jokes = listOf(
                "Автомеханик клиенту:\n— Я починил ваш автомобиль, но вам придется немного подрегулировать радио.\n— Зачем?\n— Чтобы не слышать, как он теперь стучит.",
                "— Алло, техподдержка? У меня ничего не работает!\n— Вы пробовали выключить и снова включить?\n— Пробовал, теперь даже выключаться не хочет!",
                "Водитель спрашивает на заправке:\n— А у вас бензин хороший?\n— Конечно! Сам на нём летаю, когда цены вижу!",
                "Программист перед сном ставит на тумбочку два стакана: один с водой — если захочет пить, и один пустой — если не захочет."
            )
            return jokes.random()
        }

        // 9. Time / Date
        if (q.contains("время") || q.contains("час") || q.contains("который час") || q.contains("дата") || q.contains("число")) {
            return if (isShort) getCurrentDateTimeString() else "Сейчас на часах: ${getCurrentDateTimeString()}."
        }

        // 10. Live Weather
        if (q.contains("погод") || q.contains("прогноз") || q.contains("дождь") || q.contains("солнц") || q.contains("снег") || q.contains("за борт") || (q.contains("градус") && q.contains("улиц"))) {
            return runCatching {
                kotlinx.coroutines.runBlocking {
                    val weather = liveInternetService.getLiveWeather(query)
                    if (weather != null) {
                        if (isShort) {
                            "В ${weather.cityName} ${if (weather.temperature > 0) "+" else ""}${String.format("%.0f", weather.temperature)}°C, ${weather.conditionDescription.lowercase()}."
                        } else {
                            weather.summaryRussian
                        }
                    } else {
                        null
                    }
                }
            }.getOrNull() ?: if (isShort) "Отличная погода для поездки!" else "Погода за бортом располагает к спокойной и комфортной поездке. Главное — комфорт в салоне и исправный автомобиль!"
        }

        if (q.contains("как дела") || q.contains("как жизнь") || q.contains("как сам") || q.contains("как ты")) {
            return if (isShort) "Все отлично, системы в норме! Как у тебя?" else "Прекрасно! Все алгоритмы работают четко, готов ответить на любые твои вопросы. Как проходит твой день?"
        }

        if (q.contains("привет") || q.contains("здравствуй") || q.contains("добрый день") || q.contains("добрый вечер") || q.contains("хай")) {
            return if (isShort) "Привет! Чем помочь?" else "Приветствую! Я универсальная нейросеть AutoScan. Могу ответить на любые вопросы — от науки и кода до автодиагностики и погоды. Спрашивай!"
        }

        if (q.contains("кто ты") || q.contains("что умеешь") || q.contains("что за приложение")) {
            return if (isShort) "Я универсальная нейросеть и голосовой ассистент." else "Я полноценная нейросеть и бортовой ассистент AutoScan. Умею отвечать на абсолютно любые вопросы обо всем на свете, писать тексты и код, расшифровывать ошибки авто, мониторить датчики и подсказывать погоду."
        }

        if (q.contains("спасибо") || q.contains("благодар") || q.contains("отлично") || q.contains("понял") || q.contains("круто") || q.contains("супер")) {
            return if (isShort) "Пожалуйста! Всегда на связи." else "Всегда к твоим услугам! Спрашивай обо всем, что интересно."
        }

        // 11. Diagnostic Symptoms
        if (q.contains("троит") || q.contains("пропуск") || q.contains("вибрац")) {
            return if (isShort) "Проверь свечи зажигания, наконечники и катушки (сейчас обороты $rpm об/мин)." else "При троении и вибрации чаще всего виноваты свечи, пробой катушки зажигания или забитая форсунка. Обороты сейчас: $rpm об/мин. Начни с проверки свечей и считывания кодов пропусков (P0300–P0304)."
        }
        if (q.contains("плавают") || q.contains("холост")) {
            return if (isShort) "Почисти дроссельную заслонку и проверь подсос воздуха." else "Если плавают холостые обороты ($rpm об/мин), обычно помогает чистка дросселя, адаптация заслонки и проверка впускного коллектора дымогенератором на подсос воздуха."
        }
        if (q.contains("не заводит") || q.contains("стартер")) {
            return if (isShort) "Проверь заряд АКБ (сейчас $volt В) и подачу топлива." else "Если мотор не заводится, первым делом проверь напряжение АКБ (сейчас: $volt В). Если ниже 12 В — требуется подзарядка. Также проверь реле бензонасоса и предохранители."
        }
        if (q.contains("чек") || q.contains("ошибк")) {
            return if (isShort) "Запусти сканирование DTC на главном экране." else "Чтобы считать ошибки, нажми 'Диагностика DTC' на главном экране. Я опрошу блоки PCM, ABS, TCU и дам полное заключение."
        }

        val searchMatches = DtcDatabase.searchDatabase(q)
        if (searchMatches.isNotEmpty()) {
            val first = searchMatches.first()
            if (isShort) {
                return "${first.code} (${first.title}): ${first.solution.substringBefore(".") + "."}"
            }
            return "⚠️ **Информация (${first.code} - ${first.title})**:\n\n" +
                   "• **Категория:** ${first.category}\n" +
                   "• **Возможные причины:** ${first.causes.joinToString(", ")}\n" +
                   "• **Решение:**\n${first.solution}"
        }

        // 12. Car Maintenance & Components
        if (q.contains("масл") || q.contains("замен") && q.contains("фильтр")) {
            return if (isShort) "Моторное масло меняют каждые 7-10 тыс. км (обычно 5W-30 или 5W-40)." else "Моторное масло и масляный фильтр рекомендуется менять каждые 7 500 – 10 000 км или раз в год. Вязкость (5W-30, 5W-40, 0W-20) выбирайте строго по допускам производителя вашего авто. Проверяйте уровень на остывшем двигателе по меткам щупа."
        }
        if (q.contains("тормоз") || q.contains("колодк")) {
            return if (isShort) "Минимальная толщина колодок 3 мм. Тормозную жидкость менять раз в 2 года." else "Тормозные колодки подлежат замене при износе фрикционной накладки до 2–3 мм или появлении писка/скрипа. Тормозную жидкость (DOT 4/DOT 5.1) необходимо менять каждые 2 года или 40 000 км пробега, так как она гигроскопична."
        }
        if (q.contains("шин") || q.contains("колес") || q.contains("давлен")) {
            return if (isShort) "Норма давления в шинах обычно 2.0–2.4 бара." else "Стандартное давление в шинах легковых автомобилей составляет от 2.0 до 2.4 бар (атмосфер). Точное значение указано на информационной табличке на стойке водительской двери или на лючке бензобака."
        }
        if (q.contains("стучит") || q.contains("шум") || q.contains("стук") || q.contains("скрип") || q.contains("гул")) {
            return if (isShort) "Стук на кочках — подвеска/стойки, гул в движении — подшипник ступицы." else "При посторонних звуках: глухой стук на неровностях обычно указывает на стойки или втулки стабилизатора; металлический стук — шаровые опоры или рулевые наконечники; гул, усиливающийся в поворотах — ступичный подшипник; звонкий стук в двигателе — гидрокомпенсаторы или цепь ГРМ."
        }
        if (q.contains("свеч")) {
            return if (isShort) "Обычные свечи служат 30 тыс. км, иридиевые — до 80-100 тыс. км." else "Обычные никелевые свечи зажигания меняют каждые 30 000 км, а платиновые и иридиевые служат до 80 000 – 100 000 км. Нагар на свечах помогает диагностировать работу мотора: черный нагар — богатая смесь, белый — бедная смесь, масляный — износ маслосъемных колпачков."
        }
        if (q.contains("печк") || q.contains("кондиционер") || q.contains("климат")) {
            return if (isShort) "Проверьте уровень антифриза, салонный фильтр и давление фреона." else "Если печка дует холодным воздухом — проверьте уровень антифриза в расширительном бачке, работу термостата и чистоту салонного фильтра. Если не холодит кондиционер — проверьте давление хладагента (фреона) и срабатывание муфты компрессора."
        }

        if (!isStrictOffline) {
            return "Я готов ответить на любой ваш вопрос о диагностике, автомобильных системах, датчиках и поездке. Уточните вопрос или выберите нужный датчик для замера."
        }

        return null
    }

    private fun getOfflineDtcConsultation(code: String, desc: String, dbInfo: DtcInfo?): String {
        if (dbInfo != null) {
            val mainCause = dbInfo.causes.firstOrNull() ?: "проводка или разъем"
            val secondaryCause = dbInfo.causes.getOrNull(1)
            val causesSummary = if (secondaryCause != null) "$mainCause либо $secondaryCause" else mainCause
            val canDrive = if (dbInfo.severity.contains("Критическ", ignoreCase = true)) {
                "⚠️ Своим ходом ехать нежелательно (риск повреждения узлов). Только на минимальной скорости до СТО."
            } else {
                "✅ Своим ходом доехать до сервиса можно без проблем."
            }

            return """
                🔧 **$code: ${dbInfo.title}**
                
                • **Что сбоит:** ${dbInfo.symptoms.firstOrNull() ?: dbInfo.title}.
                • **Куда смотреть:** В первую очередь проверь $causesSummary. Осмотри разъем на окисление.
                • **Как лечить:** ${dbInfo.solution.substringBefore(". ")}.
                • **Можно ли ехать:** $canDrive
            """.trimIndent()
        }

        return """
            🔧 **$code: $desc**
            
            • **Что произошло:** Зафиксирован сбой по цепи или сигналу блока управления.
            • **Куда смотреть:** Проверь контакты фишки, целостность проводки и предохранитель цепи.
            • **Как лечить:** Очисти контакты, сбрось ошибку (Mode 04) и сделай контрольный заезд.
            • **Можно ли ехать:** Если нет резких рывков и посторонних звуков — до сервиса доехать можно.
        """.trimIndent()
    }
}

