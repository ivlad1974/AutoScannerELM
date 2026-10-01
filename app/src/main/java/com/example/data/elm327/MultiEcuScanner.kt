package com.example.data.elm327

import android.util.Log
import com.example.data.DtcError
import com.example.data.DtcSeverity
import com.example.data.EcuBlock
import com.example.data.EcuStatus
import kotlinx.coroutines.delay

object MultiEcuScanner {

    private const val TAG = "MultiEcuScanner"

    data class EcuHeaderDefinition(
        val txHeader: String,
        val rxHeader: String,
        val name: String,
        val description: String
    )

    val standardHeaders = listOf(
        EcuHeaderDefinition("7E0", "7E8", "Двигатель (ECM / PCM)", "Электронный блок управления двигателем и впрыском"),
        EcuHeaderDefinition("7E1", "7E9", "Трансмиссия (TCM)", "Контроллер автоматической коробки передач / вариатора / робота"),
        EcuHeaderDefinition("7E2", "7EA", "Тормозная система (ABS / ESP)", "Антиблокировочная система и курсовая устойчивость"),
        EcuHeaderDefinition("7E3", "7EB", "Подушки безопасности (SRS)", "Модуль управления подушками безопасности и натяжителями"),
        EcuHeaderDefinition("7E4", "7EC", "Кузовная электроника (BCM)", "Центральный кузовной блок (свет, замки, комфорт, иммобилайзер)"),
        EcuHeaderDefinition("7E5", "7ED", "Панель приборов (IPC)", "Электронный щиток приборов и индикаторы"),
        EcuHeaderDefinition("7E6", "7EE", "Гибридная установка (HV ECU)", "Контроллер высоковольтной батареи и инвертора"),
        EcuHeaderDefinition("7E7", "7EF", "Центральный шлюз (Gateway)", "Межсетевой интерфейс шин CAN и LIN")
    )

    suspend fun scanAllEcus(
        sendCmd: suspend (String, Long) -> String,
        onProgress: suspend (currentEcu: EcuBlock, scannedList: List<EcuBlock>) -> Unit
    ): Pair<List<EcuBlock>, List<DtcError>> {
        val ecuBlocks = mutableListOf<EcuBlock>()
        val allDtcs = mutableListOf<DtcError>()

        // 1. First probe CAN multi-header capability
        val atShResponse = sendCmd("AT SH 7E0", 300L)
        val canHeadersSupported = !atShResponse.contains("?", ignoreCase = true) && !atShResponse.contains("ERROR", ignoreCase = true)

        val headersToScan = if (canHeadersSupported) standardHeaders else standardHeaders.take(1)

        for ((index, def) in headersToScan.withIndex()) {
            val initialBlock = EcuBlock(
                id = "ECU_${def.txHeader}",
                name = def.name,
                description = def.description,
                status = EcuStatus.SCANNING,
                header = def.txHeader,
                txHeader = def.txHeader,
                rxHeader = def.rxHeader
            )
            onProgress(initialBlock, ecuBlocks.toList())

            try {
                if (canHeadersSupported) {
                    sendCmd("AT SH ${def.txHeader}", 300L)
                    delay(30)
                }

                // Check responsiveness via 0100
                val probeResp = sendCmd("0100", 1000L)
                val isResponding = isPositiveResponse(probeResp, "4100")

                if (!isResponding) {
                    // Try direct DTC request Mode 03 just in case 0100 is not supported on non-powertrain ECU
                    val dtcProbe = sendCmd("03", 1000L)
                    val isDtcResponding = isPositiveResponse(dtcProbe, "43")

                    if (!isDtcResponding) {
                        val unrespBlock = initialBlock.copy(
                            status = EcuStatus.UNRESPONSIVE,
                            description = "${def.description} (Блок не ответил по шине CAN)"
                        )
                        ecuBlocks.add(unrespBlock)
                        onProgress(unrespBlock, ecuBlocks.toList())
                        continue
                    }
                }

                // Query Mode 01 PID 01 (MIL & DTC count)
                val mode0101 = sendCmd("0101", 800L)
                val dtcCountFrom01 = parseDtcCountFrom0101(mode0101)

                // Query Mode 03 (Confirmed DTCs)
                val dtcResp03 = sendCmd("03", 1500L)
                val codes03 = parseDtcCodes(dtcResp03, "43", def.name, isPending = false)

                // Query Mode 07 (Pending DTCs)
                val dtcResp07 = sendCmd("07", 1200L)
                val codes07 = parseDtcCodes(dtcResp07, "47", def.name, isPending = true)

                // Query Mode 0A (Permanent DTCs)
                val dtcResp0A = sendCmd("0A", 1200L)
                val codes0A = parseDtcCodes(dtcResp0A, "4A", def.name, isPending = false)

                val uniqueCodesForEcu = (codes03 + codes07 + codes0A).distinctBy { it.code }
                allDtcs.addAll(uniqueCodesForEcu)

                val status = if (uniqueCodesForEcu.isNotEmpty()) EcuStatus.HAS_ERRORS else EcuStatus.OK
                val updatedBlock = initialBlock.copy(
                    status = status,
                    errorCount = uniqueCodesForEcu.size,
                    dtcCodes = uniqueCodesForEcu.map { it.code },
                    description = if (uniqueCodesForEcu.isNotEmpty()) {
                        "Обнаружено ошибок: ${uniqueCodesForEcu.size} (${uniqueCodesForEcu.joinToString { it.code }})"
                    } else {
                        "Блок активен (CAN ID: ${def.txHeader} / ${def.rxHeader}). Ошибок не обнаружено."
                    }
                )

                ecuBlocks.add(updatedBlock)
                onProgress(updatedBlock, ecuBlocks.toList())

            } catch (e: Exception) {
                Log.e(TAG, "Error scanning ECU ${def.txHeader}: ${e.message}")
                val errBlock = initialBlock.copy(status = EcuStatus.UNRESPONSIVE)
                ecuBlocks.add(errBlock)
                onProgress(errBlock, ecuBlocks.toList())
            }
        }

        // Restore default broadcast header
        try {
            sendCmd("AT SH 7DF", 300L)
        } catch (e: Exception) {
            // Ignore
        }

        return Pair(ecuBlocks, allDtcs)
    }

    private fun isPositiveResponse(raw: String, expectedHex: String): Boolean {
        if (raw.isBlank() || raw.contains("NO DATA", ignoreCase = true) ||
            raw.contains("NODATA", ignoreCase = true) || raw.contains("ERROR", ignoreCase = true) ||
            raw.contains("UNABLE", ignoreCase = true)
        ) {
            return false
        }
        val cleaned = raw.replace(" ", "").replace("\r", "").replace("\n", "").uppercase()
        return cleaned.contains(expectedHex.uppercase())
    }

    private fun parseDtcCountFrom0101(raw: String): Int {
        if (raw.isBlank() || raw.contains("NO DATA", ignoreCase = true)) return 0
        val cleaned = raw.replace(" ", "").replace("\r", "").replace("\n", "").uppercase()
        val idx = cleaned.indexOf("4101")
        if (idx != -1 && cleaned.length >= idx + 6) {
            return try {
                val aByte = cleaned.substring(idx + 4, idx + 6).toInt(16)
                aByte and 0x7F // lower 7 bits are DTC count
            } catch (e: Exception) {
                0
            }
        }
        return 0
    }

    private fun parseDtcCodes(raw: String, modePrefix: String, ecuName: String, isPending: Boolean): List<DtcError> {
        if (raw.isBlank() || raw.contains("NO DATA", ignoreCase = true) ||
            raw.contains("NODATA", ignoreCase = true) || raw.contains("ERROR", ignoreCase = true)
        ) {
            return emptyList()
        }

        val cleaned = raw.replace(" ", "").replace("\r", "").replace("\n", "").uppercase()
        val tokens = raw.split("[\\s\r\n]+".toRegex()).map { it.trim().uppercase() }
            .filter { it.matches("^[0-9A-F]{2}$".toRegex()) }

        val dtcList = mutableListOf<DtcError>()

        // Search for Mode prefix tokens
        var i = 0
        while (i < tokens.size) {
            if (tokens[i] == modePrefix) {
                // Next byte can be number of DTCs or direct DTC bytes
                var k = i + 1
                while (k + 1 < tokens.size) {
                    val b1 = tokens[k].toIntOrNull(16) ?: break
                    val b2 = tokens[k + 1].toIntOrNull(16) ?: break

                    if (b1 == 0 && b2 == 0) {
                        k += 2
                        continue
                    }

                    val code = decodeDtcBytes(b1, b2)
                    if (code != null && code.length == 5) {
                        val category = when (code.first()) {
                            'P' -> "Двигатель и Трансмиссия (Powertrain)"
                            'C' -> "Шасси и Тормоза (Chassis)"
                            'B' -> "Кузов и Салон (Body)"
                            'U' -> "Шина данных и Сеть (Network)"
                            else -> "Общая диагностика"
                        }
                        val desc = com.example.data.ai.DtcDatabase.findDtc(code)?.title
                            ?: "Код неисправности $code ($ecuName)"

                        dtcList.add(
                            DtcError(
                                code = code,
                                category = category,
                                ecuName = ecuName,
                                description = desc,
                                severity = if (isPending) DtcSeverity.MINOR else DtcSeverity.WARNING,
                                isPending = isPending
                            )
                        )
                    }
                    k += 2
                }
            }
            i++
        }

        return dtcList
    }

    private fun decodeDtcBytes(b1: Int, b2: Int): String? {
        val prefix = when ((b1 and 0xC0) shr 6) {
            0 -> "P"
            1 -> "C"
            2 -> "B"
            3 -> "U"
            else -> return null
        }
        val digit2 = ((b1 and 0x30) shr 4).toString()
        val digit3 = (b1 and 0x0F).toString(16).uppercase()
        val digit4 = ((b2 and 0xF0) shr 4).toString(16).uppercase()
        val digit5 = (b2 and 0x0F).toString(16).uppercase()

        return "$prefix$digit2$digit3$digit4$digit5"
    }

    fun getAllSupportedEcuBlocks(): List<EcuBlock> {
        return standardHeaders.map { def ->
            EcuBlock(
                id = "ECU_${def.txHeader}",
                name = def.name,
                description = def.description,
                status = EcuStatus.NOT_SCANNED,
                header = def.txHeader,
                txHeader = def.txHeader,
                rxHeader = def.rxHeader
            )
        }
    }
}
