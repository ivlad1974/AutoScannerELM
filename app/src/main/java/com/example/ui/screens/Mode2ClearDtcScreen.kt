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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.ClearRed
import com.example.ui.theme.HighDensityBorder
import com.example.ui.theme.HighDensitySurface
import com.example.ui.theme.SoftRedContainer
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun Mode2ClearDtcScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val dtcErrors by viewModel.dtcErrors.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val isConnected = connectionState != com.example.data.ConnectionState.DISCONNECTED

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "СБРОС ОШИБОК (OBD2 MODE 04)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ClearRed
                    )
                    Text(
                        text = "Очистка памяти ECU",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }
            }
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
                        Icon(Icons.Default.Warning, contentDescription = null, tint = ClearRed, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "АДАПТЕР НЕ ПОДКЛЮЧЕН",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ClearRed
                            )
                            Text(
                                text = "Для сброса ошибок и погашения лампы Check Engine подключите адаптер ELM327 на главном экране.",
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Warning banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SoftRedContainer),
                border = androidx.compose.foundation.BorderStroke(1.dp, ClearRed.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = ClearRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ВАЖНОЕ ПРЕДУПРЕЖДЕНИЕ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ClearRed
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "1. Зажигание должно быть ВКЛЮЧЕНО (ON).\n" +
                                    "2. Двигатель должен быть ВЫКЛЮЧЕН (OFF).\n" +
                                    "3. Сброс ошибок погасит лампу Check Engine, но физическая неисправность вернет ошибку вновь.",
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Main Action Box
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, HighDensityBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = null,
                        tint = ClearRed,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Активных кодов ошибок: ${dtcErrors.size}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )

                    Text(
                        text = "Отправка команды 04 сбросит сохраненные ошибки во всех доступных блоках управления.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.clearDtcErrors() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_confirm_clear_dtc"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ClearRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "ПОДТВЕРДИТЬ И СБРОСИТЬ ОШИБКИ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    if (statusMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = statusMessage,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ClearRed
                        )
                    }
                }
            }
        }
    }
}
