package com.example.data.ai

import android.content.Context
import android.location.Location
import android.location.LocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class LiveWeatherReport(
    val cityName: String,
    val temperature: Double,
    val apparentTemperature: Double,
    val conditionDescription: String,
    val humidity: Int,
    val windSpeedKmH: Double,
    val precipitation: Double,
    val tempMax: Double? = null,
    val tempMin: Double? = null,
    val precipitationProbability: Int? = null,
    val summaryRussian: String
)

class LiveInternetService(private val context: Context? = null) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    // Pre-calculated coordinates for top 60+ cities for instant response
    private val fastCityMap = mapOf(
        "челябинск" to Triple(55.1644, 61.4368, "Челябинск"),
        "москва" to Triple(55.7558, 37.6173, "Москва"),
        "санкт-петербург" to Triple(59.9343, 30.3351, "Санкт-Петербург"),
        "питер" to Triple(59.9343, 30.3351, "Санкт-Петербург"),
        "екатеринбург" to Triple(56.8389, 60.6057, "Екатеринбург"),
        "казань" to Triple(55.7887, 49.1221, "Казань"),
        "нижний новгород" to Triple(56.3269, 44.0059, "Нижний Новгород"),
        "новосибирск" to Triple(55.0084, 82.9357, "Новосибирск"),
        "самара" to Triple(53.1959, 50.1002, "Самара"),
        "омск" to Triple(54.9885, 73.3242, "Омск"),
        "ростов-на-дону" to Triple(47.2357, 39.7015, "Ростов-на-Дону"),
        "ростов" to Triple(47.2357, 39.7015, "Ростов-на-Дону"),
        "уфа" to Triple(54.7388, 55.9721, "Уфа"),
        "красноярск" to Triple(56.0153, 92.8932, "Красноярск"),
        "воронеж" to Triple(51.6755, 39.2089, "Воронеж"),
        "пермь" to Triple(58.0105, 56.2502, "Пермь"),
        "волгоград" to Triple(48.7080, 44.5133, "Волгоград"),
        "краснодар" to Triple(45.0393, 38.9872, "Краснодар"),
        "саратов" to Triple(51.5406, 46.0086, "Саратов"),
        "тюмень" to Triple(57.1522, 65.5272, "Тюмень"),
        "тольятти" to Triple(53.5303, 49.3461, "Тольятти"),
        "барнаул" to Triple(53.3606, 83.7636, "Барнаул"),
        "ижевск" to Triple(56.8528, 53.2115, "Ижевск"),
        "ульяновск" to Triple(54.3142, 48.4031, "Ульяновск"),
        "иркутск" to Triple(52.2870, 104.3050, "Иркутск"),
        "хабаровск" to Triple(48.4802, 135.0719, "Хабаровск"),
        "ярославль" to Triple(57.6261, 39.8845, "Ярославль"),
        "владивосток" to Triple(43.1198, 131.8869, "Владивосток"),
        "махачкала" to Triple(42.9849, 47.5046, "Махачкала"),
        "томск" to Triple(56.4977, 84.9744, "Томск"),
        "оренбург" to Triple(51.7727, 55.0988, "Оренбург"),
        "кемерово" to Triple(55.3547, 86.0873, "Кемерово"),
        "новокузнецк" to Triple(53.7596, 87.1216, "Новокузнецк"),
        "рязань" to Triple(54.6269, 39.6916, "Рязань"),
        "астрахань" to Triple(46.3497, 48.0408, "Астрахань"),
        "пенза" to Triple(53.2007, 45.0046, "Пенза"),
        "липецк" to Triple(52.6103, 39.5947, "Липецк"),
        "тула" to Triple(54.1961, 37.6182, "Тула"),
        "киров" to Triple(58.6035, 49.6679, "Киров"),
        "чебоксары" to Triple(56.1439, 47.2489, "Чебоксары"),
        "калининград" to Triple(54.7104, 20.4522, "Калининград"),
        "брянск" to Triple(53.2436, 34.3634, "Брянск"),
        "курск" to Triple(51.7304, 36.1927, "Курск"),
        "иваново" to Triple(56.9972, 40.9714, "Иваново"),
        "магнитогорск" to Triple(53.4072, 58.9791, "Магнитогорск"),
        "улан-удэ" to Triple(51.8345, 107.5846, "Улан-Удэ"),
        "тверь" to Triple(56.8587, 35.9176, "Тверь"),
        "ставрополь" to Triple(45.0428, 41.9734, "Ставрополь"),
        "белгород" to Triple(50.5997, 36.5983, "Белгород"),
        "сочи" to Triple(43.6028, 39.7342, "Сочи"),
        "сургут" to Triple(61.2540, 73.3962, "Сургут"),
        "владимир" to Triple(56.1290, 40.4066, "Владимир"),
        "архангельск" to Triple(64.5401, 40.5433, "Архангельск"),
        "калуга" to Triple(54.5293, 36.2754, "Калуга"),
        "смоленск" to Triple(54.7818, 32.0401, "Смоленск"),
        "мурманск" to Triple(68.9585, 33.0827, "Мурманск"),
        "чита" to Triple(52.0336, 113.5010, "Чита"),
        "орёл" to Triple(52.9651, 36.0785, "Орёл"),
        "орел" to Triple(52.9651, 36.0785, "Орёл"),
        "якутск" to Triple(62.0355, 129.6755, "Якутск"),
        "вологда" to Triple(59.2205, 39.8915, "Вологда"),
        "грозный" to Triple(43.3180, 45.6986, "Грозный"),
        "минск" to Triple(53.9006, 27.5590, "Минск"),
        "алматы" to Triple(43.2220, 76.8512, "Алматы"),
        "астана" to Triple(51.1694, 71.4491, "Астана")
    )

    suspend fun getLiveWeather(userQuery: String? = null): LiveWeatherReport? = withContext(Dispatchers.IO) {
        try {
            // 1. Extract city name from query if specified
            val targetCity = extractCityFromQuery(userQuery)

            var lat = 55.7558
            var lon = 37.6173
            var resolvedCityName = "Москва"

            if (!targetCity.isNullOrBlank()) {
                val normalizedTarget = normalizeRussianCityName(targetCity)
                val fastMatch = fastCityMap[normalizedTarget] ?: fastCityMap[targetCity.lowercase()]
                if (fastMatch != null) {
                    lat = fastMatch.first
                    lon = fastMatch.second
                    resolvedCityName = fastMatch.third
                } else {
                    val geo = geocodeCity(normalizedTarget) ?: geocodeCity(targetCity)
                    if (geo != null) {
                        lat = geo.first
                        lon = geo.second
                        resolvedCityName = geo.third
                    } else {
                        resolvedCityName = normalizedTarget.replaceFirstChar { it.uppercase() }
                    }
                }
            } else {
                // Try device GPS location or IP geolocation
                val loc = getDeviceLocation()
                if (loc != null) {
                    lat = loc.latitude
                    lon = loc.longitude
                    resolvedCityName = "Вашем районе"
                } else {
                    val ipLoc = getIpLocation()
                    if (ipLoc != null) {
                        lat = ipLoc.first
                        lon = ipLoc.second
                        resolvedCityName = ipLoc.third
                    }
                }
            }

            // 2. Fetch Open-Meteo live weather
            val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m" +
                    "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=auto"

            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            val current = json.optJSONObject("current") ?: return@withContext null
            val daily = json.optJSONObject("daily")

            val temp = current.optDouble("temperature_2m", 0.0)
            val apparent = current.optDouble("apparent_temperature", temp)
            val humidity = current.optInt("relative_humidity_2m", 50)
            val windSpeed = current.optDouble("wind_speed_10m", 0.0)
            val precip = current.optDouble("precipitation", 0.0)
            val weatherCode = current.optInt("weather_code", 0)

            val conditionDesc = parseWeatherCode(weatherCode)
            val tempMax = daily?.optJSONArray("temperature_2m_max")?.optDouble(0)
            val tempMin = daily?.optJSONArray("temperature_2m_min")?.optDouble(0)
            val precipProb = daily?.optJSONArray("precipitation_probability_max")?.optInt(0)

            val tempInt = kotlin.math.round(temp).toInt()
            val apparentInt = kotlin.math.round(apparent).toInt()
            val tempStr = if (tempInt > 0) "+$tempInt" else "$tempInt"
            val feelsLikeStr = if (apparentInt > 0) "+$apparentInt" else "$apparentInt"
            val windMs = String.format(java.util.Locale.US, "%.1f", windSpeed / 3.6)

            val summary = buildString {
                append("Погода в $resolvedCityName: $conditionDesc, температура $tempStr°C (ощущается как $feelsLikeStr°C). ")
                append("Ветер: $windMs м/с, влажность $humidity%. ")
                if (precip > 0.0) {
                    append("Осадки: ${String.format(java.util.Locale.US, "%.1f", precip)} мм. ")
                }
                if (tempMax != null && tempMin != null) {
                    val maxInt = kotlin.math.round(tempMax).toInt()
                    val minInt = kotlin.math.round(tempMin).toInt()
                    val maxStr = if (maxInt > 0) "+$maxInt" else "$maxInt"
                    val minStr = if (minInt > 0) "+$minInt" else "$minInt"
                    append("Днем от $minStr°C до $maxStr°C.")
                }
            }

            LiveWeatherReport(
                cityName = resolvedCityName,
                temperature = temp,
                apparentTemperature = apparent,
                conditionDescription = conditionDesc,
                humidity = humidity,
                windSpeedKmH = windSpeed,
                precipitation = precip,
                tempMax = tempMax,
                tempMin = tempMin,
                precipitationProbability = precipProb,
                summaryRussian = summary
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun normalizeRussianCityName(raw: String): String {
        var c = raw.lowercase().trim()
        c = c.replace(Regex("^(г\\.?|город)\\s+"), "")
        c = c.replace(Regex("[.,?!]"), "").trim()

        // Common Russian cases lemmatization
        val lemmatized = when {
            c.endsWith("ске") -> c.removeSuffix("е") // челябинске -> челябинск
            c.endsWith("бурге") -> c.removeSuffix("е") // екатеринбурге -> екатеринбург
            c.endsWith("граде") -> c.removeSuffix("е") // волгограде -> волгоград
            c.endsWith("поле") -> c.removeSuffix("е") // ставрополе -> ставрополь
            c.endsWith("ове") -> c.removeSuffix("е") // иванове -> иваново
            c.endsWith("еве") -> c.removeSuffix("е") // кемерове -> кемерово
            c.endsWith("ве") && c.length > 4 -> c.removeSuffix("е") + "а" // москве -> москва
            c.endsWith("ре") && c.length > 4 -> c.removeSuffix("е") + "а" // самаре -> самара
            c.endsWith("фе") -> c.removeSuffix("е") + "а" // уфе -> уфа
            c.endsWith("не") && c.length > 4 -> c.removeSuffix("е") // воронеже/коломне
            c.endsWith("ни") -> c.removeSuffix("и") + "ь" // казани -> казань, тюмени -> тюмень, перми -> пермь
            c.endsWith("де") -> c.removeSuffix("е")
            c.endsWith("ке") -> c.removeSuffix("е")
            c.endsWith("те") -> c.removeSuffix("е")
            c.endsWith("ме") -> c.removeSuffix("е")
            c.endsWith("ле") -> c.removeSuffix("е")
            c.endsWith("не") -> c.removeSuffix("е")
            c.endsWith("ре") -> c.removeSuffix("е")
            c.endsWith("зе") -> c.removeSuffix("е")
            c.endsWith("се") -> c.removeSuffix("е")
            c.endsWith("ше") -> c.removeSuffix("е")
            c.endsWith("же") -> c.removeSuffix("е")
            else -> c
        }
        return lemmatized
    }

    private fun extractCityFromQuery(query: String?): String? {
        if (query.isNullOrBlank()) return null
        val q = query.lowercase().trim()

        // Pattern matching: "погода в [городе]", "какая погода в [городе]", "прогноз в [городе]", "в [городе] погода", etc.
        val patterns = listOf(
            Regex("(?:погод[аеуы]|прогноз|дожд[ьяи]|снег|температур[аеуы]|градус(?:ов|а)?)\\s+(?:в|по|для|г\\.?|город)?\\s*([а-яa-z\\-]{3,25})", RegexOption.IGNORE_CASE),
            Regex("(?:в|во)\\s+([а-яa-z\\-]{3,25})\\s+(?:погод[аеуы]|прогноз|дожд[ьяи]|снег|температур[аеуы]|градус(?:ов|а)?)", RegexOption.IGNORE_CASE),
            Regex("погод[аеуы]\\s+([а-яa-z\\-]{3,25})", RegexOption.IGNORE_CASE),
            Regex("([а-яa-z\\-]{3,25})\\s+погод[аеуы]", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(q)
            if (match != null && match.groupValues.size > 1) {
                var city = match.groupValues[1].trim()
                city = city.replace(Regex("\\b(сегодня|завтра|сейчас|на|улице|дорогах|какая|какой|сколько|будет|стоит|авто|машине)\\b"), "").trim()
                if (city.length >= 3 && !city.contains("погод") && !city.contains("прогноз") && !city.contains("градус")) {
                    return city
                }
            }
        }

        // Check if query directly contains any known city from fastCityMap
        for ((cityNameKey, _) in fastCityMap) {
            if (q.contains(cityNameKey)) {
                return cityNameKey
            }
        }

        return null
    }

    private fun geocodeCity(cityName: String): Triple<Double, Double, String>? {
        return try {
            val encoded = URLEncoder.encode(cityName, "UTF-8")
            val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=ru&format=json"
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return null

            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val results = json.optJSONArray("results") ?: return null
            if (results.length() == 0) return null

            val first = results.getJSONObject(0)
            val lat = first.getDouble("latitude")
            val lon = first.getDouble("longitude")
            val name = first.optString("name", cityName)
            Triple(lat, lon, name)
        } catch (e: Exception) {
            null
        }
    }

    private fun getDeviceLocation(): Location? {
        if (context == null) return null
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
            val gpsLoc = try { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (e: Exception) { null }
            val netLoc = try { lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (e: Exception) { null }
            gpsLoc ?: netLoc
        } catch (e: Exception) {
            null
        }
    }

    private fun getIpLocation(): Triple<Double, Double, String>? {
        // Provider 1: ipwho.is (fast, no strict rate limit)
        try {
            val req1 = Request.Builder().url("https://ipwho.is/").build()
            val resp1 = httpClient.newCall(req1).execute()
            if (resp1.isSuccessful) {
                val json = JSONObject(resp1.body?.string() ?: "")
                if (json.optBoolean("success", false)) {
                    val lat = json.optDouble("latitude", Double.NaN)
                    val lon = json.optDouble("longitude", Double.NaN)
                    val city = json.optString("city", "Ваш регион")
                    if (!lat.isNaN() && !lon.isNaN()) {
                        return Triple(lat, lon, city)
                    }
                }
            }
        } catch (e: Exception) {}

        // Provider 2: ipapi.co
        try {
            val request = Request.Builder()
                .url("https://ipapi.co/json/")
                .header("User-Agent", "AutoScan-App/1.0")
                .build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val lat = json.optDouble("latitude", Double.NaN)
                val lon = json.optDouble("longitude", Double.NaN)
                val city = json.optString("city", "Ваш регион")
                if (!lat.isNaN() && !lon.isNaN()) {
                    return Triple(lat, lon, city)
                }
            }
        } catch (e: Exception) {}

        return null
    }

    private fun parseWeatherCode(code: Int): String {
        return when (code) {
            0 -> "Ясно, солнечно"
            1 -> "Преимущественно ясно"
            2 -> "Переменная облачность"
            3 -> "Пасмурно"
            45, 48 -> "Туман, ухудшение видимости"
            51, 53, 55 -> "Небольшой моросящий дождь"
            56, 57 -> "Ледяная морось"
            61 -> "Небольшой дождь"
            63 -> "Умеренный дождь"
            65 -> "Сильный ливень"
            66, 67 -> "Ледяной дождь, возможен гололед на дороге"
            71 -> "Небольшой снег"
            73 -> "Снегопад"
            75 -> "Сильный снегопад, метель"
            77 -> "Снежная крупа"
            80, 81, 82 -> "Ливневый дождь"
            85, 86 -> "Ливневый снег"
            95 -> "Гроза"
            96, 99 -> "Гроза с градом"
            else -> "Умеренные погодные условия"
        }
    }

    suspend fun searchWeb(query: String): String? = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext null

        val lowerQ = q.lowercase()
        if (lowerQ.contains("погод") || lowerQ.contains("прогноз") || lowerQ.contains("дожд") || lowerQ.contains("за борт")) {
            val weather = getLiveWeather(q)
            if (weather != null) return@withContext weather.summaryRussian
        }

        val wikiAnswer = searchWikipedia(q)
        if (!wikiAnswer.isNullOrBlank()) {
            return@withContext wikiAnswer
        }

        val ddgAnswer = searchDuckDuckGo(q)
        if (!ddgAnswer.isNullOrBlank()) {
            return@withContext ddgAnswer
        }

        null
    }

    private fun searchWikipedia(query: String): String? {
        return try {
            val cleanQuery = query.replace("(?i)^(кто такой|кто такая|что такое|расскажи про|расскажи о|где находится|когда|почему|сколько|какой|какая|какое|зачем)\\s+".toRegex(), "").trim()
            val targetQuery = if (cleanQuery.length >= 2) cleanQuery else query
            val encoded = URLEncoder.encode(targetQuery, "UTF-8")

            val searchUrl = "https://ru.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encoded&utf8=&format=json"
            val req1 = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "AutoScan-Assistant/1.0 (Android; ru)")
                .build()

            val title = httpClient.newCall(req1).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val body = resp.body?.string() ?: return@use null
                val json = JSONObject(body)
                val searchArr = json.optJSONObject("query")?.optJSONArray("search")
                if (searchArr != null && searchArr.length() > 0) {
                    searchArr.optJSONObject(0)?.optString("title")
                } else null
            } ?: return null

            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val extractUrl = "https://ru.wikipedia.org/w/api.php?action=query&prop=extracts&exintro=true&explaintext=true&titles=$encodedTitle&format=json"
            val req2 = Request.Builder()
                .url(extractUrl)
                .header("User-Agent", "AutoScan-Assistant/1.0 (Android; ru)")
                .build()

            httpClient.newCall(req2).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val body = resp.body?.string() ?: return@use null
                val json = JSONObject(body)
                val pages = json.optJSONObject("query")?.optJSONObject("pages") ?: return@use null
                val firstKey = pages.keys().asSequence().firstOrNull() ?: return@use null
                val pageObj = pages.optJSONObject(firstKey)
                val extract = pageObj?.optString("extract")
                if (!extract.isNullOrBlank()) {
                    val cleaned = extract
                        .replace("\\s*\\([^)]*\\)".toRegex(), "")
                        .replace("\\s*\\[[^\\]]*\\]".toRegex(), "")
                        .replace("\\s+".toRegex(), " ")
                        .trim()
                    val sentences = cleaned.split(". ")
                    val takeSentences = sentences.take(3).joinToString(". ").trim()
                    val resultText = if (takeSentences.endsWith(".")) takeSentences else "$takeSentences."
                    "$title: $resultText"
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun searchDuckDuckGo(query: String): String? {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "AutoScan-Assistant/1.0 (Android)")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val body = resp.body?.string() ?: return@use null
                val json = JSONObject(body)
                val abstractText = json.optString("AbstractText")
                if (abstractText.isNotBlank()) {
                    return@use abstractText
                }
                val relatedTopics = json.optJSONArray("RelatedTopics")
                if (relatedTopics != null && relatedTopics.length() > 0) {
                    val first = relatedTopics.optJSONObject(0)
                    val text = first?.optString("Text")
                    if (!text.isNullOrBlank()) {
                        return@use text
                    }
                }
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
