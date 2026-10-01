package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.data.ConnectionState
import com.example.data.ObdSensor
import com.example.data.formatSensorValue
import com.example.ui.MainViewModel
import com.example.ui.components.FuelStrategySelectionDialog
import com.example.ui.components.HUDGauge
import com.example.ui.components.ProfilesAndPidsTab
import com.example.ui.components.SensorChart
import com.example.ui.theme.AutomotiveBlue
import com.example.ui.theme.ClearRed
import com.example.ui.theme.HighDensitySurface
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Mode3LiveSensorsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateAiChat: () -> Unit
) {
    val context = LocalContext.current
    val sensors by viewModel.sensors.collectAsState()
    val isLogging by viewModel.isLogging.collectAsState()
    val vehicleInfo by viewModel.vehicleInfo.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var showFuelDialog by remember { mutableStateOf(false) }
    val selectedFuelStratId by viewModel.selectedFuelStrategyId.collectAsState()
    val supportedPidsResult by viewModel.supportedPidsResult.collectAsState()
    val activeManufacturerProfile by viewModel.activeManufacturerProfile.collectAsState()
    val availableManufacturerProfiles = viewModel.availableManufacturerProfiles

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Все") }
    var onlySupportedFilter by remember { mutableStateOf(false) }

    val categories = remember(sensors) {
        listOf("Все") + sensors.map { it.category }.distinct().sorted()
    }

    val filteredSensors by remember(sensors, searchQuery, selectedCategory, onlySupportedFilter) {
        derivedStateOf {
            sensors.filter { sensor ->
                val matchesCat = selectedCategory == "Все" || sensor.category.equals(selectedCategory, ignoreCase = true)
                val matchesQuery = searchQuery.isBlank() ||
                        sensor.name.contains(searchQuery, ignoreCase = true) ||
                        sensor.pid.contains(searchQuery, ignoreCase = true) ||
                        sensor.category.contains(searchQuery, ignoreCase = true)
                val matchesSupported = !onlySupportedFilter || sensor.isSupported || sensor.isCustom
                matchesCat && matchesQuery && matchesSupported
            }
        }
    }

    val selectedCount = remember(sensors) { sensors.count { it.isSelected } }
    val selectedSensors = remember(sensors) { sensors.filter { it.isSelected } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("btn_back_sensors")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "МОНИТОРИНГ И ЛОГИРОВАНИЕ (MODE 01)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AutomotiveBlue,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Датчики и параметры",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // CSV Logging Button
            Button(
                onClick = {
                    if (isLogging) {
                        viewModel.stopSensorLogging()
                    } else {
                        viewModel.startSensorLogging()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLogging) ClearRed else StatusGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("btn_toggle_logging")
            ) {
                Icon(
                    imageVector = if (isLogging) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isLogging) "Стоп CSV" else "Запись CSV",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Tabs: 0: Список и выбор датчиков, 1: HUD Приборы, 2: Живые графики
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 12.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Выбор датчиков",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$selectedCount/${sensors.size}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                modifier = Modifier.testTag("tab_sensor_selection")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "HUD Приборы",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$selectedCount",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                modifier = Modifier.testTag("tab_hud_gauges")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Живые графики",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (selectedTab == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    if (selectedTab == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$selectedCount",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (selectedTab == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                modifier = Modifier.testTag("tab_live_charts")
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Профили и PID",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (selectedTab == 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    if (selectedTab == 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (activeManufacturerProfile != null) "Активен" else "${sensors.count { it.isCustom }}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (selectedTab == 3) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                modifier = Modifier.testTag("tab_profiles_and_pids")
            )
        }

        // AI Telemetry Monitoring Live Banner
        val aiMonitoringState by viewModel.aiMonitoringState.collectAsState()
        AnimatedVisibility(
            visible = aiMonitoringState.isActive,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clickable { onNavigateAiChat() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.SoftBlueContainer),
                border = BorderStroke(1.5.dp, AutomotiveBlue)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = AutomotiveBlue,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "⏱️ ИИ отслеживает: ${aiMonitoringState.sensorName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AutomotiveBlue
                        )
                        Text(
                            text = "${aiMonitoringState.elapsedSeconds}/${aiMonitoringState.targetDurationSeconds}с • Значение: ${"%.1f".format(aiMonitoringState.currentLiveValue)} ${aiMonitoringState.unit} (Нажмите для перехода в чат)",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Button(
                        onClick = { onNavigateAiChat() },
                        colors = ButtonDefaults.buttonColors(containerColor = AutomotiveBlue),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Чат", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> {
                // Tab 0: Sensor Checklist with Live Values & Quick Actions
                SensorSelectionTab(
                    sensors = filteredSensors,
                    totalSensorsCount = sensors.size,
                    selectedCount = selectedCount,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onSelectCategory = { selectedCategory = it },
                    onlySupportedFilter = onlySupportedFilter,
                    onToggleOnlySupportedFilter = { onlySupportedFilter = !onlySupportedFilter },
                    supportedCount = supportedPidsResult.totalSupportedCount,
                    onToggleSensor = { pid -> viewModel.toggleSensorSelection(pid) },
                    onSelectAll = { viewModel.selectAllSensors() },
                    onDeselectAll = { viewModel.deselectAllSensors() },
                    onSelectDefault = { viewModel.selectDefaultSensors() },
                    onOpenFuelStrategyDialog = { showFuelDialog = true },
                    onOpenProfilesTab = { selectedTab = 3 },
                    onStartAiMonitoring = { sensor ->
                        viewModel.startAiSensorMonitoring(sensor.pid, sensor.name, sensor.unit, 15)
                    }
                )
            }
            1 -> {
                // Tab 1: HUD Gauges
                if (selectedSensors.isEmpty()) {
                    EmptySensorSelectionPrompt(
                        onOpenSelectionTab = { selectedTab = 0 },
                        message = "Нет выбранных датчиков для отображения на панели приборов."
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(selectedSensors, key = { it.pid }) { sensor ->
                            HUDGauge(
                                label = sensor.name,
                                value = sensor.value,
                                unit = sensor.unit,
                                minVal = sensor.minVal,
                                maxVal = sensor.maxVal,
                                warningThreshold = if (sensor.pid == "0105") 105.0 else null
                            )
                        }
                    }
                }
            }
            2 -> {
                // Tab 2: Detailed Live Sensor Charts
                if (selectedSensors.isEmpty()) {
                    EmptySensorSelectionPrompt(
                        onOpenSelectionTab = { selectedTab = 0 },
                        message = "Нет выбранных датчиков для отображения графиков."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(selectedSensors, key = { it.pid }) { sensor ->
                            SensorChart(
                                sensor = sensor,
                                onPeriodChange = { sec -> viewModel.setSensorGraphPeriod(sensor.pid, sec) }
                            )
                        }
                    }
                }
            }
            3 -> {
                // Tab 3: Manufacturer Profiles, Bitmaps & Custom PIDs
                ProfilesAndPidsTab(
                    supportedPidsResult = supportedPidsResult,
                    activeProfile = activeManufacturerProfile,
                    availableProfiles = availableManufacturerProfiles,
                    allSensors = sensors,
                    onRunPidDiscovery = { viewModel.runPidDiscovery() },
                    onLoadProfile = { profileId -> viewModel.loadManufacturerProfile(profileId) },
                    onUnloadProfile = { viewModel.unloadManufacturerProfile() },
                    onAddCustomSensor = { sensor -> viewModel.addCustomSensor(sensor) },
                    onRemoveCustomSensor = { pid -> viewModel.removeCustomSensor(pid) },
                    onImportCsv = { csv -> viewModel.importSensorsFromCsv(csv) },
                    onExportCsv = { viewModel.exportSensorsToCsv() }
                )
            }
        }
    }

    if (showFuelDialog) {
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
            onDismiss = { showFuelDialog = false }
        )
    }
}

@Composable
private fun SensorSelectionTab(
    sensors: List<ObdSensor>,
    totalSensorsCount: Int,
    selectedCount: Int,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    onlySupportedFilter: Boolean,
    onToggleOnlySupportedFilter: () -> Unit,
    supportedCount: Int,
    onToggleSensor: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onSelectDefault: () -> Unit,
    onOpenFuelStrategyDialog: () -> Unit,
    onOpenProfilesTab: () -> Unit,
    onStartAiMonitoring: (ObdSensor) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Quick Selection Action Bar
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Управление выбором датчиков",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Выбрано $selectedCount из $totalSensorsCount • Подтверждено ЭБУ: $supportedCount",
                                fontSize = 12.sp,
                                color = AutomotiveBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenProfilesTab,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("btn_goto_profiles_tab")
                        ) {
                            Text("Профили / PID", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSelectAll,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_select_all_sensors"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp), tint = AutomotiveBlue)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Все", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onDeselectAll,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_deselect_all_sensors"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp), tint = ClearRed)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Снять", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onSelectDefault,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_select_default_sensors"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = StatusGreen)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Базовые", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onToggleOnlySupportedFilter,
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("btn_filter_only_supported"),
                            shape = RoundedCornerShape(8.dp),
                            colors = if (onlySupportedFilter) ButtonDefaults.outlinedButtonColors(containerColor = StatusGreen.copy(alpha = 0.15f)) else ButtonDefaults.outlinedButtonColors(),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text(
                                if (onlySupportedFilter) "✓ Только ЭБУ" else "Поддерж. ЭБУ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (onlySupportedFilter) StatusGreen else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        "Поиск датчика по названию...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Очистить",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AutomotiveBlue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_search_sensor")
            )
        }

        // Category Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isCatSelected = selectedCategory == cat
                    Surface(
                        onClick = { onSelectCategory(cat) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCatSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isCatSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.testTag("chip_category_$cat")
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = if (isCatSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (isCatSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Sensor Checklist with live values
        items(sensors, key = { it.pid }) { sensor ->
            SensorChecklistItem(
                sensor = sensor,
                onToggle = { onToggleSensor(sensor.pid) },
                onOpenFuelStrategyDialog = onOpenFuelStrategyDialog,
                onStartAiMonitoring = { onStartAiMonitoring(sensor) }
            )
        }
    }
}

@Composable
private fun SensorChecklistItem(
    sensor: ObdSensor,
    onToggle: () -> Unit,
    onOpenFuelStrategyDialog: () -> Unit,
    onStartAiMonitoring: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (sensor.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        label = "border_color"
    )

    val formattedLiveVal = remember(sensor.value, sensor.pid, sensor.unit) {
        formatSensorValue(sensor.value, sensor.pid, sensor.unit)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (sensor.isSelected) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (sensor.isSelected) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .testTag("sensor_item_${sensor.pid}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Main Top Row: Checkbox, PID badge, Sensor Name, and Live Value
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox
                Checkbox(
                    checked = sensor.isSelected,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("checkbox_${sensor.pid}")
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Sensor Name (Large, clear typography)
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = sensor.name,
                            fontSize = 15.sp,
                            fontWeight = if (sensor.isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (sensor.isSupported) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = StatusGreen.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "✓ ЭБУ",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StatusGreen,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        if (sensor.isCustom) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AutomotiveBlue.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, AutomotiveBlue.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "Польз.",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AutomotiveBlue,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Live Real-Time Value Box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (sensor.isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (sensor.isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.Transparent
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = formattedLiveVal,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = if (sensor.isSelected) {
                                    if (sensor.pid == "0105" && sensor.value > 102.0) ClearRed else MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = sensor.unit,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 1.dp)
                            )
                        }
                    }
                }
            }

            // Description and Meta Section
            if (sensor.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = sensor.description,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom Meta tags: Category, Range, and Transmission Status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = sensor.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (sensor.pid == "012F" || sensor.name.contains("топлив", ignoreCase = true)) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            onClick = onOpenFuelStrategyDialog,
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = "⚙ Метод топлива",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Диапазон: ${sensor.minVal.toInt()}..${sensor.maxVal.toInt()} ${sensor.unit}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = onStartAiMonitoring,
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "⏱️ ИИ Замер",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (sensor.isSelected) "✓ Активен" else "Отключен",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (sensor.isSelected) StatusGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptySensorSelectionPrompt(
    onOpenSelectionTab: () -> Unit,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Датчики не выбраны",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = message,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onOpenSelectionTab,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("btn_goto_sensor_selection")
                ) {
                    Text("Перейти к выбору датчиков", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

