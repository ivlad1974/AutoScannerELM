package com.example

import com.example.data.formatDecimalNumberToRussianWords
import com.example.data.intToRussianWords
import com.example.data.tts.TextToSpeechHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testTenthsPronunciation() {
        assertEquals("четырнадцать и две десятых", formatDecimalNumberToRussianWords(14, "2"))
        assertEquals("один и шесть десятых", formatDecimalNumberToRussianWords(1, "6"))
        assertEquals("ноль и восемь десятых", formatDecimalNumberToRussianWords(0, "8"))
        assertEquals("ноль и одна десятая", formatDecimalNumberToRussianWords(0, "1"))
        assertEquals("два и четыре десятых", formatDecimalNumberToRussianWords(2, "4"))
        assertEquals("пятнадцать и шесть десятых", formatDecimalNumberToRussianWords(15, "6"))
        assertEquals("двенадцать и шесть десятых", formatDecimalNumberToRussianWords(12, "6"))
    }

    @Test
    fun testHundredthsPronunciation() {
        assertEquals("ноль и восемьдесят пять сотых", formatDecimalNumberToRussianWords(0, "85"))
        assertEquals("ноль и пять сотых", formatDecimalNumberToRussianWords(0, "05"))
        assertEquals("ноль и двадцать одна сотая", formatDecimalNumberToRussianWords(0, "21"))
        assertEquals("один и двадцать пять сотых", formatDecimalNumberToRussianWords(1, "25"))
        assertEquals("четырнадцать и двадцать пять сотых", formatDecimalNumberToRussianWords(14, "25"))
    }

    @Test
    fun testTrailingZerosInDecimals() {
        // 14.20 should simplify to tenths: "четырнадцать и две десятых"
        assertEquals("четырнадцать и две десятых", formatDecimalNumberToRussianWords(14, "20"))
        // 0.50 should simplify to "ноль и пять десятых"
        assertEquals("ноль и пять десятых", formatDecimalNumberToRussianWords(0, "50"))
        // 14.00 should be whole "четырнадцать"
        assertEquals("четырнадцать", formatDecimalNumberToRussianWords(14, "00"))
    }

    @Test
    fun testSignedDecimals() {
        assertEquals("минус два и пять десятых", formatDecimalNumberToRussianWords(2, "5", "минус "))
        assertEquals("плюс три и пять десятых", formatDecimalNumberToRussianWords(3, "5", "плюс "))
    }

    @Test
    fun testFullTextNormalizationInDialogues() {
        // Pressure ranges:
        val pressure = TextToSpeechHelper.normalizeTextForSpeech("Нормальное давление 2.2 - 2.4 бара.")
        assertTrue(pressure.contains("два и две десятых до два и четыре десятых бар"))

        // Voltage:
        val voltage = TextToSpeechHelper.normalizeTextForSpeech("Напряжение АКБ 14.2 В, на заглушенном 12.6В.")
        println("TEST VOLTAGE OUTPUT: [$voltage]")
        assertTrue("Expected 14.2 to be voiced, but was: $voltage", voltage.contains("четырнадцать и две десятых вольт"))
        assertTrue(voltage.contains("двенадцать и шесть десятых вольт"))

        // Gap in spark plugs (hundredths):
        val spark = TextToSpeechHelper.normalizeTextForSpeech("Зазор свечей зажигания 0.85 мм.")
        assertTrue(spark.contains("ноль и восемьдесят пять сотых миллиметра"))

        // Fuel trim (negative with percent):
        val trim = TextToSpeechHelper.normalizeTextForSpeech("Коррекция смеси -2.34%.")
        assertTrue(trim.contains("минус два и тридцать четыре сотых процента"))

        // Temperature (whole degrees only):
        val temp = TextToSpeechHelper.normalizeTextForSpeech("Температура двигателя 92.4°C, на улице +24.2 градуса.")
        assertTrue(temp.contains("девяносто два градуса"))
        assertTrue(temp.contains("плюс двадцать четыре градуса"))

        // Acceleration:
        val accel = TextToSpeechHelper.normalizeTextForSpeech("Разгон до 100 км/ч занимает 8.5 секунд.")
        assertTrue(accel.contains("восемь и пять десятых секунд"))

        // Valve gap:
        val valves = TextToSpeechHelper.normalizeTextForSpeech("Зазор впускных клапанов 0.05 мм.")
        assertTrue(valves.contains("ноль и пять сотых миллиметра"))
    }

    @Test
    fun testRangePronunciationDoesNotSayMinus() {
        // Range 22-24:
        val range1 = TextToSpeechHelper.normalizeTextForSpeech("диапазон 22-24")
        println("RANGE 1: $range1")
        assertTrue(!range1.contains("минус"))
        assertTrue(range1.contains("от 22 до 24"))

        // Standalone range 22-24 with degrees:
        val range2 = TextToSpeechHelper.normalizeTextForSpeech("температура в салоне 22-24 градуса")
        println("RANGE 2: $range2")
        assertTrue(!range2.contains("минус"))
        assertTrue(range2.contains("от двадцать два до двадцать четыре градуса"))

        // Range with °C:
        val rangeCelsius = TextToSpeechHelper.normalizeTextForSpeech("температура 22-24°C")
        println("RANGE CELSIUS: $rangeCelsius")
        assertTrue(!rangeCelsius.contains("минус"))
        assertTrue(rangeCelsius.contains("от двадцать два до двадцать четыре градуса Цельсия"))

        // Formula with minus:
        val formula = TextToSpeechHelper.normalizeTextForSpeech("10 - 4 = 6")
        println("FORMULA: $formula")
        assertTrue(formula.contains("минус"))
        assertTrue(formula.contains("10 минус 4 равно 6"))

        // Genuine negative temperature:
        val negativeTemp = TextToSpeechHelper.normalizeTextForSpeech("на улице -15°C")
        println("NEGATIVE TEMP: $negativeTemp")
        assertTrue(negativeTemp.contains("минус"))
        assertTrue(negativeTemp.contains("пятнадцать градусов"))
    }

    @Test
    fun testUnitsPronunciation() {
        // Meters per second (м/с):
        val ms5 = TextToSpeechHelper.normalizeTextForSpeech("Скорость ветра 5 м/с")
        println("MS 5: $ms5")
        assertTrue(ms5.contains("5 метров в секунду"))

        val ms1 = TextToSpeechHelper.normalizeTextForSpeech("Скорость потока 1 м/с")
        println("MS 1: $ms1")
        assertTrue(ms1.contains("1 метр в секунду"))

        val ms2 = TextToSpeechHelper.normalizeTextForSpeech("Скорость 2 м/с")
        println("MS 2: $ms2")
        assertTrue(ms2.contains("2 метра в секунду"))

        val msWord = TextToSpeechHelper.normalizeTextForSpeech("составляет двадцать пять м/с")
        println("MS WORD: $msWord")
        assertTrue(msWord.contains("двадцать пять метров в секунду"))

        // Kilometers per hour (км/ч):
        val kmh = TextToSpeechHelper.normalizeTextForSpeech("Скорость 60 км/ч")
        println("KMH: $kmh")
        assertTrue(kmh.contains("60 километров в час"))

        // RPM (об/мин):
        val rpm = TextToSpeechHelper.normalizeTextForSpeech("Обороты 850 об/мин")
        println("RPM: $rpm")
        assertTrue(rpm.contains("850 оборотов в минуту"))

        // Volts (В):
        val volts = TextToSpeechHelper.normalizeTextForSpeech("Напряжение 14.2 В")
        println("VOLTS: $volts")
        assertTrue(volts.contains("четырнадцать и две десятых вольта"))
    }
}
