package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.example.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ConnectionState
import com.example.ui.MainViewModel
import com.example.ui.components.DashboardTilesGrid
import com.example.ui.components.FuelStrategySelectionDialog
import com.example.ui.components.InlineConnectingProcessWidget
import com.example.ui.components.InlineScanningProcessWidget
import com.example.ui.components.ObdProtocolSelectionDialog
import com.example.ui.components.SensorAlertsDialog
import com.example.ui.components.TachometerCard
import com.example.ui.components.ThermometerCard
import com.example.ui.components.VoiceAssistantSettingsDialog
import androidx.compose.material.icons.filled.QuestionAnswer
import com.example.ui.theme.AutomotiveBlue
import com.example.ui.theme.ClearRed
import com.example.ui.theme.HighDensityBorder
import com.example.ui.theme.HighDensitySurface
import com.example.ui.theme.HighDensitySurfaceVariant
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.OnSoftBlueText
import com.example.ui.theme.OnSoftRedText
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.SoftRedContainer
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@SuppressLint("MissingPermission")
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateMode1: () -> Unit,
    onNavigateMode2: () -> Unit,
    onNavigateMode3: () -> Unit,
    onNavigateAiChat: () -> Unit,
    onNavigateLogs: () -> Unit,
    onNavigateDesignShowcase: () -> Unit = {}
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val vehicleInfo by viewModel.vehicleInfo.collectAsState()
    val dtcErrors by viewModel.dtcErrors.collectAsState()
    val isVoiceEnabled by viewModel.isVoiceEnabled.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val sensors by viewModel.sensors.collectAsState()
    val sensorAlerts by viewModel.sensorAlerts.collectAsState()
    val aiAlert by viewModel.aiAlert.collectAsState()
    val currentTheme by viewModel.appTheme.collectAsState()
    val dashboardTileConfigs by viewModel.dashboardTileConfigs.collectAsState()
    val isHandsFreeActive by viewModel.isHandsFreeActive.collectAsState()
    val handsFreeMode by viewModel.handsFreeMode.collectAsState()
    val wakePhrase by viewModel.wakePhrase.collectAsState()
    val readyResponse by viewModel.readyResponse.collectAsState()
    val lastDetectedPhrase by viewModel.lastDetectedPhrase.collectAsState()
    val handsFreeAudioLevel by viewModel.handsFreeAudioLevel.collectAsState()
    val handsFreeWorkMode by viewModel.handsFreeWorkMode.collectAsState()
    val micAudioLevel by viewModel.micAudioLevel.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanningProgressText by viewModel.scanningProgressText.collectAsState()
    val scanningProgressFraction by viewModel.scanningProgressFraction.collectAsState()

    var showDeviceDialog by remember { mutableStateOf(false) }
    var isConnectionSettingsExpanded by remember { mutableStateOf(false) }
    var pairedDevicesList by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var showSensorAlertsDialog by remember { mutableStateOf(false) }
    var showVoiceSettingsDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showProtocolDialog by remember { mutableStateOf(false) }
    var showFuelStrategyDialog by remember { mutableStateOf(false) }
    val selectedProtocolCode by viewModel.selectedProtocol.collectAsState()
    val selectedFuelStratId by viewModel.selectedFuelStrategyId.collectAsState()
    val disconnectedReconnectSec by viewModel.disconnectedReconnectIntervalSec.collectAsState()
    val ignitionOffPollSec by viewModel.ignitionOffPollIntervalSec.collectAsState()
    val ignitionOnCheckSec by viewModel.ignitionOnCheckIntervalSec.collectAsState()
    var showProfileImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var importErrorMessage by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // SWIPE BACK TO MINIMIZE (Requirement: Swipe left gesture minimizes app, only exit button closes process)
    BackHandler {
        (context as? android.app.Activity)?.moveTaskToBack(true)
    }

    val speechIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val matches = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
        if (!matches.isNullOrEmpty()) {
            val text = matches[0]
            if (text.isNotBlank()) {
                viewModel.askAiCustomQuery(text)
            }
        }
    }

    val audioPermissionForManualAskLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                    putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Скажите вашу команду ИИ-Автомеханику...")
                }
                speechIntentLauncher.launch(intent)
            } catch (e: Exception) {
                viewModel.startVoiceInput()
            }
        }
    }

    // Silent audio permission launcher: only grants RECORD_AUDIO permission without opening Google modal dialog
    val silentAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        // No modal Google speech dialog triggered!
    }

    val openGoogleVoiceAskDialog: () -> Unit = {
        val hasAudio = androidx.core.content.ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasAudio) {
            try {
                val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                    putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Скажите вашу команду ИИ-Автомеханику...")
                }
                speechIntentLauncher.launch(intent)
            } catch (e: Exception) {
                viewModel.startVoiceInput()
            }
        } else {
            audioPermissionForManualAskLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val enableBtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (viewModel.isBluetoothEnabled()) {
            viewModel.autoConnectIfSavedDeviceExists()
        }
    }

    val openDevicePicker = {
        viewModel.enableBluetooth()
        pairedDevicesList = viewModel.getPairedDevices()
        showDeviceDialog = true
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.enableBluetooth()
        if (!viewModel.isBluetoothEnabled()) {
            try {
                enableBtLauncher.launch(Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE))
            } catch (e: Exception) {
                // Ignore
            }
        }
        viewModel.connectAutoOrPick(onNeedPicker = openDevicePicker)
    }

    val startupPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.enableBluetooth()
        if (!viewModel.isBluetoothEnabled()) {
            try {
                enableBtLauncher.launch(Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE))
            } catch (e: Exception) {
                // Ignore
            }
        } else {
            viewModel.autoConnectIfSavedDeviceExists()
        }
    }

    // Auto-enable Bluetooth and auto-connect to previously connected device on app start
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasBtConnect = androidx.core.content.ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasBtScan = androidx.core.content.ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_SCAN
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!hasBtConnect || !hasBtScan) {
                startupPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                    )
                )
            } else {
                viewModel.enableBluetooth()
                if (!viewModel.isBluetoothEnabled()) {
                    try {
                        enableBtLauncher.launch(Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    } catch (e: Exception) {
                        // Ignore
                    }
                } else {
                    viewModel.autoConnectIfSavedDeviceExists()
                }
            }
        } else {
            viewModel.enableBluetooth()
            if (!viewModel.isBluetoothEnabled()) {
                try {
                    enableBtLauncher.launch(Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE))
                } catch (e: Exception) {
                    // Ignore
                }
            } else {
                viewModel.autoConnectIfSavedDeviceExists()
            }
        }
    }

    val handleConnectClick = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            )
        } else {
            viewModel.connectAutoOrPick(onNeedPicker = openDevicePicker)
        }
    }

    val createProfileFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val ok = viewModel.exportProfileToUri(context, uri)
            if (ok) {
                Toast.makeText(context, "💾 Профиль сохранен в выбранный файл на диске!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Ошибка сохранения файла профиля", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val openProfileFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val ok = viewModel.importProfileFromUri(context, uri)
            if (ok) {
                Toast.makeText(context, "✅ Профиль (настройки, алармы и чат) успешно загружен!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Ошибка чтения файла профиля", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val rpm = if (connectionState == ConnectionState.DISCONNECTED) 0.0 else (sensors.find { it.pid == "010C" }?.value ?: 0.0)
    val coolant = if (connectionState == ConnectionState.DISCONNECTED) 0.0 else (sensors.find { it.pid == "0105" }?.value ?: 0.0)
    val speed = if (connectionState == ConnectionState.DISCONNECTED) 0.0 else (sensors.find { it.pid == "010D" }?.value ?: 0.0)
    val battery = if (connectionState == ConnectionState.DISCONNECTED) 0.0 else (sensors.find { it.pid == "0142" || it.name.contains("Напряжение", ignoreCase = true) }?.value ?: 14.1)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // High Density Action Header Bar (Buttons distributed across full width)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val activeAlertsCount = sensorAlerts.count { it.isEnabled }

                // 1. Sensor Alerts Button
                Surface(
                    onClick = { showSensorAlertsDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("top_bar_alerts_btn"),
                    shape = RoundedCornerShape(14.dp),
                    color = if (activeAlertsCount > 0) AutomotiveBlue.copy(alpha = 0.15f) else HighDensitySurface,
                    border = BorderStroke(
                        1.dp,
                        if (activeAlertsCount > 0) AutomotiveBlue else HighDensityBorder
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (activeAlertsCount > 0) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                            contentDescription = "Алармы датчиков",
                            tint = if (activeAlertsCount > 0) AutomotiveBlue else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // 2. Charts & Logs Button
                Surface(
                    onClick = { onNavigateLogs() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("top_bar_logs_btn"),
                    shape = RoundedCornerShape(14.dp),
                    color = HighDensitySurface,
                    border = BorderStroke(1.dp, HighDensityBorder)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "Журнал и графики",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // 3. Voice Feedback Button
                Surface(
                    onClick = { viewModel.toggleVoiceFeedback() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("top_bar_voice_btn"),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isVoiceEnabled) AutomotiveBlue.copy(alpha = 0.15f) else HighDensitySurface,
                    border = BorderStroke(
                        1.dp,
                        if (isVoiceEnabled) AutomotiveBlue else HighDensityBorder
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isVoiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Голосовое оповещение",
                            tint = if (isVoiceEnabled) AutomotiveBlue else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // 4. Exit Button
                Surface(
                    onClick = { showExitDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("top_bar_exit_btn"),
                    shape = RoundedCornerShape(14.dp),
                    color = ClearRed.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, ClearRed.copy(alpha = 0.35f))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Выход из программы",
                            tint = ClearRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // TOP ELM327 CONNECTION WIDGET (Single connect/status button + Expandable settings box)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (connectionState != ConnectionState.DISCONNECTED) StatusGreen.copy(alpha = 0.6f) else HighDensityBorder
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Main Connection / Status Button or Inline Process Widget (In place of button)
                        when {
                            connectionState == ConnectionState.CONNECTING -> {
                                InlineConnectingProcessWidget(
                                    statusText = statusMessage.ifBlank { "Подключение к адаптеру ELM327..." },
                                    onCancel = { viewModel.disconnect() },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            isScanning -> {
                                InlineScanningProcessWidget(
                                    progressText = scanningProgressText.ifBlank { "Сканирование блоков управления..." },
                                    progressFraction = scanningProgressFraction,
                                    onStopScan = { viewModel.stopEcuScan() },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            connectionState == ConnectionState.DISCONNECTED -> {
                                Button(
                                    onClick = handleConnectClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("btn_connect_bt"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AutomotiveBlue,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bluetooth,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "ПОДКЛЮЧИТЬ ELM327",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            else -> {
                                val isIgnitionOn = vehicleInfo.isIgnitionOn
                                val statusColor = if (isIgnitionOn) StatusGreen else Color(0xFFFFB300)
                                val statusText = if (isIgnitionOn) "🟢 ELM327: ЗАЖИГАНИЕ ВКЛ (ОТКЛЮЧИТЬ)" else "🟡 ELM327: ЗАЖИГАНИЕ ВЫКЛ (ОТКЛЮЧИТЬ)"

                                Button(
                                    onClick = { viewModel.disconnect() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("btn_connected_status"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = statusColor.copy(alpha = 0.15f),
                                        contentColor = statusColor
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(statusColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = statusText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Settings Accordion Expand Toggle
                        IconButton(
                            onClick = { isConnectionSettingsExpanded = !isConnectionSettingsExpanded },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (isConnectionSettingsExpanded) AutomotiveBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isConnectionSettingsExpanded) AutomotiveBlue else HighDensityBorder,
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(
                                imageVector = if (isConnectionSettingsExpanded) Icons.Default.ExpandLess else Icons.Default.Tune,
                                contentDescription = "Настройки подключения",
                                tint = if (isConnectionSettingsExpanded) AutomotiveBlue else TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Collapsible Settings Box
                    AnimatedVisibility(
                        visible = isConnectionSettingsExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HorizontalDivider(color = HighDensityBorder, thickness = 1.dp)

                            // Vehicle profile info
                            val rawName = if (connectionState == ConnectionState.DISCONNECTED) "АВТОМОБИЛЬ (ОТКЛЮЧЕНО)" else "${vehicleInfo.make} ${vehicleInfo.model}".trim()
                            val cleanName = if (rawName.contains("CONNECTED", ignoreCase = true)) {
                                rawName.replace("CONNECTED", "", ignoreCase = true).trim()
                            } else rawName

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ПРОФИЛЬ АВТО:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                                Text(
                                    text = if (cleanName.isBlank()) "АВТОМОБИЛЬ" else cleanName.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                            }

                            // Protocol Selection Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text("ПРОТОКОЛ OBD2 (AT SP)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AutomotiveBlue)
                                    val protoDisplayName = viewModel.availableProtocols.find { it.code == selectedProtocolCode }?.name
                                        ?: vehicleInfo.protocol.ifBlank { "Протокол $selectedProtocolCode" }
                                    Text(
                                        text = protoDisplayName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                OutlinedButton(
                                    onClick = { showProtocolDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Протокол", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Timing and Polling Intervals Settings Section
                            HorizontalDivider(color = HighDensityBorder.copy(alpha = 0.6f), thickness = 1.dp)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(HighDensitySurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .border(1.dp, HighDensityBorder, RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = AutomotiveBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ИНТЕРВАЛЫ ПРОВЕРКИ И ПЕРЕПОДКЛЮЧЕНИЯ",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = AutomotiveBlue
                                    )
                                }

                                // 1. Ignition OFF poll interval
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "При выключенном зажигании",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "Опрос датчика зажигания и АКБ",
                                                fontSize = 9.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Surface(
                                                onClick = { viewModel.setIgnitionOffPollIntervalSec(ignitionOffPollSec - 1) },
                                                shape = RoundedCornerShape(6.dp),
                                                color = HighDensitySurface,
                                                border = BorderStroke(1.dp, HighDensityBorder),
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("-", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                }
                                            }
                                            Text(
                                                text = "${ignitionOffPollSec}с",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = AutomotiveBlue,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )
                                            Surface(
                                                onClick = { viewModel.setIgnitionOffPollIntervalSec(ignitionOffPollSec + 1) },
                                                shape = RoundedCornerShape(6.dp),
                                                color = HighDensitySurface,
                                                border = BorderStroke(1.dp, HighDensityBorder),
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                }
                                            }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf(1, 2, 3, 5, 10).forEach { s ->
                                            val isSel = ignitionOffPollSec == s
                                            Surface(
                                                onClick = { viewModel.setIgnitionOffPollIntervalSec(s) },
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isSel) AutomotiveBlue.copy(alpha = 0.25f) else HighDensitySurface,
                                                border = BorderStroke(1.dp, if (isSel) AutomotiveBlue else HighDensityBorder),
                                                modifier = Modifier.weight(1f).height(24.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "${s}с",
                                                        fontSize = 10.sp,
                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSel) AutomotiveBlue else TextSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = HighDensityBorder.copy(alpha = 0.4f), thickness = 0.5.dp)

                                // 2. Ignition ON check interval
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "При включенном зажигании",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "Период фонового контроля зажигания",
                                                fontSize = 9.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Surface(
                                                onClick = { viewModel.setIgnitionOnCheckIntervalSec(ignitionOnCheckSec - 1) },
                                                shape = RoundedCornerShape(6.dp),
                                                color = HighDensitySurface,
                                                border = BorderStroke(1.dp, HighDensityBorder),
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("-", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                }
                                            }
                                            Text(
                                                text = "${ignitionOnCheckSec}с",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = StatusGreen,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )
                                            Surface(
                                                onClick = { viewModel.setIgnitionOnCheckIntervalSec(ignitionOnCheckSec + 1) },
                                                shape = RoundedCornerShape(6.dp),
                                                color = HighDensitySurface,
                                                border = BorderStroke(1.dp, HighDensityBorder),
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                }
                                            }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf(2, 3, 5, 10, 15).forEach { s ->
                                            val isSel = ignitionOnCheckSec == s
                                            Surface(
                                                onClick = { viewModel.setIgnitionOnCheckIntervalSec(s) },
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isSel) StatusGreen.copy(alpha = 0.25f) else HighDensitySurface,
                                                border = BorderStroke(1.dp, if (isSel) StatusGreen else HighDensityBorder),
                                                modifier = Modifier.weight(1f).height(24.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "${s}с",
                                                        fontSize = 10.sp,
                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSel) StatusGreen else TextSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = HighDensityBorder.copy(alpha = 0.4f), thickness = 0.5.dp)

                                // 3. Disconnected Reconnect interval
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "При обрыве связи (автоповтор)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "Интервал повторных попыток подключения",
                                                fontSize = 9.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Surface(
                                                onClick = { viewModel.setDisconnectedReconnectIntervalSec(disconnectedReconnectSec - 1) },
                                                shape = RoundedCornerShape(6.dp),
                                                color = HighDensitySurface,
                                                border = BorderStroke(1.dp, HighDensityBorder),
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("-", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                }
                                            }
                                            Text(
                                                text = "${disconnectedReconnectSec}с",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = AutomotiveBlue,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )
                                            Surface(
                                                onClick = { viewModel.setDisconnectedReconnectIntervalSec(disconnectedReconnectSec + 1) },
                                                shape = RoundedCornerShape(6.dp),
                                                color = HighDensitySurface,
                                                border = BorderStroke(1.dp, HighDensityBorder),
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                }
                                            }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf(1, 3, 5, 10, 30).forEach { s ->
                                            val isSel = disconnectedReconnectSec == s
                                            Surface(
                                                onClick = { viewModel.setDisconnectedReconnectIntervalSec(s) },
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isSel) AutomotiveBlue.copy(alpha = 0.25f) else HighDensitySurface,
                                                border = BorderStroke(1.dp, if (isSel) AutomotiveBlue else HighDensityBorder),
                                                modifier = Modifier.weight(1f).height(24.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "${s}с",
                                                        fontSize = 10.sp,
                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSel) AutomotiveBlue else TextSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Adapter Selection & Backup/Restore Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = openDevicePicker,
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AutomotiveBlue, contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text("Сменить адаптер", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { createProfileFileLauncher.launch("obd2_profile_backup.json") },
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text("💾 Сохранить", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { openProfileFileLauncher.launch(arrayOf("application/json", "*/*")) },
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text("📂 Загрузить", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // VARIANT 1: 4 LARGE DASHBOARD TELEMETRY TILES
        item {
            DashboardTilesGrid(
                tileConfigs = dashboardTileConfigs,
                sensors = sensors,
                isConnected = connectionState != ConnectionState.DISCONNECTED,
                onUpdateConfig = { slotIndex, newPid, newStyle, newTimeRange ->
                    viewModel.updateDashboardTileConfig(slotIndex, newPid, newStyle, newTimeRange)
                }
            )
        }

        // VARIANT 1: 2 HUGE PRIMARY ACTION BUTTONS (DIAGNOSTICS & AI MECHANIC)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Button 1: ECU Diagnostics
                val hasErrors = dtcErrors.isNotEmpty() && connectionState != ConnectionState.DISCONNECTED
                val diagBgColor = if (hasErrors) SoftRedContainer else SoftBlueContainer
                val diagTextColor = if (hasErrors) OnSoftRedText else OnSoftBlueText
                val diagIconBg = if (hasErrors) ClearRed else AutomotiveBlue

                Button(
                    onClick = { onNavigateMode1() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(78.dp)
                        .testTag("btn_mode_1"),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = diagBgColor,
                        contentColor = diagTextColor
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(diagIconBg, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BugReport,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "ДИАГНОСТИКА ЭБУ",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                                val errorSubtitle = when {
                                    connectionState == ConnectionState.DISCONNECTED -> "Сканировать коды ошибок (DTC)"
                                    dtcErrors.isEmpty() -> "🟢 0 ошибок (Системы в норме)"
                                    else -> "🔴 Найдено ошибок: ${dtcErrors.size} (Внимание!)"
                                }
                                Text(
                                    text = errorSubtitle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasErrors) ClearRed else diagTextColor.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Text("›", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = diagTextColor)
                    }
                }

                // Card 2: AI Mechanic (Voice & Chat & Hands-Free) with 5 dynamic color states:
                // Говорит -> Зеленая | Думает -> Желтая | Активирован по кодовому слову (слушает команду) -> Розовая | Слушает эфир -> Красная | Отключено -> Синяя
                val isAssistantSpeaking = isSpeaking
                val isAssistantThinking = isAiLoading || (handsFreeMode == com.example.data.tts.HandsFreeVoiceController.HandsFreeMode.PROCESSING && !isSpeaking)
                val isAwaitingCommand = isHandsFreeActive && handsFreeMode == com.example.data.tts.HandsFreeVoiceController.HandsFreeMode.AWAITING_COMMAND
                val isAssistantListening = (isHandsFreeActive && (
                    handsFreeMode == com.example.data.tts.HandsFreeVoiceController.HandsFreeMode.LISTENING_CONTINUOUS ||
                    handsFreeMode == com.example.data.tts.HandsFreeVoiceController.HandsFreeMode.TESTING_WAKE_WORD
                )) || isListening

                val aiButtonTargetColor = when {
                    isAwaitingCommand -> Color(0xFFEC4899) // Розовая (Активирован по кодовому слову, готов выполнять команды)
                    isAssistantSpeaking -> Color(0xFF16A34A) // Зеленая (Говорит)
                    isAssistantThinking -> Color(0xFFD97706) // Желтая/Янтарная (Думает)
                    isAssistantListening -> Color(0xFFDC2626) // Красная (Слушает эфир)
                    else -> AutomotiveBlue // Синяя (Отключена / В покое)
                }

                val animatedAiButtonColor by animateColorAsState(
                    targetValue = aiButtonTargetColor,
                    label = "aiButtonColorAnimation"
                )

                // Large Main Title for the AI Card replacing generic "ИИ АВТОМЕХАНИК":
                val aiMainActionTitle = when {
                    isAwaitingCommand -> "ЖДУ КОМАНДУ"
                    isAssistantListening -> "СЛУШАЮ"
                    isAssistantSpeaking -> "ОТВЕЧАЮ..."
                    isAssistantThinking -> "ДУМАЮ..."
                    else -> "СПРОСИТЬ"
                }

                val aiLeadingIcon = when {
                    isAwaitingCommand -> Icons.Default.Hearing
                    isAssistantSpeaking -> Icons.Default.RecordVoiceOver
                    isAssistantThinking -> Icons.Default.AutoAwesome
                    isAssistantListening -> Icons.Default.Hearing
                    else -> Icons.Default.Mic
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_ai_chat"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = animatedAiButtonColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Main AI info header (Click to ask by voice or stop speech if active)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (isAssistantSpeaking) {
                                        viewModel.stopSpeaking()
                                    } else {
                                        openGoogleVoiceAskDialog()
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color.White.copy(alpha = 0.24f), CircleShape)
                                        .border(1.2.dp, Color.White.copy(alpha = 0.45f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = aiLeadingIcon,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = aiMainActionTitle,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.8.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Right Action/Badge (Shown only if speaking to allow quick stop)
                            if (isAssistantSpeaking) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFDC2626))
                                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .clickable { viewModel.stopSpeaking() }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Stop,
                                            contentDescription = "Остановить",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "ОСТАНОВИТЬ",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Live status bar for microphone activity
                        if (isAssistantListening || isAwaitingCommand || isHandsFreeActive) {
                            val animatedMicLevel by animateFloatAsState(
                                targetValue = micAudioLevel.coerceIn(0f, 1f),
                                label = "homeMicStatusBar"
                            )
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = when {
                                            isAwaitingCommand -> "ИИ активирован по кодовому слову (Жду команду):"
                                            isAssistantListening -> "Уровень микрофона (активная речь):"
                                            else -> "Уровень микрофона (ожидание фразы):"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(3.5.dp))
                                        .background(Color.Black.copy(alpha = 0.35f))
                                        .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(3.5.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(animatedMicLevel.coerceIn(0.02f, 1f))
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(3.5.dp))
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
                                                            if (animatedMicLevel > 0.7f) Color(0xFFEF4444) else Color(0xFFEAB308)
                                                        )
                                                    }
                                                )
                                            )
                                    )
                                }
                            }
                        }

                        // Action buttons row (Chat, Hands-free Quick Toggle & Settings)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Chat Button
                            Button(
                                onClick = { onNavigateAiChat() },
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.22f),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.QuestionAnswer, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Чат с механиком", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // 2. Hands-Free Quick Toggle & Settings Button
                            Row(
                                modifier = Modifier.weight(1.2f).height(42.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        if (isHandsFreeActive) {
                                            viewModel.toggleHandsFree()
                                            Toast.makeText(context, "Режим Hands-Free отключен", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val hasAudio = androidx.core.content.ContextCompat.checkSelfPermission(
                                                context, Manifest.permission.RECORD_AUDIO
                                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                            if (!hasAudio) {
                                                try {
                                                    silentAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                } catch (e: Exception) {}
                                            }
                                            viewModel.toggleHandsFree()
                                            Toast.makeText(context, "Режим Hands-Free включен", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isHandsFreeActive) {
                                            if (isAwaitingCommand) Color(0xFFEC4899) else Color(0xFFDC2626)
                                        } else {
                                            Color.White.copy(alpha = 0.22f)
                                        },
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    if (isHandsFreeActive) {
                                        Icon(
                                            imageVector = Icons.Default.Hearing,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "ВЫКЛ",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Без рук",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        val hasAudio = androidx.core.content.ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.RECORD_AUDIO
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                        if (!hasAudio) {
                                            try {
                                                silentAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            } catch (e: Exception) {}
                                        }
                                        showVoiceSettingsDialog = true
                                    },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
                                ) {
                                    Icon(Icons.Default.Tune, contentDescription = "Настройки голоса", tint = Color.White, modifier = Modifier.size(17.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // SECONDARY ACTIONS (Mode 01 Live Sensors & Telemetry)
        item {
            Button(
                onClick = { onNavigateMode3() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .testTag("btn_mode_3"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HighDensitySurface,
                    contentColor = TextPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, HighDensityBorder)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(AutomotiveBlue.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Sensors, contentDescription = null, tint = AutomotiveBlue, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("ВСЕ ДАТЧИКИ И ПАРАМЕТРЫ", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Режим 01 (Live PID) — просмотр в реальном времени", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                        }
                    }
                    Text("›", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                }
            }
        }

        // Live Real-Time Alert Banner (if triggered)
        if (aiAlert != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftRedContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ClearRed)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = aiAlert ?: "",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = OnSoftRedText
                        )
                    }
                }
            }
        }
    }

    if (showSensorAlertsDialog) {
        SensorAlertsDialog(
            alerts = sensorAlerts,
            availableSensors = sensors,
            onDismiss = { showSensorAlertsDialog = false },
            onAddAlert = { newAlert -> viewModel.addSensorAlert(newAlert) },
            onUpdateAlert = { updatedAlert -> viewModel.updateSensorAlert(updatedAlert) },
            onToggleAlert = { id, enabled -> viewModel.toggleSensorAlert(id, enabled) },
            onRemoveAlert = { id -> viewModel.removeSensorAlert(id) },
            onExportProfile = { viewModel.exportProfileToFile(context) },
            onImportProfileJson = { jsonStr -> viewModel.importProfileFromJson(jsonStr) }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = ClearRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Выход из программы", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "Все ваши настройки, диалоги и предупреждения автоматически сохранены в файл профиля. Фоновая служба также будет остановлена.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        viewModel.exitApplication(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ClearRed, contentColor = Color.White)
                ) {
                    Text("Выйти из программы", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showDeviceDialog) {
        BluetoothDeviceDialog(
            devices = pairedDevicesList,
            isBluetoothEnabled = viewModel.isBluetoothEnabled(),
            onEnableBluetooth = {
                viewModel.enableBluetooth()
                pairedDevicesList = viewModel.getPairedDevices()
            },
            onRefreshList = {
                viewModel.enableBluetooth()
                pairedDevicesList = viewModel.getPairedDevices()
            },
            onSelectDevice = { device ->
                showDeviceDialog = false
                viewModel.connectBluetoothDevice(device)
            },
            onDismiss = { showDeviceDialog = false }
        )
    }

    if (showProfileImportDialog) {
        AlertDialog(
            onDismissRequest = { showProfileImportDialog = false },
            containerColor = HighDensitySurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Восстановление профиля",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Вставьте скопированный JSON-текст профиля для мгновенного восстановления настроек:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        placeholder = { Text("{\"version\":1, \"alerts\": [...]}") },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                    )
                    if (importErrorMessage != null) {
                        Text(
                            text = importErrorMessage!!,
                            fontSize = 12.sp,
                            color = ClearRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJsonText.isBlank()) {
                            importErrorMessage = "Введите JSON-данные профиля"
                        } else {
                            val success = viewModel.importProfileFromJson(importJsonText)
                            if (success) {
                                showProfileImportDialog = false
                                Toast.makeText(context, "✅ Профиль успешно восстановлен!", Toast.LENGTH_SHORT).show()
                            } else {
                                importErrorMessage = "Ошибка структуры JSON файла"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AutomotiveBlue)
                ) {
                    Text("Восстановить", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showProfileImportDialog = false }) {
                    Text("Отмена", color = TextSecondary)
                }
            }
        )
    }

    if (showProtocolDialog) {
        ObdProtocolSelectionDialog(
            availableProtocols = viewModel.availableProtocols,
            currentProtocolCode = selectedProtocolCode,
            onSelectProtocol = { protoCode ->
                viewModel.applyProtocol(protoCode) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showProtocolDialog = false }
        )
    }

    if (showFuelStrategyDialog) {
        FuelStrategySelectionDialog(
            strategies = viewModel.availableFuelStrategies,
            currentStrategyId = selectedFuelStratId,
            onSelectStrategy = { stratId ->
                viewModel.setFuelStrategy(stratId)
                Toast.makeText(context, "Выбран метод уровня топлива", Toast.LENGTH_SHORT).show()
            },
            onTestStrategy = { stratId, callback ->
                viewModel.testFuelStrategy(stratId, callback)
            },
            onDismiss = { showFuelStrategyDialog = false }
        )
    }

    if (showVoiceSettingsDialog) {
        VoiceAssistantSettingsDialog(
            isHandsFreeActive = isHandsFreeActive,
            currentWakePhrase = wakePhrase,
            currentReadyResponse = readyResponse,
            currentMode = handsFreeMode,
            lastDetectedPhrase = lastDetectedPhrase,
            audioLevel = 0f,
            audioLevelProvider = { micAudioLevel },
            currentWorkMode = handsFreeWorkMode,
            onSetWorkMode = { viewModel.setHandsFreeWorkMode(it) },
            onSaveSettings = { finalWake, finalResp, isEnabled ->
                if (isEnabled) {
                    val hasAudio = androidx.core.content.ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (!hasAudio) {
                        try {
                            silentAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } catch (e: Exception) {}
                    }
                }
                val wasActive = isHandsFreeActive
                viewModel.updateHandsFreeSettings(finalWake, finalResp, isEnabled)
                if (wasActive != isEnabled) {
                    Toast.makeText(
                        context,
                        if (isEnabled) "Hands-Free активирован («$finalWake»)" else "Hands-Free выключен",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onTestWakeWord = { testWake, testResp, callback ->
                val hasAudio = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!hasAudio) {
                    try {
                        silentAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } catch (e: Exception) {}
                }
                viewModel.testWakeWord(testWake, testResp, callback)
            },
            onTestVoiceResponse = { testText ->
                viewModel.testResponseVoice(testText)
            },
            onDismiss = { showVoiceSettingsDialog = false }
        )
    }
}

@SuppressLint("MissingPermission")
@Composable
fun BluetoothDeviceDialog(
    devices: List<BluetoothDevice>,
    isBluetoothEnabled: Boolean,
    onEnableBluetooth: () -> Unit,
    onRefreshList: () -> Unit,
    onSelectDevice: (BluetoothDevice) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = HighDensitySurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(AutomotiveBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Выбор устройства ELM327",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isBluetoothEnabled) "Блютуз включен" else "Блютуз выключен",
                        fontSize = 11.sp,
                        color = if (isBluetoothEnabled) StatusGreen else ClearRed
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HorizontalDivider(color = HighDensityBorder)

                if (!isBluetoothEnabled) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SoftRedContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Блютуз выключен на вашем устройстве. Нажмите кнопку ниже, чтобы включить его.",
                                fontSize = 12.sp,
                                color = OnSoftRedText
                            )
                            Button(
                                onClick = onEnableBluetooth,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = ClearRed, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Включить Bluetooth", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else if (devices.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BluetoothDisabled,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "Сопряженные Bluetooth устройства не найдены",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Убедитесь, что адаптер ELM327 v1.5 включен и сопряжен в настройках Bluetooth вашего смартфона.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Ignore if settings intent fails
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Настройки", fontSize = 11.sp)
                            }

                            Button(
                                onClick = onRefreshList,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AutomotiveBlue),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Обновить", fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Выберите адаптер для подключения:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(devices.size) { index ->
                            val dev = devices[index]
                            val deviceName = try { dev.name ?: "Неизвестное устройство" } catch (e: Exception) { "ELM327 OBD2" }
                            val deviceAddress = dev.address ?: "00:00:00:00:00:00"

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectDevice(dev) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = HighDensitySurfaceVariant),
                                border = androidx.compose.foundation.BorderStroke(1.dp, HighDensityBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(AutomotiveBlue.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bluetooth,
                                            contentDescription = null,
                                            tint = AutomotiveBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = deviceName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = deviceAddress,
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    Button(
                                        onClick = { onSelectDevice(dev) },
                                        colors = ButtonDefaults.buttonColors(containerColor = AutomotiveBlue),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("Выбрать", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = TextSecondary)
            }
        }
    )
}

