package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DtcError
import com.example.data.DtcSeverity
import com.example.data.EcuBlock
import com.example.data.EcuStatus
import com.example.ui.theme.AutomotiveBlue
import com.example.ui.theme.ClearRed
import com.example.ui.theme.HighDensityBorder
import com.example.ui.theme.HighDensitySurface
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.SoftRedContainer
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun EcuBlockCard(
    ecu: EcuBlock,
    modifier: Modifier = Modifier
) {
    val (statusColor, statusText, icon) = when (ecu.status) {
        EcuStatus.OK -> Triple(StatusGreen, "ОК — Ошибок нет", Icons.Default.CheckCircle)
        EcuStatus.HAS_ERRORS -> Triple(ClearRed, "Ошибок: ${ecu.errorCount}", Icons.Default.Warning)
        EcuStatus.SCANNING -> Triple(AutomotiveBlue, "Сканирование...", Icons.Default.HourglassTop)
        EcuStatus.NOT_SCANNED -> Triple(Color.Gray, "Готов к проверке", Icons.Default.CheckCircle)
        EcuStatus.UNRESPONSIVE -> Triple(WarningAmber, "Нет ответа", Icons.Default.Error)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(HighDensitySurface, RoundedCornerShape(16.dp))
            .border(1.dp, if (ecu.status == EcuStatus.HAS_ERRORS) SoftRedContainer else HighDensityBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(statusColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ecu.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = ecu.description,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun DtcItemCard(
    dtc: DtcError,
    onAskAi: (DtcError) -> Unit,
    modifier: Modifier = Modifier
) {
    val (severityColor, severityLabel) = when (dtc.severity) {
        DtcSeverity.CRITICAL -> Pair(ClearRed, "КРИТИЧЕСКАЯ")
        DtcSeverity.WARNING -> Pair(WarningAmber, "ВНИМАНИЕ")
        DtcSeverity.MINOR -> Pair(AutomotiveBlue, "ИНФО")
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(HighDensitySurface, RoundedCornerShape(16.dp))
            .border(1.dp, HighDensityBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("dtc_item_${dtc.code}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(severityColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = dtc.code,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = severityColor
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dtc.category,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = dtc.ecuName,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .background(severityColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = severityLabel,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = severityColor
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = dtc.description,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )

        if (dtc.possibleCauses.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Вероятные причины: ${dtc.possibleCauses.take(2).joinToString(", ")}",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { onAskAi(dtc) },
            modifier = Modifier.fillMaxWidth().testTag("btn_ask_ai_${dtc.code}"),
            colors = ButtonDefaults.buttonColors(
                containerColor = AutomotiveBlue,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Консультация ИИ по ошибке ${dtc.code}",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}
