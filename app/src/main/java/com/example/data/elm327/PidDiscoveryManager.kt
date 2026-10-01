package com.example.data.elm327

import android.util.Log
import com.example.data.ObdSensor
import com.example.data.SupportedPidsResult

object PidDiscoveryManager {

    private const val TAG = "PidDiscoveryManager"

    /**
     * Queries the vehicle via Mode 01 bitmap requests (0100, 0120, 0140, 0160, 0180, 01A0, 01C0, 01E0),
     * Mode 09 bitmap (0900), and Mode 06 bitmap (0600).
     */
    suspend fun discoverSupportedPids(
        sendCmd: suspend (String, Long) -> String
    ): SupportedPidsResult {
        val supportedPids = mutableSetOf<String>()
        val mode09Pids = mutableSetOf<String>()
        val mode06Tests = mutableSetOf<String>()

        try {
            // 1. Discover Mode 01 PIDs using 32-bit bitmaps
            var basePid = 0x00
            val maxBasePid = 0xE0

            while (basePid <= maxBasePid) {
                val hexReq = String.format("01%02X", basePid)
                val rawResp = sendCmd(hexReq, 1200L)
                val bytes = parse4ByteResponse(rawResp, hexReq)

                if (bytes == null || bytes.all { it == 0 }) {
                    Log.d(TAG, "No response or all zeros for $hexReq. Ending discovery at base 0x${basePid.toString(16)}")
                    break
                }

                // Decode 32 bits from 4 bytes (A, B, C, D)
                for (byteIdx in 0..3) {
                    val byteVal = bytes[byteIdx] and 0xFF
                    for (bitIdx in 7 downTo 0) {
                        val isSet = (byteVal and (1 shl bitIdx)) != 0
                        if (isSet) {
                            val pidNum = basePid + (byteIdx * 8) + (7 - bitIdx) + 1
                            val pidHex = String.format("01%02X", pidNum)
                            supportedPids.add(pidHex)
                        }
                    }
                }

                // Check if the 32nd PID (bit 0 of byte 3) is set. If set, next block is supported.
                val hasNextBlock = (bytes[3] and 0x01) != 0
                if (!hasNextBlock) {
                    Log.d(TAG, "Next block bit (0x${(basePid + 0x20).toString(16)}) not set. Stopping Mode 01 discovery.")
                    break
                }

                basePid += 0x20
            }

            // Fallback: If 0100 didn't return anything (e.g. old ISO9141/KWP vehicle), support standard baseline
            if (supportedPids.isEmpty()) {
                Log.w(TAG, "0100 bitmap discovery returned empty. Falling back to core standard OBD PIDs.")
                supportedPids.addAll(listOf("0104", "0105", "010B", "010C", "010D", "010E", "010F", "0110", "0111", "0114", "0115", "011F", "0142"))
            }

            // 2. Discover Mode 09 capabilities (0900)
            val raw0900 = sendCmd("0900", 1200L)
            val bytes09 = parse4ByteResponse(raw0900, "0900")
            if (bytes09 != null) {
                // Check common Mode 09 PIDs
                if ((bytes09[0] and 0x40) != 0) mode09Pids.add("0902") // VIN
                if ((bytes09[0] and 0x10) != 0) mode09Pids.add("0904") // CalID
                if ((bytes09[0] and 0x04) != 0) mode09Pids.add("0906") // CVN
                if ((bytes09[0] and 0x01) != 0) mode09Pids.add("0908") // IPT
                if ((bytes09[1] and 0x40) != 0) mode09Pids.add("090A") // ECU Name
            } else {
                mode09Pids.add("0902") // Always try VIN as standard
            }

            // 3. Discover Mode 06 capabilities (0600)
            val raw0600 = sendCmd("0600", 1200L)
            val bytes06 = parse4ByteResponse(raw0600, "0600")
            if (bytes06 != null) {
                for (byteIdx in 0..3) {
                    val byteVal = bytes06[byteIdx] and 0xFF
                    for (bitIdx in 7 downTo 0) {
                        if ((byteVal and (1 shl bitIdx)) != 0) {
                            val mid = (byteIdx * 8) + (7 - bitIdx) + 1
                            mode06Tests.add(String.format("%02X", mid))
                        }
                    }
                }
            } else {
                mode06Tests.addAll(listOf("01", "02", "21", "31", "32", "39", "A2", "A3", "A4", "A5", "B1"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in PID discovery: ${e.message}", e)
            supportedPids.addAll(listOf("0104", "0105", "010B", "010C", "010D", "010E", "010F", "0110", "0111", "0142"))
            mode09Pids.add("0902")
        }

        return SupportedPidsResult(
            supportedPids = supportedPids,
            totalSupportedCount = supportedPids.size,
            mode09SupportedPids = mode09Pids,
            mode06SupportedTests = mode06Tests,
            discoveredHeaders = listOf("7E0", "7E1", "7E2", "7E3")
        )
    }

    private fun parse4ByteResponse(raw: String, reqCmd: String): IntArray? {
        if (raw.isBlank() || raw.contains("NO DATA", ignoreCase = true) ||
            raw.contains("NODATA", ignoreCase = true) || raw.contains("ERROR", ignoreCase = true)
        ) {
            return null
        }

        val cleaned = raw.replace(" ", "").replace("\r", "").replace("\n", "").uppercase()
        val matchPrefix = "4" + reqCmd.substring(1) // e.g. "0100" -> "4100", "0900" -> "4900", "0600" -> "4600"

        val idx = cleaned.indexOf(matchPrefix)
        if (idx != -1 && cleaned.length >= idx + matchPrefix.length + 8) {
            return try {
                val start = idx + matchPrefix.length
                IntArray(4) { i ->
                    cleaned.substring(start + i * 2, start + (i + 1) * 2).toInt(16)
                }
            } catch (e: Exception) {
                null
            }
        }

        // Fallback token match
        val tokens = raw.split("[\\s\r\n]+".toRegex()).map { it.trim().uppercase() }
            .filter { it.matches("^[0-9A-F]{2}$".toRegex()) }

        val modeResp = "4" + reqCmd.substring(1, 2)
        val pidResp = reqCmd.substring(2, 4)

        if (tokens.size >= 6) {
            for (i in 0..tokens.size - 6) {
                if (tokens[i] == modeResp && tokens[i + 1] == pidResp) {
                    return try {
                        IntArray(4) { k -> tokens[i + 2 + k].toInt(16) }
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        }

        return null
    }

    /**
     * Marks which sensors in the catalog are supported according to the discovery result.
     */
    fun applySupportedPids(
        catalogSensors: List<ObdSensor>,
        supportedPids: Set<String>
    ): List<ObdSensor> {
        return catalogSensors.map { sensor ->
            val isSupp = if (sensor.isCustom) true else supportedPids.contains(sensor.pid.uppercase())
            sensor.copy(isSupported = isSupp)
        }
    }
}
