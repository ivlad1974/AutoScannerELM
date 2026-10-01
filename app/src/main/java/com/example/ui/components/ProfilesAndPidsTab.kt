package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ObdSensor
import com.example.data.SupportedPidsResult
import com.example.data.elm327.FormulaEvaluator
import com.example.data.elm327.ManufacturerProfile
import com.example.ui.theme.AutomotiveBlue
import com.example.ui.theme.ClearRed
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ProfilesAndPidsTab(
    supportedPidsResult: SupportedPidsResult,
    activeProfile: ManufacturerProfile?,
    availableProfiles: List<ManufacturerProfile>,
    allSensors: List<ObdSensor>,
    onRunPidDiscovery: () -> Unit,
    onLoadProfile: (String) -> Unit,
    onUnloadProfile: () -> Unit,
    onAddCustomSensor: (ObdSensor) -> Unit,
    onRemoveCustomSensor: (String) -> Unit,
    onImportCsv: (String) -> Int,
    onExportCsv: () -> String
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportContent by remember { mutableStateOf("") }
    var isDiscovering by remember { mutableStateOf(false) }

    val customSensors = remember(allSensors) { allSensors.filter { it.isCustom } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. PID BITMAP DISCOVERY SECTION ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, AutomotiveBlue.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().testTag("card_pid_discovery")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "АВТООПРЕДЕЛЕНИЕ ПОДДЕРЖКИ PID",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AutomotiveBlue,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Битовые карты ЭБУ (0100 / 0120 / ...)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(
                            onClick = {
                                isDiscovering = true
                                onRunPidDiscovery()
                                Toast.makeText(context, "Запущен опрос битовых карт PID...", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AutomotiveBlue),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_run_pid_discovery")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Сканировать", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Summary Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = StatusGreen.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Mode 01 PID", fontSize = 10.sp, color = TextSecondary)
                                Text(
                                    "${supportedPidsResult.supportedPids.count { it.startsWith("01") }} активно",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusGreen
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = AutomotiveBlue.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, AutomotiveBlue.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Mode 09 Инфо", fontSize = 10.sp, color = TextSecondary)
                                Text(
                                    "${supportedPidsResult.mode09SupportedPids.size} инфо",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AutomotiveBlue
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Mode 06 Мониторы", fontSize = 10.sp, color = TextSecondary)
                                Text(
                                    "${supportedPidsResult.mode06SupportedTests.size} тестов",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (supportedPidsResult.discoveredHeaders.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Обнаруженные шинные адреса ЭБУ (Headers):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(supportedPidsResult.discoveredHeaders) { header ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                ) {
                                    Text(
                                        text = "CAN $header",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 2. MANUFACTURER SPECIFIC PROFILES SECTION ---
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ЗАВОДСКИЕ ПРОФИЛИ ДАТЧИКОВ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AutomotiveBlue,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Расширенные PID производителей",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (activeProfile != null) {
                        OutlinedButton(
                            onClick = {
                                onUnloadProfile()
                                Toast.makeText(context, "Профиль отключен", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ClearRed),
                            border = BorderStroke(1.dp, ClearRed),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("btn_unload_profile")
                        ) {
                            Text("Отключить", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                availableProfiles.forEach { profile ->
                    val isActive = activeProfile?.id == profile.id
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            if (isActive) 1.5.dp else 1.dp,
                            if (isActive) AutomotiveBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("card_profile_${profile.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = if (isActive) AutomotiveBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = profile.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isActive) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = StatusGreen.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, StatusGreen)
                                        ) {
                                            Text(
                                                "АКТИВЕН",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = StatusGreen,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${profile.sensors.size} доп. датчиков • Header: ${profile.defaultHeader} • ${profile.description}",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    lineHeight = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (isActive) onUnloadProfile() else onLoadProfile(profile.id)
                                    Toast.makeText(context, if (isActive) "Профиль отключен" else "Загружен: ${profile.name}", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isActive) ClearRed else AutomotiveBlue
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("btn_toggle_profile_${profile.id}")
                            ) {
                                Text(if (isActive) "Снять" else "Включить", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- 3. CUSTOM PIDS AND TORQUE PRO CSV SECTION ---
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ПОЛЬЗОВАТЕЛЬСКИЕ PID И CSV",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AutomotiveBlue,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Формулы и совместимость Torque Pro",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.testTag("btn_open_add_custom_sensor")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Добавить PID", tint = AutomotiveBlue)
                        }
                        IconButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier.testTag("btn_open_import_csv")
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = "Импорт CSV", tint = AutomotiveBlue)
                        }
                        IconButton(
                            onClick = {
                                exportContent = onExportCsv()
                                showExportDialog = true
                            },
                            modifier = Modifier.testTag("btn_open_export_csv")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Экспорт CSV", tint = AutomotiveBlue)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (customSensors.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "Пользовательские датчики пока не добавлены. Нажмите '+' для создания PID с формулой или кнопку импорта для загрузки CSV из Torque Pro.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    customSensors.forEach { sensor ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(sensor.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = AutomotiveBlue.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                sensor.pid,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = AutomotiveBlue,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Формула: ${sensor.formula} • Ед: ${sensor.unit} • Диапазон: ${sensor.minVal}..${sensor.maxVal}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                                IconButton(
                                    onClick = { onRemoveCustomSensor(sensor.pid) },
                                    modifier = Modifier.testTag("btn_remove_sensor_${sensor.pid}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = ClearRed, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- ADD CUSTOM PID DIALOG ---
    if (showAddDialog) {
        AddCustomPidDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { sensor ->
                onAddCustomSensor(sensor)
                showAddDialog = false
                Toast.makeText(context, "Датчик добавлен: ${sensor.name}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // --- IMPORT CSV DIALOG ---
    if (showImportDialog) {
        ImportCsvDialog(
            onDismiss = { showImportDialog = false },
            onImport = { csv ->
                val count = onImportCsv(csv)
                showImportDialog = false
                Toast.makeText(context, "Импортировано датчиков: $count", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // --- EXPORT CSV DIALOG ---
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Экспорт датчиков (Torque CSV)", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Скопируйте сгенерированный CSV для использования в Torque Pro или резервного копирования:", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportContent,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("AutoScan Sensors CSV", exportContent)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "CSV скопирован в буфер обмена!", Toast.LENGTH_SHORT).show()
                        showExportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AutomotiveBlue)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Скопировать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }
}

@Composable
private fun AddCustomPidDialog(
    onDismiss: () -> Unit,
    onAdd: (ObdSensor) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var pid by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("01") }
    var formula by remember { mutableStateOf("A") }
    var unit by remember { mutableStateOf("") }
    var minValStr by remember { mutableStateOf("0") }
    var maxValStr by remember { mutableStateOf("100") }
    var ecuHeader by remember { mutableStateOf("") }
    var testResult by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить пользовательский PID", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название датчика") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_custom_sensor_name")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = mode,
                        onValueChange = { mode = it },
                        label = { Text("Mode") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = pid,
                        onValueChange = { pid = it.uppercase() },
                        label = { Text("PID (hex)") },
                        modifier = Modifier.weight(1.5f).testTag("input_custom_sensor_pid"),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = formula,
                    onValueChange = {
                        formula = it
                        // Test formula on dummy byte 100
                        val dummy = intArrayOf(100, 50, 20, 10)
                        testResult = try {
                            val res = FormulaEvaluator.evaluate(it, dummy)
                            "Тест: [100, 50] -> ${"%.2f".format(res)}"
                        } catch (e: Exception) {
                            "Ошибка формулы"
                        }
                    },
                    label = { Text("Формула (A, B, C, D, +, -, *, /, скобки)") },
                    supportingText = {
                        Text(testResult ?: "Пример: (A*256+B)/10 или A-40", fontSize = 10.sp, color = TextSecondary)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("input_custom_sensor_formula")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Ед. изм.") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ecuHeader,
                        onValueChange = { ecuHeader = it.uppercase() },
                        label = { Text("Header (7E0)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && pid.isNotBlank()) {
                        val fullPid = if (pid.startsWith(mode)) pid else "$mode$pid"
                        val sensor = ObdSensor(
                            pid = fullPid,
                            name = name,
                            value = 0.0,
                            unit = unit,
                            minVal = minValStr.toDoubleOrNull() ?: 0.0,
                            maxVal = maxValStr.toDoubleOrNull() ?: 100.0,
                            category = "Пользовательские",
                            isSelected = true,
                            isSupported = true,
                            isCustom = true,
                            formula = formula,
                            mode = mode,
                            ecuHeader = ecuHeader,
                            description = "Пользовательский PID $fullPid с формулой $formula"
                        )
                        onAdd(sensor)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AutomotiveBlue),
                modifier = Modifier.testTag("btn_confirm_add_custom_sensor")
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
private fun ImportCsvDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var csvText by remember {
        mutableStateOf(
            "# AutoScan / Torque Pro CSV Sample\n" +
            "Name,ShortName,ModeAndPID,Equation,Min Value,Max Value,Units,Header\n" +
            "\"Температура АКПП ATF\",\"ATF Temp\",\"2102\",\"A-40\",-40,150,\"°C\",\"7E0\"\n" +
            "\"Уровень AdBlue/Мочевины\",\"AdBlue\",\"0185\",\"A*0.392\",0,100,\"%\",\"\"\n" +
            "\"Давление в рампе ТНВД High\",\"RailP_H\",\"0123\",\"(A*256+B)*10\",0,2000,\"bar\",\"\""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Импорт датчиков (Torque CSV)", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Вставьте строки CSV в формате Torque Pro (Name,ShortName,ModeAndPID,Equation,Min,Max,Units,Header):",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = csvText,
                    onValueChange = { csvText = it },
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(csvText) },
                colors = ButtonDefaults.buttonColors(containerColor = AutomotiveBlue)
            ) {
                Text("Импортировать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
