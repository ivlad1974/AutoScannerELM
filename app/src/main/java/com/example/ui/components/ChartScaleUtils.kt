package com.example.ui.components

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AutomotiveBlue
import com.example.ui.theme.ClearRed
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.WarningAmber

data class ChartAxisTick(
    val value: Double,
    val formattedText: String
)

data class ChartScaleInfo(
    val baseMin: Double,
    val baseMax: Double,
    val totalRange: Double,
    val ticks: List<ChartAxisTick>
)

/**
 * Calculates human-readable, adequately rounded axis bounds and steps (e.g. RPM multiples of 100/500/1000, Volts 1V/0.5V).
 */
fun calculateAdequateAxisScale(
    minVal: Double,
    maxVal: Double,
    pid: String = "",
    unit: String = "",
    targetTickCount: Int = 4
): ChartScaleInfo {
    val rawMin = if (minVal.isInfinite() || minVal.isNaN()) 0.0 else minVal
    val rawMax = if (maxVal.isInfinite() || maxVal.isNaN()) 1.0 else maxVal
    val diff = (rawMax - rawMin).coerceAtLeast(0.0)

    val step: Double = when {
        // Обороты двигателя (RPM) -> строго кратно 100, 200, 500 или 1000 об/мин
        pid == "010C" || unit.contains("об/мин", ignoreCase = true) || unit.contains("rpm", ignoreCase = true) -> {
            when {
                diff > 4500 -> 1000.0
                diff > 1800 -> 500.0
                diff > 700 -> 200.0
                diff > 250 -> 100.0
                diff > 100 -> 50.0
                else -> 50.0
            }
        }
        // Напряжение АКБ -> 0.5V, 1.0V или 2.0V
        pid == "0142" || unit.equals("В", ignoreCase = true) || unit.equals("V", ignoreCase = true) -> {
            when {
                diff > 6.0 -> 2.0
                diff > 2.0 -> 1.0
                diff > 0.8 -> 0.5
                diff > 0.3 -> 0.2
                else -> 0.1
            }
        }
        // Скорость км/ч -> 10, 20, 50 км/ч
        pid == "010D" || unit.contains("км/ч", ignoreCase = true) || unit.contains("km/h", ignoreCase = true) -> {
            when {
                diff > 100 -> 20.0
                diff > 40 -> 10.0
                diff > 15 -> 5.0
                else -> 2.0
            }
        }
        // Температура °C -> 5, 10, 20 °C
        pid in listOf("0105", "015C", "0146") || unit.contains("°C", ignoreCase = true) -> {
            when {
                diff > 60 -> 20.0
                diff > 25 -> 10.0
                diff > 10 -> 5.0
                else -> 2.0
            }
        }
        // Проценты %
        unit.contains("%") -> {
            when {
                diff > 50 -> 25.0
                diff > 25 -> 10.0
                diff > 10 -> 5.0
                else -> 2.0
            }
        }
        else -> {
            val range = if (diff <= 0.001) 1.0 else diff
            val roughStep = range / targetTickCount.toDouble()
            val exponent = Math.floor(Math.log10(roughStep))
            val magnitude = Math.pow(10.0, exponent)
            val mantissa = roughStep / magnitude
            val niceMantissa = when {
                mantissa < 1.5 -> 1.0
                mantissa < 3.5 -> 2.0
                mantissa < 7.5 -> 5.0
                else -> 10.0
            }
            niceMantissa * magnitude
        }
    }

    var baseMin = Math.floor(rawMin / step) * step
    var baseMax = Math.ceil(rawMax / step) * step

    if (baseMax - baseMin < step * 2) {
        baseMin -= step
        baseMax += step
    }

    val totalRange = (baseMax - baseMin).coerceAtLeast(step)

    val ticks = mutableListOf<ChartAxisTick>()
    var currentTick = baseMin
    val epsilon = step * 0.0001

    while (currentTick <= baseMax + epsilon) {
        val formatted = when {
            step >= 1.0 && Math.abs(currentTick - Math.round(currentTick)) < 0.001 -> {
                String.format(java.util.Locale.US, "%.0f", currentTick)
            }
            step >= 0.1 -> {
                String.format(java.util.Locale.US, "%.1f", currentTick)
            }
            else -> {
                String.format(java.util.Locale.US, "%.2f", currentTick)
            }
        }
        ticks.add(ChartAxisTick(currentTick, formatted))
        currentTick += step
    }

    return ChartScaleInfo(
        baseMin = baseMin,
        baseMax = baseMax,
        totalRange = totalRange,
        ticks = ticks
    )
}

/**
 * Returns the exact color for a specific point value on a sensor telemetry graph.
 * Ensures that different sections of the graph line and area are painted in colors
 * that strictly correspond to that value range (e.g. cold = blue, normal = green, warning = amber, danger = red).
 */
fun getSensorPointColor(
    pidOrName: String,
    value: Double,
    isConnected: Boolean = true,
    customThreshold: Double? = null,
    isGreaterThanCondition: Boolean = true
): Color {
    if (!isConnected) return Color.Gray
    val key = pidOrName.uppercase().trim()

    // 1. If custom user threshold exists for this sensor, use exact alert condition
    if (customThreshold != null) {
        val isExceeded = if (isGreaterThanCondition) value >= customThreshold else value <= customThreshold
        if (isExceeded) return ClearRed
        val warningZone = if (isGreaterThanCondition) (customThreshold * 0.88) else (customThreshold * 1.12)
        val isWarning = if (isGreaterThanCondition) value >= warningZone else value <= warningZone
        if (isWarning) return WarningAmber
        return StatusGreen
    }

    // 2. Standard automotive physical sensor value zones
    return when {
        // Охлаждающая жидкость (Coolant Temp °C)
        key == "0105" || key.contains("ОХЛАЖД") || key.contains("COOLANT") || key.contains("ТЕМПЕРАТУРА ОЖ") || (key.contains("ДВИГАТЕЛ") && key.contains("ТЕМП")) -> when {
            value > 103.0 -> ClearRed // Перегрев
            value > 96.0 -> WarningAmber // Повышенная
            value >= 75.0 -> StatusGreen // Оптимальная рабочая
            value >= 50.0 -> AutomotiveBlue // Прогрев
            else -> Color(0xFF38BDF8) // Холодный ДВС (<50°C)
        }
        // Обороты двигателя (RPM)
        key == "010C" || key.contains("ОБОРОТ") || key.contains("RPM") || key.contains("ТАХОМЕТР") -> when {
            value >= 5500.0 -> ClearRed // Красная зона отсечки
            value >= 3500.0 -> WarningAmber // Высокие обороты
            value >= 850.0 -> StatusGreen // Рабочий диапазон тяги
            value > 0.0 -> AutomotiveBlue // Холостой ход (600-850 об/мин)
            else -> Color(0xFF64748B) // Заглушен
        }
        // Скорость автомобиля (Speed km/h)
        key == "010D" || key.contains("СКОРОСТ") || key.contains("SPEED") || key.contains("СПИДОМЕТР") -> when {
            value > 130.0 -> ClearRed // Превышение магистральной скорости
            value > 90.0 -> WarningAmber // Трассовая скорость
            value > 0.0 -> StatusGreen // Городской темп
            else -> Color(0xFF64748B) // Стоянка
        }
        // Напряжение бортовой сети и АКБ (Battery Volts)
        key == "0142" || key.contains("НАПРЯЖЕН") || key.contains("АКБ") || key.contains("ВОЛЬТ") || key.contains("BATTERY") || key.contains("VOLT") -> when {
            value > 14.9 -> ClearRed // Перезаряд генератора
            value in 13.5..14.8 -> StatusGreen // Нормальная зарядка генератора
            value in 12.2..13.5 -> AutomotiveBlue // Норма на заглушенном / штатная батарея
            value in 11.7..12.2 -> WarningAmber // Разряжен / просадка
            else -> ClearRed // Критический разряд (<11.7В)
        }
        // Уровень топлива в баке (%)
        key == "012F" || key.contains("ТОПЛИВ") || key.contains("FUEL") || key.contains("БАК") -> when {
            value < 12.0 -> ClearRed // Критический остаток топлива
            value < 25.0 -> WarningAmber // Четверть бака
            else -> StatusGreen // Достаточный запас
        }
        // Положение дросселя и нагрузка (%): 0-35% зел, 35-70% оранж, >70% красн
        key == "0104" || key == "0111" || key == "0149" || key.contains("ДРОССЕЛ") || key.contains("НАГРУЗК") || key.contains("ПЕДАЛ") || key.contains("THROTTLE") || key.contains("LOAD") -> when {
            value > 80.0 -> ClearRed // Газ в пол / максимальная нагрузка
            value > 50.0 -> WarningAmber // Ускорение
            value > 0.0 -> StatusGreen // Экономная спокойная езда
            else -> AutomotiveBlue
        }
        // Температура масла (°C)
        key == "015C" || key.contains("МАСЛ") || key.contains("OIL") -> when {
            value > 125.0 -> ClearRed // Перегрев масла
            value > 110.0 -> WarningAmber // Повышенная нагрузка
            value >= 80.0 -> StatusGreen // Рабочая температура масла
            else -> AutomotiveBlue // Не прогрето
        }
        // Давление во впускном коллекторе (MAP / Boost kPa)
        key == "010B" || key.contains("ДАВЛЕН") || key.contains("MAP") || key.contains("BOOST") -> when {
            value > 180.0 -> ClearRed // Высокий наддув
            value > 120.0 -> WarningAmber // Турбо наддув
            value >= 30.0 -> StatusGreen // Нормальное атмосферное разряжение
            else -> AutomotiveBlue
        }
        // Расход воздуха (MAF g/s)
        key == "0110" || key.contains("ВОЗДУХ") || key.contains("MAF") -> when {
            value > 80.0 -> ClearRed
            value > 30.0 -> WarningAmber
            value >= 2.0 -> StatusGreen
            else -> AutomotiveBlue
        }
        // Температура впуска (°C)
        key == "010F" || key == "0146" || key.contains("ВПУСК") || key.contains("IAT") -> when {
            value > 65.0 -> ClearRed // Горячий впуск
            value > 45.0 -> WarningAmber
            value >= 10.0 -> StatusGreen
            else -> Color(0xFF38BDF8) // Холодный воздух
        }
        // Топливные коррекции (STFT/LTFT %)
        key == "0106" || key == "0107" || key.contains("КОРРЕКЦ") || key.contains("TRIM") -> {
            val absVal = kotlin.math.abs(value)
            when {
                absVal > 15.0 -> ClearRed // Сильная бедная/богатая смесь
                absVal > 7.0 -> WarningAmber // Повышенная коррекция
                else -> StatusGreen // Идеальная стехиометрия (+-7%)
            }
        }
        // Расход топлива (л/100км)
        key == "015E" || key == "015F" || key.contains("РАСХОД") -> when {
            value > 18.0 -> ClearRed
            value > 12.0 -> WarningAmber
            value > 0.0 -> StatusGreen
            else -> AutomotiveBlue
        }
        else -> {
            when {
                value > 100.0 -> WarningAmber
                value > 0.0 -> StatusGreen
                else -> AutomotiveBlue
            }
        }
    }
}
