package com.example.data.elm327

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round

/**
 * Lightweight and robust expression evaluator for SAE J1979 and Torque-style OBD formulas.
 *
 * Supported variables:
 *   A -> bytes[0]
 *   B -> bytes[1]
 *   C -> bytes[2]
 *   D -> bytes[3]
 *   ... up to Z
 *
 * Supported operators: +, -, *, /, %, ^, parentheses
 * Supported functions: abs(x), min(x,y), max(x,y), round(x)
 * Signed 8-bit conversions: SIGNED(A)
 */
object FormulaEvaluator {

    fun evaluate(formula: String, bytes: IntArray): Double? {
        if (formula.isBlank()) {
            return when (bytes.size) {
                0 -> 0.0
                1 -> bytes[0].toDouble()
                2 -> 256.0 * bytes[0] + bytes[1]
                else -> 256.0 * bytes[0] + bytes[1]
            }
        }

        return try {
            val sanitized = formula.trim()
                .replace(" ", "")
                .replace(",", ".") // allow comma in decimals

            val parser = Parser(sanitized, bytes)
            val result = parser.parse()
            if (result.isNaN() || result.isInfinite()) null else result
        } catch (e: Exception) {
            null
        }
    }

    private class Parser(private val input: String, private val bytes: IntArray) {
        private var pos = 0

        private fun peek(): Char = if (pos < input.length) input[pos] else '\u0000'
        private fun get(): Char = if (pos < input.length) input[pos++] else '\u0000'

        fun parse(): Double {
            val res = parseExpression()
            return res
        }

        // Expression: Term (('+' | '-') Term)*
        private fun parseExpression(): Double {
            var v = parseTerm()
            while (true) {
                when (peek()) {
                    '+' -> {
                        get()
                        v += parseTerm()
                    }
                    '-' -> {
                        get()
                        v -= parseTerm()
                    }
                    else -> return v
                }
            }
        }

        // Term: Factor (('*' | '/' | '%') Factor)*
        private fun parseTerm(): Double {
            var v = parseFactor()
            while (true) {
                when (peek()) {
                    '*' -> {
                        get()
                        v *= parseFactor()
                    }
                    '/' -> {
                        get()
                        val denom = parseFactor()
                        v = if (denom == 0.0) 0.0 else v / denom
                    }
                    '%' -> {
                        get()
                        val denom = parseFactor()
                        v = if (denom == 0.0) 0.0 else v % denom
                    }
                    else -> return v
                }
            }
        }

        // Factor: Power ('^' Power)*
        private fun parseFactor(): Double {
            var v = parseUnary()
            if (peek() == '^') {
                get()
                val exponent = parseFactor()
                v = v.pow(exponent)
            }
            return v
        }

        // Unary: ('+' | '-')? Primary
        private fun parseUnary(): Double {
            if (peek() == '+') {
                get()
                return parseUnary()
            }
            if (peek() == '-') {
                get()
                return -parseUnary()
            }
            return parsePrimary()
        }

        // Primary: Number | Variable | Function | '(' Expression ')'
        private fun parsePrimary(): Double {
            val c = peek()

            if (c == '(') {
                get()
                val v = parseExpression()
                if (peek() == ')') get()
                return v
            }

            // Functions or Variables
            if (c.isLetter()) {
                val sb = StringBuilder()
                while (peek().isLetter() || peek() == '_') {
                    sb.append(get())
                }
                val token = sb.toString().uppercase()

                // Check function calls
                if (peek() == '(') {
                    get() // consume '('
                    val arg1 = parseExpression()
                    var arg2 = 0.0
                    if (peek() == ';') {
                        get()
                        arg2 = parseExpression()
                    }
                    if (peek() == ')') get()

                    return when (token) {
                        "ABS" -> abs(arg1)
                        "MIN" -> min(arg1, arg2)
                        "MAX" -> max(arg1, arg2)
                        "ROUND" -> round(arg1)
                        "SIGNED" -> {
                            val raw = arg1.toInt() and 0xFF
                            if (raw > 127) (raw - 256).toDouble() else raw.toDouble()
                        }
                        else -> arg1
                    }
                }

                // Variable A..Z
                if (token.length == 1) {
                    val idx = token[0] - 'A'
                    if (idx in bytes.indices) {
                        return (bytes[idx] and 0xFF).toDouble()
                    }
                    return 0.0
                }

                // Known aliases
                if (token == "PI") return Math.PI
                return 0.0
            }

            // Number
            if (c.isDigit() || c == '.') {
                val start = pos
                var hasDot = false
                while (peek().isDigit() || (!hasDot && peek() == '.')) {
                    if (peek() == '.') hasDot = true
                    get()
                }
                val numStr = input.substring(start, pos)
                return numStr.toDoubleOrNull() ?: 0.0
            }

            // Skip unexpected char
            get()
            return 0.0
        }
    }
}
