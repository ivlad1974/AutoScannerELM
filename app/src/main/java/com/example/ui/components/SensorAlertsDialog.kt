package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.AlertCondition
import com.example.data.ObdSensor
import com.example.data.SensorAlert
import com.example.ui.theme.*

private val HighContrastLabelColor = Color(0xFFCBD5E1)
private val HighContrastBorderColor = Color(0xFF475569)

@Composable
fun SensorAlertsDialog(
    alerts: List<SensorAlert>,
    availableSensors: List<ObdSensor>,
    onDismiss: () -> Unit,
    onAddAlert: (SensorAlert) -> Unit,
    onUpdateAlert: (SensorAlert) -> Unit,
    onToggleAlert: (String, Boolean) -> Unit,
    onRemoveAlert: (String) -> Unit,
    onExportProfile: (() -> Unit)? = null,
    onImportProfileJson: ((String) -> Boolean)? = null
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var alertToEdit by remember { mutableStateOf<SensorAlert?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var importStatus by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            color = HighDensitySurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, HighDensityBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxHeight(0.88f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(ClearRed.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = ClearRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "МОНИТОРИНГ И ПРЕДУПРЕЖДЕНИЯ",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AutomotiveBlue
                            )
                            Text(
                                text = "Отслеживание датчиков",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = HighContrastLabelColor)
                    }
                }

                Text(
                    text = "Нажмите на любое предупреждение, чтобы изменить датчик, порог и период повтора. Настройки сохраняются автоматически.",
                    fontSize = 12.sp,
                    color = HighContrastLabelColor,
                    lineHeight = 16.sp
                )

                // Profile Save / Restore Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onExportProfile?.invoke()
                            android.widget.Toast.makeText(context, "Профиль сохранен (obd2_profile_backup.json)", android.widget.Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HighContrastBorderColor)
                    ) {
                        Text("💾 Экспорт профиля", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HighContrastBorderColor)
                    ) {
                        Text("📥 Импорт профиля", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Alert list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (alerts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Нет активных предупреждений.\nНажмите «Добавить» для настройки отслеживания.",
                                    fontSize = 13.sp,
                                    color = HighContrastLabelColor,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    items(alerts, key = { it.id }) { alert ->
                        AlertItemCard(
                            alert = alert,
                            onClick = { alertToEdit = alert },
                            onToggle = { enabled -> onToggleAlert(alert.id, enabled) },
                            onDelete = { onRemoveAlert(alert.id) }
                        )
                    }
                }

                // Add button
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_add_sensor_alert"),
                    colors = ButtonDefaults.buttonColors(containerColor = AutomotiveBlue, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Добавить порог предупреждения", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (alertToEdit != null) {
        EditSensorAlertDialog(
            alert = alertToEdit!!,
            availableSensors = availableSensors,
            onDismiss = { alertToEdit = null },
            onConfirm = { updatedAlert ->
                onUpdateAlert(updatedAlert)
                alertToEdit = null
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Импорт профиля", fontWeight = FontWeight.Bold, color = Color.White) },
            containerColor = HighDensitySurface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Вставьте JSON профиля или текст конфигурации:", fontSize = 12.sp, color = HighContrastLabelColor)
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        placeholder = { Text("{\"version\":1, \"alerts\": [...]}") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AutomotiveBlue,
                            unfocusedBorderColor = HighContrastBorderColor,
                            focusedPlaceholderColor = Color(0xFF94A3B8),
                            unfocusedPlaceholderColor = Color(0xFF64748B)
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White)
                    )
                    if (importStatus != null) {
                        Text(importStatus!!, fontSize = 12.sp, color = ClearRed, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importText.isBlank()) {
                            importStatus = "Введите JSON профиля"
                        } else {
                            val success = onImportProfileJson?.invoke(importText) ?: false
                            if (success) {
                                showImportDialog = false
                                android.widget.Toast.makeText(context, "Профиль успешно восстановлен!", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                importStatus = "Ошибка парсинга файла профиля"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AutomotiveBlue)
                ) {
                    Text("Восстановить", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Отмена", color = HighContrastLabelColor)
                }
            }
        )
    }

    if (showAddDialog) {
        AddSensorAlertDialog(
            availableSensors = availableSensors,
            onDismiss = { showAddDialog = false },
            onConfirm = { newAlert ->
                onAddAlert(newAlert)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AlertItemCard(
    alert: SensorAlert,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val condSymbol = if (alert.condition == AlertCondition.GREATER_THAN) ">" else "<"
    val condLabel = if (alert.condition == AlertCondition.GREATER_THAN) "Превышение" else "Падение ниже"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alert.isEnabled) Color(0xFF1E293B) else Color(0xFF0F172A)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (alert.isEnabled) ClearRed.copy(alpha = 0.5f) else HighContrastBorderColor
        )
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = alert.sensorName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Редактировать",
                        tint = AutomotiveBlue,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (alert.condition == AlertCondition.GREATER_THAN) ClearRed.copy(alpha = 0.2f) else SoftBlueContainer,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "$condLabel $condSymbol ${com.example.data.formatSensorValue(alert.thresholdValue, alert.sensorPid, alert.unit)} ${alert.unit}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (alert.condition == AlertCondition.GREATER_THAN) Color(0xFFFF6B6B) else Color(0xFF60A5FA)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = HighContrastLabelColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Повтор: каждые ${alert.repeatIntervalSeconds} сек • Нажмите для изменения",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = HighContrastLabelColor
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = alert.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ClearRed,
                        uncheckedThumbColor = Color(0xFF94A3B8),
                        uncheckedTrackColor = Color(0xFF334155)
                    )
                )

                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = ClearRed.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSensorAlertDialog(
    availableSensors: List<ObdSensor>,
    onDismiss: () -> Unit,
    onConfirm: (SensorAlert) -> Unit
) {
    var selectedSensor by remember { mutableStateOf(availableSensors.firstOrNull() ?: ObdSensor("0105", "Температура двигателя", 89.0, "°C", -40.0, 150.0, "Охлаждение")) }
    var condition by remember { mutableStateOf(AlertCondition.GREATER_THAN) }
    var thresholdText by remember { mutableStateOf("95") }
    var repeatSecondsText by remember { mutableStateOf("30") }
    var isExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            color = HighDensitySurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, HighDensityBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Новое предупреждение",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // 1. Choose sensor
                Text(text = "Выберите датчик:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = HighContrastLabelColor)
                ExposedDropdownMenuBox(
                    expanded = isExpanded,
                    onExpandedChange = { isExpanded = !isExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedSensor.name,
                        onValueChange = {},
                        readOnly = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AutomotiveBlue,
                            unfocusedBorderColor = HighContrastBorderColor,
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = isExpanded,
                        onDismissRequest = { isExpanded = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        availableSensors.forEach { sensor ->
                            DropdownMenuItem(
                                text = { Text(sensor.name, color = Color.White, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    selectedSensor = sensor
                                    thresholdText = when (sensor.pid) {
                                        "0105" -> "95"
                                        "0142" -> "12.0"
                                        "010C" -> "4500"
                                        "010D" -> "110"
                                        "012F" -> "15"
                                        else -> "50"
                                    }
                                    isExpanded = false
                                }
                            )
                        }
                    }
                }

                // 2. Condition
                Text(text = "Условие срабатывания:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = HighContrastLabelColor)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = condition == AlertCondition.GREATER_THAN,
                        onClick = { condition = AlertCondition.GREATER_THAN },
                        label = { Text("Превышение ( > )", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ClearRed,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = condition == AlertCondition.LESS_THAN,
                        onClick = { condition = AlertCondition.LESS_THAN },
                        label = { Text("Падение ниже ( < )", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AutomotiveBlue,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // 3. Threshold value
                Text(text = "Величина срабатывания (${selectedSensor.unit}):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = HighContrastLabelColor)
                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = { thresholdText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AutomotiveBlue,
                        unfocusedBorderColor = HighContrastBorderColor,
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // 4. Repeat interval in SECONDS (Input field)
                Text(text = "Период повторов (в секундах):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = HighContrastLabelColor)
                OutlinedTextField(
                    value = repeatSecondsText,
                    onValueChange = { repeatSecondsText = it.filter { ch -> ch.isDigit() } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AutomotiveBlue,
                        unfocusedBorderColor = HighContrastBorderColor,
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    ),
                    suffix = { Text("сек", color = HighContrastLabelColor, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена", color = HighContrastLabelColor)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val valDouble = thresholdText.toDoubleOrNull() ?: 50.0
                            val secInt = repeatSecondsText.toIntOrNull()?.coerceIn(1, 3600) ?: 30
                            val newAlert = SensorAlert(
                                sensorPid = selectedSensor.pid,
                                sensorName = selectedSensor.name,
                                condition = condition,
                                thresholdValue = valDouble,
                                unit = selectedSensor.unit,
                                customMessage = "Предупреждение по ${selectedSensor.name}",
                                repeatIntervalSeconds = secInt
                            )
                            onConfirm(newAlert)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AutomotiveBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Добавить", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSensorAlertDialog(
    alert: SensorAlert,
    availableSensors: List<ObdSensor>,
    onDismiss: () -> Unit,
    onConfirm: (SensorAlert) -> Unit
) {
    var selectedSensor by remember {
        mutableStateOf(
            availableSensors.find { it.pid == alert.sensorPid }
                ?: ObdSensor(alert.sensorPid, alert.sensorName, alert.thresholdValue, alert.unit, 0.0, 200.0, "Датчик")
        )
    }
    var condition by remember { mutableStateOf(alert.condition) }
    var thresholdText by remember {
        mutableStateOf(
            if (alert.thresholdValue % 1.0 == 0.0) alert.thresholdValue.toInt().toString()
            else alert.thresholdValue.toString()
        )
    }
    var repeatSecondsText by remember { mutableStateOf(alert.repeatIntervalSeconds.toString()) }
    var isExpanded by remember { mutableStateOf(false) }

    fun triggerSave(
        s: ObdSensor = selectedSensor,
        c: AlertCondition = condition,
        tText: String = thresholdText,
        rText: String = repeatSecondsText
    ) {
        val valDouble = tText.toDoubleOrNull() ?: alert.thresholdValue
        val secInt = rText.toIntOrNull()?.coerceIn(1, 3600) ?: alert.repeatIntervalSeconds
        val updated = alert.copy(
            sensorPid = s.pid,
            sensorName = s.name,
            condition = c,
            thresholdValue = valDouble,
            unit = s.unit,
            customMessage = "Предупреждение по ${s.name}",
            repeatIntervalSeconds = secInt
        )
        onConfirm(updated)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            color = HighDensitySurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, HighDensityBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with title and auto-save indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Настройка отслеживания",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "✓ Сохраняется автоматически",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = StatusGreen
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = Color.White
                        )
                    }
                }

                // 1. Choose sensor
                Text(
                    text = "Отслеживаемый датчик:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HighContrastLabelColor
                )
                ExposedDropdownMenuBox(
                    expanded = isExpanded,
                    onExpandedChange = { isExpanded = !isExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedSensor.name,
                        onValueChange = {},
                        readOnly = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AutomotiveBlue,
                            unfocusedBorderColor = HighContrastBorderColor,
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = isExpanded,
                        onDismissRequest = { isExpanded = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        availableSensors.forEach { sensor ->
                            DropdownMenuItem(
                                text = { Text(sensor.name, color = Color.White, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    selectedSensor = sensor
                                    isExpanded = false
                                    triggerSave(s = sensor)
                                }
                            )
                        }
                    }
                }

                // 2. Condition
                Text(
                    text = "Условие срабатывания:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HighContrastLabelColor
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = condition == AlertCondition.GREATER_THAN,
                        onClick = {
                            condition = AlertCondition.GREATER_THAN
                            triggerSave(c = AlertCondition.GREATER_THAN)
                        },
                        label = { Text("Превышение ( > )", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ClearRed,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = condition == AlertCondition.LESS_THAN,
                        onClick = {
                            condition = AlertCondition.LESS_THAN
                            triggerSave(c = AlertCondition.LESS_THAN)
                        },
                        label = { Text("Падение ниже ( < )", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AutomotiveBlue,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // 3. Threshold value
                Text(
                    text = "Величина срабатывания (${selectedSensor.unit}):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HighContrastLabelColor
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            val curr = thresholdText.toDoubleOrNull() ?: alert.thresholdValue
                            val step = if (selectedSensor.pid == "0142") 0.2 else if (selectedSensor.pid == "010C") 100.0 else 1.0
                            val newVal = curr - step
                            val formatted = if (newVal % 1.0 == 0.0) newVal.toInt().toString() else String.format(java.util.Locale.US, "%.1f", newVal)
                            thresholdText = formatted
                            triggerSave(tText = formatted)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(48.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF334155), contentColor = Color.White),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("−", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    }

                    OutlinedTextField(
                        value = thresholdText,
                        onValueChange = {
                            thresholdText = it
                            triggerSave(tText = it)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AutomotiveBlue,
                            unfocusedBorderColor = HighContrastBorderColor,
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    FilledTonalButton(
                        onClick = {
                            val curr = thresholdText.toDoubleOrNull() ?: alert.thresholdValue
                            val step = if (selectedSensor.pid == "0142") 0.2 else if (selectedSensor.pid == "010C") 100.0 else 1.0
                            val newVal = curr + step
                            val formatted = if (newVal % 1.0 == 0.0) newVal.toInt().toString() else String.format(java.util.Locale.US, "%.1f", newVal)
                            thresholdText = formatted
                            triggerSave(tText = formatted)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(48.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF334155), contentColor = Color.White),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("+", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                // 4. Repeat interval in SECONDS (Input field only, no buttons)
                Text(
                    text = "Период повторов (в секундах):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HighContrastLabelColor
                )
                OutlinedTextField(
                    value = repeatSecondsText,
                    onValueChange = {
                        val digits = it.filter { ch -> ch.isDigit() }
                        repeatSecondsText = digits
                        triggerSave(rText = digits)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AutomotiveBlue,
                        unfocusedBorderColor = HighContrastBorderColor,
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    ),
                    suffix = { Text("сек", color = HighContrastLabelColor, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        }
    }
}
