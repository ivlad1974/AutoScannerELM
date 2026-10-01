package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.DtcItemCard
import com.example.ui.components.EcuBlockCard
import com.example.ui.components.InlineScanningProcessWidget
import com.example.ui.theme.AutomotiveBlue
import com.example.ui.theme.ClearRed
import com.example.ui.theme.HighDensityBorder
import com.example.ui.theme.HighDensitySurface
import com.example.ui.theme.OnSoftRedText
import com.example.ui.theme.SoftRedContainer
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun Mode1DtcScanScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateAiChat: () -> Unit
) {
    val ecuBlocks by viewModel.ecuBlocks.collectAsState()
    val dtcErrors by viewModel.dtcErrors.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanningProgressText by viewModel.scanningProgressText.collectAsState()
    val scanningProgressFraction by viewModel.scanningProgressFraction.collectAsState()
    val isConnected = connectionState != com.example.data.ConnectionState.DISCONNECTED
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "ПОЛНОЕ СКАНИРОВАНИЕ БЛОКОВ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AutomotiveBlue
                        )
                        Text(
                            text = "Сканирование ошибок (DTC)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Standard Universal Voice Assistant Card ("Слушаю" design, auto-stops speech on tap)
        item {
            com.example.ui.components.UniversalVoiceAssistantCard(
                viewModel = viewModel,
                onNavigateAiChat = onNavigateAiChat
            )
        }

        // Connection Warning Banner if not connected
        if (!isConnected) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftRedContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ClearRed.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ClearRed, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "АДАПТЕР НЕ ПОДКЛЮЧЕН",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ClearRed
                            )
                            Text(
                                text = "Для поиска и считывания реальных кодов ошибок подключите адаптер ELM327 на главном экране.",
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Action controls
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, HighDensityBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isScanning) {
                        InlineScanningProcessWidget(
                            progressText = scanningProgressText.ifBlank { "Сканирование блоков управления..." },
                            progressFraction = scanningProgressFraction,
                            onStopScan = { viewModel.stopEcuScan() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Button(
                            onClick = { viewModel.performFullEcuScan() },
                            modifier = Modifier.fillMaxWidth().testTag("btn_start_full_scan"),
                            colors = ButtonDefaults.buttonColors(containerColor = AutomotiveBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Запустить опрос всех ECU на ошибки", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Reset / Clear DTC Button (OBD2 Mode 04)
                    Button(
                        onClick = { showClearConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth().testTag("btn_clear_dtc_inside_diag"),
                        colors = ButtonDefaults.buttonColors(containerColor = SoftRedContainer, contentColor = OnSoftRedText),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, tint = ClearRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Сбросить ошибки ЭБУ (Mode 04 / Clear)", fontWeight = FontWeight.Bold, color = ClearRed)
                    }

                    if (dtcErrors.isNotEmpty()) {
                        Button(
                            onClick = {
                                viewModel.askAiAboutAllDtcs()
                                onNavigateAiChat()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("btn_ask_ai_all"),
                            colors = ButtonDefaults.buttonColors(containerColor = AutomotiveBlue.copy(alpha = 0.12f), contentColor = AutomotiveBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AutomotiveBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Проконсультироваться с ИИ по ${dtcErrors.size} ошибкам", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ECU Blocks Status
        item {
            Text(
                text = "БЛОКИ УПРАВЛЕНИЯ В АВТОМОБИЛЕ (${ecuBlocks.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(ecuBlocks) { ecu ->
            EcuBlockCard(ecu = ecu)
        }

        // DTC Errors List
        if (dtcErrors.isNotEmpty()) {
            item {
                Text(
                    text = "НАЙДЕННЫЕ КОДЫ НЕИСПРАВНОСТЕЙ (${dtcErrors.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ClearRed,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            items(dtcErrors) { dtc ->
                DtcItemCard(
                    dtc = dtc,
                    onAskAi = { err ->
                        viewModel.askAiAboutSingleDtc(err.code, err.description)
                        onNavigateAiChat()
                    }
                )
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = ClearRed, modifier = Modifier.size(32.dp)) },
            title = { Text("Сброс кодов ошибок (Mode 04)", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Внимание: данная команда отправит запрос в ЭБУ на удаление кодов неисправностей и погашение лампы Check Engine.\n\n" +
                    "Убедитесь, что:\n" +
                    "1. Зажигание ВКЛЮЧЕНО (ON)\n" +
                    "2. Двигатель ЗАГЛУШЕН (OFF)\n\n" +
                    "Выполнить сброс памяти ошибок?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmDialog = false
                        viewModel.clearDtcErrors()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ClearRed, contentColor = Color.White)
                ) {
                    Text("Сбросить ошибки", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
