package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.data.ChatMessage
import com.example.data.ChatSender
import com.example.ui.AiMonitoringState
import com.example.ui.MainViewModel
import com.example.ui.components.VoiceAssistantSettingsDialog
import androidx.compose.material.icons.filled.Settings
import com.example.ui.theme.AutomotiveBlue
import com.example.ui.theme.ClearRed
import com.example.ui.theme.HighDensityBorder
import com.example.ui.theme.HighDensitySurface
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiAssistantScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val isVoiceEnabled by viewModel.isVoiceEnabled.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isHandsFreeActive by viewModel.isHandsFreeActive.collectAsState()
    val handsFreeMode by viewModel.handsFreeMode.collectAsState()
    val wakePhrase by viewModel.wakePhrase.collectAsState()
    val readyResponse by viewModel.readyResponse.collectAsState()
    val lastDetectedPhrase by viewModel.lastDetectedPhrase.collectAsState()
    val aiMonitoringState by viewModel.aiMonitoringState.collectAsState()
    val lastMonitoringStats by viewModel.lastMonitoringStats.collectAsState()
    val sensors by viewModel.sensors.collectAsState()
    val handsFreeWorkMode by viewModel.handsFreeWorkMode.collectAsState()
    val micAudioLevel by viewModel.micAudioLevel.collectAsState()
    val animatedMicLevel by animateFloatAsState(targetValue = micAudioLevel.coerceIn(0f, 1f), label = "animatedMicLevel")

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showMonitoringDialog by remember { mutableStateOf(false) }
    var showVoiceSettingsDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val speechIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        if (!matches.isNullOrEmpty()) {
            val text = matches[0]
            if (text.isNotBlank()) {
                viewModel.askAiCustomQuery(text)
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Скажите ваш вопрос или команду ИИ-Автомеханику...")
                }
                speechIntentLauncher.launch(intent)
            } catch (e: Exception) {
                viewModel.startVoiceInput()
            }
        }
    }

    val silentAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Silent permission grant for background hands-free listening
    }

    val openGoogleVoiceAskDialog: () -> Unit = {
        val hasAudio = androidx.core.content.ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasAudio) {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Скажите ваш вопрос или команду ИИ-Автомеханику...")
                }
                speechIntentLauncher.launch(intent)
            } catch (e: Exception) {
                viewModel.startVoiceInput()
            }
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val handleMicClick = {
        if (isSpeaking) {
            viewModel.stopSpeaking()
        } else {
            openGoogleVoiceAskDialog()
        }
    }

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.lastIndex)
        }
    }

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // Active AI Telemetry Monitoring HUD Card
        AnimatedVisibility(
            visible = aiMonitoringState.isActive,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SoftBlueContainer),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, AutomotiveBlue)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = AutomotiveBlue,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ИИ МОНИТОРИНГ ДАТЧИКА",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AutomotiveBlue
                            )
                        }
                        Text(
                            text = "${aiMonitoringState.elapsedSeconds} / ${aiMonitoringState.targetDurationSeconds} сек",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = aiMonitoringState.sensorName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "PID: ${aiMonitoringState.sensorPid} • Точек собрано: ${aiMonitoringState.points.size}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "${"%.1f".format(aiMonitoringState.currentLiveValue)} ${aiMonitoringState.unit}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AutomotiveBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val progress = if (aiMonitoringState.targetDurationSeconds > 0) {
                        (aiMonitoringState.elapsedSeconds.toFloat() / aiMonitoringState.targetDurationSeconds.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = AutomotiveBlue,
                        trackColor = AutomotiveBlue.copy(alpha = 0.2f),
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.finishAiSensorMonitoring() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AutomotiveBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Анализировать сейчас", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.cancelAiSensorMonitoring() },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp), tint = ClearRed)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Отмена", fontSize = 11.sp, color = ClearRed)
                        }
                    }
                }
            }
        }

        // Monitoring Results Quick-Action Card
        AnimatedVisibility(
            visible = lastMonitoringStats != null && !aiMonitoringState.isActive,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val stats = lastMonitoringStats
            if (stats != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AutomotiveBlue.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = AutomotiveBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "РЕЗУЛЬТАТЫ ЗАМЕРА (${stats.durationSeconds} СЕК)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AutomotiveBlue
                                )
                            }
                            IconButton(
                                onClick = { viewModel.dismissLastMonitoringCard() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }

                        Text(
                            text = stats.sensorName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Мин: ${"%.1f".format(stats.min)} ${stats.unit}", fontSize = 12.sp, color = TextSecondary)
                            Text("Макс: ${"%.1f".format(stats.max)} ${stats.unit}", fontSize = 12.sp, color = TextSecondary)
                            Text("Среднее: ${"%.1f".format(stats.avg)} ${stats.unit}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AutomotiveBlue)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.navigateTo("log_graph_viewer") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AutomotiveBlue),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.ShowChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Показать график", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { viewModel.askAiToAnalyzeLastMonitoring() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp), tint = AutomotiveBlue)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Обсудить с ИИ", fontSize = 11.sp, color = AutomotiveBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Chat Message List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            chatMessages.forEachIndexed { index, msg ->
                val prevMsg = chatMessages.getOrNull(index - 1)
                val isNewDay = prevMsg == null || !isSameCalendarDay(prevMsg.timestamp, msg.timestamp)
                if (isNewDay) {
                    item(key = "day_header_${msg.timestamp}") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(HighDensitySurface, RoundedCornerShape(12.dp))
                                    .border(1.dp, HighDensityBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = formatDayHeader(msg.timestamp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
                item(key = msg.id) {
                    ChatMessageBubble(
                        msg = msg,
                        isSpeaking = isSpeaking,
                        onStopSpeaking = { viewModel.stopSpeaking() }
                    )
                }
            }

            if (isAiLoading) {
                item {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = AutomotiveBlue,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ИИ-механик выполняет диагностический анализ...",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Listening Indicator Banner
        if (isListening) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ClearRed.copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = ClearRed,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "🎙️ Слушаю вашу речь... Задайте вопрос голосом",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClearRed
                )
            }
        }

        // Input Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, HighDensityBorder)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                val isAssistantSpeaking = isSpeaking
                val isAssistantThinking = isAiLoading || (handsFreeMode == com.example.data.tts.HandsFreeVoiceController.HandsFreeMode.PROCESSING && !isSpeaking)
                val isAwaitingCommand = isHandsFreeActive && handsFreeMode == com.example.data.tts.HandsFreeVoiceController.HandsFreeMode.AWAITING_COMMAND
                val isAssistantListening = (isHandsFreeActive && (
                    handsFreeMode == com.example.data.tts.HandsFreeVoiceController.HandsFreeMode.LISTENING_CONTINUOUS ||
                    handsFreeMode == com.example.data.tts.HandsFreeVoiceController.HandsFreeMode.TESTING_WAKE_WORD
                )) || isListening

                val isMicGaugeActive = isAssistantListening || isAwaitingCommand

                // Voice Playback Animation Bar (interrupted on click)
                AnimatedVisibility(visible = isAssistantSpeaking) {
                    val infiniteTransition = rememberInfiniteTransition(label = "voicePlayingAnim")
                    val bar1 by infiniteTransition.animateFloat(
                        initialValue = 0.2f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
                        label = "b1"
                    )
                    val bar2 by infiniteTransition.animateFloat(
                        initialValue = 0.85f, targetValue = 0.15f,
                        animationSpec = infiniteRepeatable(tween(340, easing = LinearEasing), RepeatMode.Reverse),
                        label = "b2"
                    )
                    val bar3 by infiniteTransition.animateFloat(
                        initialValue = 0.35f, targetValue = 0.95f,
                        animationSpec = infiniteRepeatable(tween(510, easing = LinearEasing), RepeatMode.Reverse),
                        label = "b3"
                    )
                    val bar4 by infiniteTransition.animateFloat(
                        initialValue = 0.95f, targetValue = 0.2f,
                        animationSpec = infiniteRepeatable(tween(390, easing = LinearEasing), RepeatMode.Reverse),
                        label = "b4"
                    )
                    val bar5 by infiniteTransition.animateFloat(
                        initialValue = 0.15f, targetValue = 0.88f,
                        animationSpec = infiniteRepeatable(tween(470, easing = LinearEasing), RepeatMode.Reverse),
                        label = "b5"
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.stopSpeaking() },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AutomotiveBlue.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.height(18.dp)
                                ) {
                                    listOf(bar1, bar2, bar3, bar4, bar5).forEach { hFraction ->
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .fillMaxHeight(hFraction.coerceIn(0.15f, 1f))
                                                .background(StatusGreen, RoundedCornerShape(1.5.dp))
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Воспроизведение ответа...",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Нажмите, чтобы остановить звук",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(ClearRed.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Остановить звук",
                                    tint = ClearRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Live status bar like in hands-free settings
                AnimatedVisibility(visible = isMicGaugeActive) {
                    val animatedStatusLevel by animateFloatAsState(
                        targetValue = micAudioLevel.coerceIn(0f, 1f),
                        label = "aiScreenMicStatusBar"
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isAwaitingCommand) Icons.Default.Hearing else Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = if (isAwaitingCommand) Color(0xFFEC4899) else AutomotiveBlue,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isAwaitingCommand) "ИИ активирован по кодовому слову (Жду команду):" else "Уровень микрофона (Live VU Meter):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAwaitingCommand) Color(0xFFEC4899) else AutomotiveBlue
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.Black.copy(alpha = 0.08f))
                                .border(0.5.dp, (if (isAwaitingCommand) Color(0xFFEC4899) else AutomotiveBlue).copy(alpha = 0.2f), RoundedCornerShape(3.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedStatusLevel.coerceIn(0.02f, 1f))
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            if (isAwaitingCommand) {
                                                listOf(
                                                    Color(0xFFF472B6),
                                                    Color(0xFFEC4899),
                                                    Color(0xFFBE185D)
                                                )
                                            } else {
                                                listOf(
                                                    Color(0xFF3B82F6),
                                                    Color(0xFF22C55E),
                                                    if (animatedStatusLevel > 0.7f) Color(0xFFEF4444) else Color(0xFFEAB308)
                                                )
                                            }
                                        )
                                    )
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Voice Input Mic Button with 5 dynamic color states:
                    // Говорит -> Зеленая | Думает -> Желтая | Активирован по кодовому слову -> Розовая | Слушает эфир -> Красная | Отключено -> Синяя

                    val micTargetColor = when {
                        isAwaitingCommand -> Color(0xFFEC4899) // Розовая (Активирован по кодовому слову, готов выполнять команды)
                        isAssistantSpeaking -> Color(0xFF16A34A) // Зеленая (Говорит)
                        isAssistantThinking -> Color(0xFFD97706) // Желтая (Думает)
                        isAssistantListening -> Color(0xFFDC2626) // Красная (Слушает)
                        else -> AutomotiveBlue // Синяя (Отключена / В покое)
                    }

                    val micBgColor by animateColorAsState(
                        targetValue = micTargetColor,
                        label = "micColor"
                    )

                    val micIcon = when {
                        isAwaitingCommand -> Icons.Default.Hearing
                        isAssistantSpeaking -> Icons.Default.Stop
                        isAssistantThinking -> Icons.Default.AutoAwesome
                        else -> Icons.Default.Mic
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(62.dp)
                    ) {
                        // Dynamic pulsating audio halo when microphone is active
                        if (isMicGaugeActive) {
                            val pulseScale = 1f + (animatedMicLevel * 0.35f)
                            val haloColor = if (isAwaitingCommand) Color(0xFFEC4899) else Color(0xFFDC2626)
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .graphicsLayer {
                                        scaleX = pulseScale
                                        scaleY = pulseScale
                                        alpha = (0.25f + animatedMicLevel * 0.5f).coerceIn(0.2f, 0.85f)
                                    }
                                    .background(haloColor, CircleShape)
                            )
                        }

                        // Main Button
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(micBgColor)
                                .border(
                                    width = if (isMicGaugeActive) 2.5.dp else 2.dp,
                                    color = if (isMicGaugeActive) Color.White.copy(alpha = 0.95f) else SoftBlueContainer,
                                    shape = CircleShape
                                )
                                .clickable { handleMicClick() }
                                .testTag("btn_mic_ai_chat"),
                            contentAlignment = Alignment.Center
                        ) {
                            // Circular dynamic VU meter gauge around perimeter of the button
                            if (isMicGaugeActive) {
                                CircularProgressIndicator(
                                    progress = { animatedMicLevel.coerceIn(0.04f, 1f) },
                                    modifier = Modifier.fillMaxSize().padding(2.dp),
                                    color = if (isAwaitingCommand) Color(0xFFFBCFE8) else if (animatedMicLevel > 0.65f) Color(0xFFFDE047) else Color.White,
                                    strokeWidth = 3.dp,
                                    trackColor = if (isAwaitingCommand) Color(0xFF831843).copy(alpha = 0.35f) else Color(0xFF7F1D1D).copy(alpha = 0.35f)
                                )
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = micIcon,
                                    contentDescription = if (isAssistantSpeaking) "Остановить ответ" else "Голосовой ввод",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    if (isAssistantSpeaking) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = { viewModel.stopSpeaking() },
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ClearRed,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(44.dp).testTag("btn_stop_speech_chat")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Стоп", tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Стоп", fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Спросите что угодно или дайте команду...", fontSize = 13.sp, color = TextSecondary) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_ai_chat"),
                        shape = RoundedCornerShape(24.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 14.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = AutomotiveBlue,
                            unfocusedBorderColor = HighDensityBorder,
                            focusedPlaceholderColor = TextSecondary,
                            unfocusedPlaceholderColor = TextSecondary
                        ),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.stopSpeaking()
                                val prompt = textInput
                                textInput = ""
                                viewModel.askAiCustomQuery(prompt)
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(AutomotiveBlue, CircleShape)
                            .testTag("btn_send_ai_chat")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Отправить",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // Interactive Sensor Monitoring Setup Dialog
    if (showMonitoringDialog) {
        var selectedPid by remember { mutableStateOf("010C") }
        var selectedDuration by remember { mutableFloatStateOf(15f) }

        val targetSensors = listOf(
            Triple("010C", "Обороты двигателя (RPM)", "об/мин"),
            Triple("0105", "Температура ОЖ", "°C"),
            Triple("010D", "Скорость авто", "км/ч"),
            Triple("0142", "Напряжение АКБ", "В"),
            Triple("0111", "Положение дросселя", "%"),
            Triple("010B", "Давление во впуске (MAP)", "кПа"),
            Triple("0110", "Расход воздуха (MAF)", "г/с"),
            Triple("0106", "Кратковременная коррекция топлива", "%"),
            Triple("010F", "Температура впуска", "°C")
        )

        AlertDialog(
            onDismissRequest = { showMonitoringDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = AutomotiveBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Запуск ИИ-мониторинга", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Выберите датчик для наблюдения:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        targetSensors.forEach { (pid, name, unit) ->
                            val isSel = selectedPid == pid
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedPid = pid },
                                label = { Text(name, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AutomotiveBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Длительность замера:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${selectedDuration.toInt()} секунд", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AutomotiveBlue)
                    }

                    Slider(
                        value = selectedDuration,
                        onValueChange = { selectedDuration = it },
                        valueRange = 5f..60f,
                        steps = 10,
                        colors = SliderDefaults.colors(
                            thumbColor = AutomotiveBlue,
                            activeTrackColor = AutomotiveBlue
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMonitoringDialog = false
                        val match = targetSensors.find { it.first == selectedPid }
                        val name = match?.second ?: "Датчик"
                        val unit = match?.third ?: ""
                        viewModel.startAiSensorMonitoring(selectedPid, name, unit, selectedDuration.toInt())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AutomotiveBlue)
                ) {
                    Text("Начать замер")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showMonitoringDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showVoiceSettingsDialog) {
        VoiceAssistantSettingsDialog(
            isHandsFreeActive = isHandsFreeActive,
            currentWakePhrase = wakePhrase,
            currentReadyResponse = readyResponse,
            currentMode = handsFreeMode,
            lastDetectedPhrase = lastDetectedPhrase,
            audioLevel = micAudioLevel,
            currentWorkMode = handsFreeWorkMode,
            onSetWorkMode = { viewModel.setHandsFreeWorkMode(it) },
            onSaveSettings = { finalWake, finalResp, isEnabled ->
                viewModel.updateHandsFreeSettings(finalWake, finalResp, isEnabled)
            },
            onTestWakeWord = { testWake, testResp, callback ->
                viewModel.testWakeWord(testWake, testResp, callback)
            },
            onTestVoiceResponse = { testText ->
                viewModel.testResponseVoice(testText)
            },
            onDismiss = { showVoiceSettingsDialog = false }
        )
    }
}

@Composable
fun ChatMessageBubble(
    msg: ChatMessage,
    isSpeaking: Boolean = false,
    onStopSpeaking: (() -> Unit)? = null
) {
    val isUser = msg.sender == ChatSender.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .then(
                    if (!isUser && isSpeaking && onStopSpeaking != null) {
                        Modifier.clickable { onStopSpeaking() }
                    } else Modifier
                ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) SoftBlueContainer else HighDensitySurface
            ),
            border = if (isUser) null else androidx.compose.foundation.BorderStroke(1.dp, HighDensityBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "Вы" else "ИИ Авто Консультант",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) AutomotiveBlue else TextSecondary
                    )
                    Text(
                        text = formatMessageTime(msg.timestamp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary.copy(alpha = 0.85f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = msg.text,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
            }
        }
    }
}

private fun isSameCalendarDay(time1: Long, time2: Long): Boolean {
    val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = time1 }
    val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = time2 }
    return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
            cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
}

private fun formatDayHeader(timestampMs: Long): String {
    val now = java.util.Calendar.getInstance()
    val msgCal = java.util.Calendar.getInstance().apply { timeInMillis = timestampMs }

    val isSameDay = now.get(java.util.Calendar.YEAR) == msgCal.get(java.util.Calendar.YEAR) &&
            now.get(java.util.Calendar.DAY_OF_YEAR) == msgCal.get(java.util.Calendar.DAY_OF_YEAR)

    val isYesterday = now.get(java.util.Calendar.YEAR) == msgCal.get(java.util.Calendar.YEAR) &&
            now.get(java.util.Calendar.DAY_OF_YEAR) - msgCal.get(java.util.Calendar.DAY_OF_YEAR) == 1

    return when {
        isSameDay -> "Сегодня"
        isYesterday -> "Вчера"
        else -> {
            val sdf = java.text.SimpleDateFormat("d MMMM, EEEE", java.util.Locale("ru"))
            sdf.format(java.util.Date(timestampMs)).replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale("ru")) else it.toString() }
        }
    }
}

private fun formatMessageTime(timestampMs: Long): String {
    val now = java.util.Calendar.getInstance()
    val msgCal = java.util.Calendar.getInstance().apply { timeInMillis = timestampMs }

    val isSameDay = now.get(java.util.Calendar.YEAR) == msgCal.get(java.util.Calendar.YEAR) &&
            now.get(java.util.Calendar.DAY_OF_YEAR) == msgCal.get(java.util.Calendar.DAY_OF_YEAR)

    val isYesterday = now.get(java.util.Calendar.YEAR) == msgCal.get(java.util.Calendar.YEAR) &&
            now.get(java.util.Calendar.DAY_OF_YEAR) - msgCal.get(java.util.Calendar.DAY_OF_YEAR) == 1

    val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(timestampMs))

    return when {
        isSameDay -> "Сегодня, $timeStr"
        isYesterday -> "Вчера, $timeStr"
        else -> {
            val dayFormat = java.text.SimpleDateFormat("E, d MMM, HH:mm", java.util.Locale("ru"))
            dayFormat.format(java.util.Date(timestampMs)).replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale("ru")) else it.toString() }
        }
    }
}
