package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.tts.HandsFreeVoiceController
import com.example.ui.MainViewModel
import com.example.ui.theme.AutomotiveBlue
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.WarningAmber

/**
 * Universal Voice Assistant Card / Button following the standardized "Слушаю" design.
 * 
 * Handles all voice interactions seamlessly:
 * 1. Speaking (Говорит/Отвечает) -> Green color + Voice icon. Tap immediately stops speech (TTS).
 * 2. Thinking (Думает/Анализ) -> Amber/Yellow color + Sparkles icon.
 * 3. Awaiting Command (Ждет команду) -> Pink color + Hearing icon.
 * 4. Listening (Слушает микрофон) -> Red color + pulsating Mic icon.
 * 5. Idle (В покое) -> Automotive Blue + Mic icon. Tap initiates Google Voice ask / AI dialogue.
 */
@Composable
fun UniversalVoiceAssistantCard(
    viewModel: MainViewModel,
    onNavigateAiChat: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isHandsFreeActive by viewModel.isHandsFreeActive.collectAsState()
    val handsFreeMode by viewModel.handsFreeMode.collectAsState()
    val micAudioLevel by viewModel.micAudioLevel.collectAsState()

    val speechIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.askAiCustomQuery(spokenText)
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Скажите ваш вопрос или команду...")
                }
                speechIntentLauncher.launch(intent)
            } catch (e: Exception) {
                viewModel.startVoiceInput()
            }
        }
    }

    val launchVoiceInput: () -> Unit = {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPerm) {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Скажите ваш вопрос или команду...")
                }
                speechIntentLauncher.launch(intent)
            } catch (e: Exception) {
                viewModel.startVoiceInput()
            }
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val isAssistantSpeaking = isSpeaking
    val isAssistantThinking = isAiLoading || (handsFreeMode == HandsFreeVoiceController.HandsFreeMode.PROCESSING && !isSpeaking)
    val isAwaitingCommand = isHandsFreeActive && handsFreeMode == HandsFreeVoiceController.HandsFreeMode.AWAITING_COMMAND
    val isAssistantListening = (isHandsFreeActive && (
        handsFreeMode == HandsFreeVoiceController.HandsFreeMode.LISTENING_CONTINUOUS ||
        handsFreeMode == HandsFreeVoiceController.HandsFreeMode.TESTING_WAKE_WORD
    )) || isListening

    val targetButtonColor = when {
        isAwaitingCommand -> Color(0xFFEC4899)
        isAssistantSpeaking -> StatusGreen
        isAssistantThinking -> WarningAmber
        isAssistantListening -> Color(0xFFDC2626)
        else -> AutomotiveBlue
    }

    val animatedButtonColor by animateColorAsState(
        targetValue = targetButtonColor,
        label = "universalVoiceBtnColor"
    )

    val actionTitle = when {
        isAwaitingCommand -> "ЖДУ КОМАНДУ"
        isAssistantListening -> "СЛУШАЮ..."
        isAssistantSpeaking -> "ОТВЕЧАЮ..."
        isAssistantThinking -> "ДУМАЮ..."
        else -> "ГОЛОСОВОЙ ИИ"
    }

    val subtitle = when {
        isAwaitingCommand -> "ИИ активирован по кодовому слову"
        isAssistantListening -> "Говорите команду или вопрос в микрофон"
        isAssistantSpeaking -> "Нажмите, чтобы остановить голос"
        isAssistantThinking -> "Обработка автомобильных параметров..."
        else -> "Нажмите, чтобы задать вопрос голосом"
    }

    val leadingIcon = when {
        isAwaitingCommand -> Icons.Default.Hearing
        isAssistantSpeaking -> Icons.Default.RecordVoiceOver
        isAssistantThinking -> Icons.Default.AutoAwesome
        isAssistantListening -> Icons.Default.Mic
        else -> Icons.Default.Mic
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("universal_voice_assistant_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = animatedButtonColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        if (isAssistantSpeaking) {
                            viewModel.stopSpeaking()
                        } else {
                            launchVoiceInput()
                        }
                    }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = actionTitle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            if (isAssistantSpeaking) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.White.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = "ТАП ДЛЯ СТОП",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                if (onNavigateAiChat != null) {
                    IconButton(
                        onClick = onNavigateAiChat,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Открыть чат",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
