package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.ColorScheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.tts.HandsFreeVoiceController

/**
 * Modern In-Car Voice Assistant Settings Dialog.
 * Offers both Smart One-Touch (Zero noise) and Continuous Drive Co-Pilot modes.
 */
@Composable
fun VoiceAssistantSettingsDialog(
    isHandsFreeActive: Boolean,
    currentWakePhrase: String,
    currentReadyResponse: String,
    currentMode: HandsFreeVoiceController.HandsFreeMode,
    lastDetectedPhrase: String,
    audioLevel: Float = 0f,
    audioLevelProvider: (() -> Float)? = null,
    currentWorkMode: HandsFreeVoiceController.AssistantWorkMode = HandsFreeVoiceController.AssistantWorkMode.CONTINUOUS_DRIVE_SESSION,
    onSetWorkMode: (HandsFreeVoiceController.AssistantWorkMode) -> Unit = {},
    onSaveSettings: (wakePhrase: String, readyResponse: String, isEnabled: Boolean) -> Unit,
    onTestWakeWord: (wakePhrase: String, readyResponse: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onTestVoiceResponse: (text: String) -> Unit,
    onDismiss: () -> Unit
) {
    val resolvedAudioLevel: () -> Float = audioLevelProvider ?: { audioLevel }
    var isHandsFreeEnabled by remember { mutableStateOf(isHandsFreeActive) }
    var wakePhraseInput by rememberSaveable { mutableStateOf(currentWakePhrase) }
    var readyResponseInput by rememberSaveable { mutableStateOf(currentReadyResponse) }

    // Verification testing state
    var isTestingWakeWord by remember { mutableStateOf(false) }
    var testResultSuccess by remember { mutableStateOf<Boolean?>(null) }
    var testResultDetails by remember { mutableStateOf("") }

    val themeColors = MaterialTheme.colorScheme

    val commitCurrentSettings = {
        val finalWake = wakePhraseInput.trim().ifBlank { "Автоскан" }
        val finalResp = readyResponseInput.trim().ifBlank { "Слушаю вас" }
        val isEnabled = isHandsFreeEnabled
        onSetWorkMode(if (isEnabled) HandsFreeVoiceController.AssistantWorkMode.CONTINUOUS_DRIVE_SESSION else HandsFreeVoiceController.AssistantWorkMode.OFF)
        onSaveSettings(finalWake, finalResp, isEnabled)
    }

    AlertDialog(
        onDismissRequest = {
            commitCurrentSettings()
            onDismiss()
        },
        shape = RoundedCornerShape(18.dp),
        containerColor = themeColors.surface,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(themeColors.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = themeColors.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Голосовой помощник",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = themeColors.onSurface
                    )
                    Text(
                        text = "✓ Все настройки сохраняются автоматически",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF22C55E)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HorizontalDivider(color = themeColors.outline.copy(alpha = 0.20f))

                // Mode Button: Hands-Free Mode Toggle
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val newEnabled = !isHandsFreeEnabled
                            isHandsFreeEnabled = newEnabled
                            val workMode = if (newEnabled)
                                HandsFreeVoiceController.AssistantWorkMode.CONTINUOUS_DRIVE_SESSION
                            else
                                HandsFreeVoiceController.AssistantWorkMode.OFF
                            onSetWorkMode(workMode)
                            val finalWake = wakePhraseInput.trim().ifBlank { "Автоскан" }
                            val finalResp = readyResponseInput.trim().ifBlank { "Слушаю вас" }
                            onSaveSettings(finalWake, finalResp, newEnabled)
                        },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isHandsFreeEnabled) Color(0xFF16A34A).copy(alpha = 0.12f) else themeColors.surfaceVariant
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isHandsFreeEnabled) Color(0xFF16A34A) else themeColors.outline.copy(alpha = 0.25f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        if (isHandsFreeEnabled) Color(0xFF16A34A) else themeColors.outline.copy(alpha = 0.2f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isHandsFreeEnabled) Icons.Default.Hearing else Icons.Default.MicOff,
                                    contentDescription = null,
                                    tint = if (isHandsFreeEnabled) Color.White else themeColors.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Голосовой эфир (Hands-Free)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isHandsFreeEnabled) Color(0xFF16A34A) else themeColors.onSurface
                                    )
                                    if (isHandsFreeEnabled) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF16A34A), RoundedCornerShape(3.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text("АКТИВЕН", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                                Text(
                                    text = if (isHandsFreeEnabled) "Слушает эфир по кодовому слову «$wakePhraseInput»" else "Фоновый микрофон выключен",
                                    fontSize = 9.sp,
                                    color = themeColors.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isHandsFreeEnabled,
                            onCheckedChange = { checked ->
                                isHandsFreeEnabled = checked
                                val workMode = if (checked)
                                    HandsFreeVoiceController.AssistantWorkMode.CONTINUOUS_DRIVE_SESSION
                                else
                                    HandsFreeVoiceController.AssistantWorkMode.OFF
                                onSetWorkMode(workMode)
                                val finalWake = wakePhraseInput.trim().ifBlank { "Автоскан" }
                                val finalResp = readyResponseInput.trim().ifBlank { "Слушаю вас" }
                                onSaveSettings(finalWake, finalResp, checked)
                            },
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                if (isHandsFreeEnabled) {
                    RedMicrophoneActiveButton(
                        audioLevelProvider = resolvedAudioLevel,
                        onClick = {
                            isHandsFreeEnabled = false
                            onSetWorkMode(HandsFreeVoiceController.AssistantWorkMode.OFF)
                            val finalWake = wakePhraseInput.trim().ifBlank { "Автоскан" }
                            val finalResp = readyResponseInput.trim().ifBlank { "Слушаю вас" }
                            onSaveSettings(finalWake, finalResp, false)
                        }
                    )
                }

                // Live VU Meter Bar
                if (isHandsFreeEnabled || isTestingWakeWord) {
                    LiveVuMeterCard(
                        audioLevelProvider = resolvedAudioLevel,
                        themeColors = themeColors
                    )
                }

                // Field 1: Wake Phrase (Auto-saves on typing)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = themeColors.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Кодовое слово (фраза активации):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.onSurface
                        )
                    }
                    OutlinedTextField(
                        value = wakePhraseInput,
                        onValueChange = {
                            wakePhraseInput = it
                            val finalWake = it.trim().ifBlank { "Автоскан" }
                            val finalResp = readyResponseInput.trim().ifBlank { "Слушаю вас" }
                            onSaveSettings(finalWake, finalResp, isHandsFreeEnabled)
                        },
                        placeholder = {
                            Text(
                                text = "Например: Автоскан, Помощник, Эксперт",
                                fontSize = 12.sp,
                                color = themeColors.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Next
                        ),
                        textStyle = TextStyle(
                            color = themeColors.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = themeColors.onSurface,
                            unfocusedTextColor = themeColors.onSurface,
                            focusedContainerColor = themeColors.surfaceVariant,
                            unfocusedContainerColor = themeColors.surfaceVariant,
                            focusedBorderColor = themeColors.primary,
                            unfocusedBorderColor = themeColors.outline.copy(alpha = 0.5f),
                            cursorColor = themeColors.primary
                        )
                    )
                }

                // Field 2: Ready Response Phrases (Auto-saves on typing)
                val parsedVariants = remember(readyResponseInput) {
                    readyResponseInput.split(",", ";").map { it.trim() }.filter { it.isNotBlank() }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = themeColors.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Варианты ответа (через запятую):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.onSurface
                            )
                        }

                        // Compact Quick listen button
                        Button(
                            onClick = {
                                val chosen = if (parsedVariants.isNotEmpty()) parsedVariants.random() else "Слушаю вас"
                                onTestVoiceResponse(chosen)
                                testResultSuccess = true
                                testResultDetails = if (parsedVariants.size > 1) "Случайно из ${parsedVariants.size}: «$chosen»" else "Озвучено: «$chosen»"
                            },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeColors.primary.copy(alpha = 0.15f),
                                contentColor = themeColors.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Звук", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    OutlinedTextField(
                        value = readyResponseInput,
                        onValueChange = {
                            readyResponseInput = it
                            val finalWake = wakePhraseInput.trim().ifBlank { "Автоскан" }
                            val finalResp = it.trim().ifBlank { "Слушаю вас" }
                            onSaveSettings(finalWake, finalResp, isHandsFreeEnabled)
                        },
                        placeholder = {
                            Text(
                                text = "Например: ну что, слушаю, говори",
                                fontSize = 12.sp,
                                color = themeColors.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Done
                        ),
                        textStyle = TextStyle(
                            color = themeColors.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = themeColors.onSurface,
                            unfocusedTextColor = themeColors.onSurface,
                            focusedContainerColor = themeColors.surfaceVariant,
                            unfocusedContainerColor = themeColors.surfaceVariant,
                            focusedBorderColor = themeColors.primary,
                            unfocusedBorderColor = themeColors.outline.copy(alpha = 0.5f),
                            cursorColor = themeColors.primary
                        )
                    )

                    // Interactive chip previews
                    if (parsedVariants.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(top = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            parsedVariants.forEach { variant ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = themeColors.primary.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, themeColors.primary.copy(alpha = 0.35f)),
                                    modifier = Modifier.clickable {
                                        onTestVoiceResponse(variant)
                                        testResultSuccess = true
                                        testResultDetails = "Озвучен вариант: «$variant»"
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = null,
                                            tint = themeColors.primary,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "«$variant»",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = themeColors.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "💡 Введите варианты через запятую: ассистент будет выбирать их случайно.",
                            fontSize = 10.sp,
                            color = themeColors.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 2.dp, start = 2.dp)
                        )
                    }
                }

                // COMPACT VERIFICATION SECTION: Test Wake Word & Response
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = themeColors.surfaceVariant),
                    border = BorderStroke(1.dp, themeColors.outline.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Hearing,
                                contentDescription = null,
                                tint = themeColors.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ПРОВЕРКА РАСПОЗНАВАНИЯ В МИКРОФОН",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = themeColors.onSurface
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Button: Test Wake Word via Mic (Small)
                            Button(
                                onClick = {
                                    isTestingWakeWord = true
                                    testResultDetails = "Слушаю микрофон..."
                                    onTestWakeWord(
                                        wakePhraseInput.ifBlank { "Автоскан" },
                                        readyResponseInput.ifBlank { "Слушаю вас" }
                                    ) { matched, spoken ->
                                        isTestingWakeWord = false
                                        testResultSuccess = matched
                                        testResultDetails = spoken
                                    }
                                },
                                enabled = !isTestingWakeWord,
                                modifier = Modifier.weight(1.2f).height(34.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isTestingWakeWord) Color(0xFFDC2626) else themeColors.primary,
                                    disabledContainerColor = Color(0xFFDC2626),
                                    contentColor = Color.White,
                                    disabledContentColor = Color.White
                                )
                            ) {
                                if (isTestingWakeWord) {
                                    RedTestWakeWordButtonContent(audioLevelProvider = resolvedAudioLevel)
                                } else {
                                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Проверить слово", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Button: Instant TTS test (Small)
                            OutlinedButton(
                                onClick = {
                                    val chosen = if (parsedVariants.isNotEmpty()) parsedVariants.random() else "Слушаю вас"
                                    onTestVoiceResponse(chosen)
                                    testResultSuccess = true
                                    testResultDetails = if (parsedVariants.size > 1) "Случайно из ${parsedVariants.size}: «$chosen»" else "Озвучено: «$chosen»"
                                },
                                modifier = Modifier.height(34.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = themeColors.onSurface),
                                border = BorderStroke(1.dp, themeColors.outline.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Тест звука", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Test Result Banner
                        if (isTestingWakeWord || testResultSuccess != null) {
                            val bannerBg = when {
                                isTestingWakeWord -> themeColors.primary.copy(alpha = 0.15f)
                                testResultSuccess == true -> Color(0xFF22C55E).copy(alpha = 0.15f)
                                else -> Color(0xFFEF4444).copy(alpha = 0.15f)
                            }
                            val bannerBorder = when {
                                isTestingWakeWord -> themeColors.primary
                                testResultSuccess == true -> Color(0xFF22C55E)
                                else -> Color(0xFFEF4444)
                            }
                            val bannerIcon = when {
                                isTestingWakeWord -> Icons.Default.Hearing
                                testResultSuccess == true -> Icons.Default.CheckCircle
                                else -> Icons.Default.ErrorOutline
                            }
                            val bannerTitle = when {
                                isTestingWakeWord -> "🎙️ Говорите кодовое слово в микрофон"
                                testResultSuccess == true -> "✅ Распознано успешно!"
                                else -> "❌ Не распознано"
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(bannerBg, RoundedCornerShape(8.dp))
                                    .border(1.dp, bannerBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = bannerIcon,
                                        contentDescription = null,
                                        tint = bannerBorder,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = bannerTitle,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = bannerBorder
                                        )
                                        if (testResultDetails.isNotBlank()) {
                                            Text(
                                                text = "Результат: «$testResultDetails»",
                                                fontSize = 9.sp,
                                                color = themeColors.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Live Assistant Status Badge
                AnimatedVisibility(
                    visible = isHandsFreeEnabled,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val statusText = when (currentMode) {
                        HandsFreeVoiceController.HandsFreeMode.LISTENING_CONTINUOUS -> "🟢 Голосовой эфир: ожидание фразы активации..."
                        HandsFreeVoiceController.HandsFreeMode.AWAITING_COMMAND -> "🌸 Активирован! Прием вашей команды..."
                        HandsFreeVoiceController.HandsFreeMode.PROCESSING -> "🧠 Выполнение команды..."
                        HandsFreeVoiceController.HandsFreeMode.TESTING_WAKE_WORD -> "🧪 Тестирование кодового слова..."
                        HandsFreeVoiceController.HandsFreeMode.IDLE -> "Остановлен"
                    }
                    val statusColor = when (currentMode) {
                        HandsFreeVoiceController.HandsFreeMode.LISTENING_CONTINUOUS -> Color(0xFF22C55E)
                        HandsFreeVoiceController.HandsFreeMode.AWAITING_COMMAND -> Color(0xFFEC4899)
                        HandsFreeVoiceController.HandsFreeMode.PROCESSING -> themeColors.primary
                        HandsFreeVoiceController.HandsFreeMode.TESTING_WAKE_WORD -> themeColors.primary
                        HandsFreeVoiceController.HandsFreeMode.IDLE -> themeColors.onSurfaceVariant
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(statusColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = statusText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                                if (lastDetectedPhrase.isNotBlank()) {
                                    Text(
                                        text = "Последнее: «$lastDetectedPhrase»",
                                        fontSize = 9.sp,
                                        color = themeColors.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = null
    )
}

@Composable
fun RedMicrophoneActiveButton(
    audioLevelProvider: () -> Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonText: String = "ОТКЛЮЧИТЬ HANDS-FREE"
) {
    val rawLevel = audioLevelProvider().coerceIn(0f, 1f)
    val animatedLevel by animateFloatAsState(
        targetValue = rawLevel,
        label = "redBtnMicLevel"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFDC2626))
            .border(
                width = 1.dp,
                color = if (animatedLevel > 0.35f) Color(0xFFFDE047) else Color(0xFFEF4444).copy(alpha = 0.8f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Отключить",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = buttonText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Микрофон активен",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Микрофон активен",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // Embedded live horizontal VU meter status bar on the red button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedLevel.coerceIn(0.02f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF3B82F6),
                                    Color(0xFF22C55E),
                                    if (animatedLevel > 0.7f) Color(0xFFEF4444) else Color(0xFFEAB308)
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
fun RedTestWakeWordButtonContent(
    audioLevelProvider: () -> Float
) {
    val rawLevel = audioLevelProvider().coerceIn(0f, 1f)
    val animatedLevel by animateFloatAsState(
        targetValue = rawLevel,
        label = "testBtnLevel"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Микрофон активен",
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "Слушаю микрофон...",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Live status bar on test button
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(5.dp)
                .clip(RoundedCornerShape(2.5.dp))
                .background(Color.Black.copy(alpha = 0.35f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedLevel.coerceIn(0.02f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF3B82F6),
                                Color(0xFF22C55E),
                                if (animatedLevel > 0.7f) Color(0xFFEF4444) else Color(0xFFEAB308)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun MicrophoneEqualizerWave(
    audioLevel: Float,
    barCount: Int = 4,
    maxBarHeight: androidx.compose.ui.unit.Dp = 14.dp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val multipliers = listOf(0.6f, 1.0f, 0.75f, 0.9f)
        for (i in 0 until barCount) {
            val mult = multipliers.getOrElse(i) { 0.8f }
            val barFactor = (audioLevel * mult).coerceIn(0.18f, 1.0f)
            val barHeight = maxBarHeight * barFactor
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (audioLevel > 0.35f) Color(0xFFFDE047) else Color.White)
            )
        }
    }
}

@Composable
fun LiveVuMeterCard(
    audioLevelProvider: () -> Float,
    themeColors: ColorScheme,
    modifier: Modifier = Modifier
) {
    val rawLevel = audioLevelProvider().coerceIn(0f, 1f)
    val animatedAudioLevel by animateFloatAsState(
        targetValue = rawLevel,
        label = "vuMeter"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surfaceVariant),
        border = BorderStroke(1.dp, themeColors.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = themeColors.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Уровень микрофона (Live VU Meter):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.onSurface
                )
            }

            // Dynamic VU Meter Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(themeColors.surface)
                    .border(1.dp, themeColors.outline.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedAudioLevel.coerceIn(0.02f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF3B82F6),
                                    Color(0xFF22C55E),
                                    if (animatedAudioLevel > 0.7f) Color(0xFFEF4444) else Color(0xFFEAB308)
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
fun LiveVuMeterCard(
    audioLevel: Float,
    themeColors: ColorScheme,
    modifier: Modifier = Modifier
) {
    LiveVuMeterCard(
        audioLevelProvider = { audioLevel },
        themeColors = themeColors,
        modifier = modifier
    )
}
