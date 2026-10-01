package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DashboardTileConfig
import com.example.data.ObdSensor
import com.example.data.TileDisplayStyle
import com.example.ui.theme.AutomotiveBlue
import com.example.ui.theme.ClearRed
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.WarningAmber
import kotlin.math.cos
import kotlin.math.sin

/**
 * 4 Configurable Dashboard Tiles Grid:
 * - Single Tap: Expands the tapped tile to take the size of all 4 tiles (hiding others). Tapping again restores grid.
 * - Long Tap: Opens a customization dialog to change sensor and display style (Digital, Gauge HUD, Live Graph, Thermal Bar).
 */
@Composable
fun DashboardTilesGrid(
    tileConfigs: List<DashboardTileConfig>,
    sensors: List<ObdSensor>,
    isConnected: Boolean,
    onUpdateConfig: (slotIndex: Int, newPid: String, newStyle: TileDisplayStyle, newTimeRange: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedSlot by rememberSaveable { mutableStateOf<Int?>(null) }
    var customizeSlot by rememberSaveable { mutableStateOf<Int?>(null) }

    // Fallback default 4 configs if not initialized
    val effectiveConfigs = remember(tileConfigs) {
        val list = tileConfigs.toMutableList()
        val defaultPids = listOf("010D", "010C", "0105", "0142")
        val defaultStyles = listOf(
            TileDisplayStyle.GRAPH_WAVE,
            TileDisplayStyle.GRAPH_WAVE,
            TileDisplayStyle.GRAPH_WAVE,
            TileDisplayStyle.GRAPH_WAVE
        )
        for (i in 0 until 4) {
            if (list.none { it.slotIndex == i }) {
                list.add(DashboardTileConfig(i, defaultPids[i], defaultStyles[i], 5))
            }
        }
        list.sortedBy { it.slotIndex }.take(4)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Expand/Collapse Mode View
        AnimatedContent(
            targetState = expandedSlot,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
            label = "DashboardExpansionTransition"
        ) { activeExpandedSlot ->
            if (activeExpandedSlot != null) {
                // EXPANDED SINGLE FULL-SIZE TILE (Takes size of all 4 tiles ~ 312dp)
                val config = effectiveConfigs.find { it.slotIndex == activeExpandedSlot }
                    ?: DashboardTileConfig(activeExpandedSlot, "010D", TileDisplayStyle.DIGITAL, 5)
                val sensor = sensors.find { it.pid == config.sensorPid }
                    ?: fallbackSensorForPid(config.sensorPid)

                ExpandedDashboardTile(
                    slotIndex = activeExpandedSlot,
                    config = config,
                    sensor = sensor,
                    isConnected = isConnected,
                    onCollapse = { expandedSlot = null },
                    onOpenCustomize = { customizeSlot = activeExpandedSlot },
                    onTimeRangeChange = { newRange ->
                        onUpdateConfig(activeExpandedSlot, config.sensorPid, config.displayStyle, newRange)
                    }
                )
            } else {
                // 2x2 NORMAL 4 TILES GRID
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Row 1: Slot 0 & Slot 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (i in 0..1) {
                            val config = effectiveConfigs.getOrNull(i)
                                ?: DashboardTileConfig(i, if (i == 0) "010D" else "010C", TileDisplayStyle.DIGITAL, 5)
                            val sensor = sensors.find { it.pid == config.sensorPid }
                                ?: fallbackSensorForPid(config.sensorPid)

                            DashboardTileCard(
                                slotIndex = i,
                                config = config,
                                sensor = sensor,
                                isConnected = isConnected,
                                onClick = { expandedSlot = i },
                                onLongClick = { customizeSlot = i },
                                onTimeRangeChange = { newRange ->
                                    onUpdateConfig(i, config.sensorPid, config.displayStyle, newRange)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Row 2: Slot 2 & Slot 3
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (i in 2..3) {
                            val config = effectiveConfigs.getOrNull(i)
                                ?: DashboardTileConfig(i, if (i == 2) "0105" else "0142", TileDisplayStyle.DIGITAL, 5)
                            val sensor = sensors.find { it.pid == config.sensorPid }
                                ?: fallbackSensorForPid(config.sensorPid)

                            DashboardTileCard(
                                slotIndex = i,
                                config = config,
                                sensor = sensor,
                                isConnected = isConnected,
                                onClick = { expandedSlot = i },
                                onLongClick = { customizeSlot = i },
                                onTimeRangeChange = { newRange ->
                                    onUpdateConfig(i, config.sensorPid, config.displayStyle, newRange)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Subtle hint for user interaction
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡 Тап — во весь экран • Долгий тап — настроить прибор",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }

    // Customization Dialog for Long Press
    if (customizeSlot != null) {
        val currentSlot = customizeSlot!!
        val currentConfig = effectiveConfigs.find { it.slotIndex == currentSlot }
            ?: DashboardTileConfig(currentSlot, "010D", TileDisplayStyle.DIGITAL, 5)

        CustomizeDashboardTileDialog(
            slotIndex = currentSlot,
            currentConfig = currentConfig,
            allSensors = sensors,
            onDismiss = { customizeSlot = null },
            onSave = { newPid, newStyle, newRange ->
                onUpdateConfig(currentSlot, newPid, newStyle, newRange)
                customizeSlot = null
            }
        )
    }
}

@Composable
fun DashboardTilesGrid(
    tileConfigs: List<DashboardTileConfig>,
    sensors: List<ObdSensor>,
    isConnected: Boolean,
    onUpdateConfig: (slotIndex: Int, newPid: String, newStyle: TileDisplayStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    DashboardTilesGrid(
        tileConfigs = tileConfigs,
        sensors = sensors,
        isConnected = isConnected,
        onUpdateConfig = { slot, pid, style, _ -> onUpdateConfig(slot, pid, style) },
        modifier = modifier
    )
}

/**
 * Backward compatibility overload for existing calls
 */
@Composable
fun DashboardTilesGrid(
    speed: Double,
    rpm: Double,
    coolantTemp: Double,
    batteryVoltage: Double,
    isConnected: Boolean,
    onTileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dummySensors = listOf(
        ObdSensor(pid = "010D", name = "Скорость", value = speed, unit = "км/ч", minVal = 0.0, maxVal = 260.0, category = "Движение"),
        ObdSensor(pid = "010C", name = "Обороты (RPM)", value = rpm, unit = "об/мин", minVal = 0.0, maxVal = 8000.0, category = "Двигатель"),
        ObdSensor(pid = "0105", name = "Температура ОЖ", value = coolantTemp, unit = "°C", minVal = -40.0, maxVal = 140.0, category = "Температура"),
        ObdSensor(pid = "0142", name = "Напряжение АКБ", value = batteryVoltage, unit = "В", minVal = 9.0, maxVal = 16.0, category = "Электрика")
    )
    val dummyConfigs = listOf(
        DashboardTileConfig(0, "010D", TileDisplayStyle.GRAPH_WAVE),
        DashboardTileConfig(1, "010C", TileDisplayStyle.GRAPH_WAVE),
        DashboardTileConfig(2, "0105", TileDisplayStyle.GRAPH_WAVE),
        DashboardTileConfig(3, "0142", TileDisplayStyle.GRAPH_WAVE)
    )

    DashboardTilesGrid(
        tileConfigs = dummyConfigs,
        sensors = dummySensors,
        isConnected = isConnected,
        onUpdateConfig = { _, _, _ -> onTileClick() },
        modifier = modifier
    )
}

/**
 * Compact Dashboard Tile Card (150dp height in 2x2 grid)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardTileCard(
    slotIndex: Int,
    config: DashboardTileConfig,
    sensor: ObdSensor,
    isConnected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onTimeRangeChange: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accentColor = getSensorAccentColor(sensor, isConnected)
    val animatedAccent by animateColorAsState(targetValue = accentColor, label = "tile_accent")

    Card(
        modifier = modifier
            .height(152.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.5.dp, animatedAccent.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("dashboard_tile_slot_$slotIndex"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (config.displayStyle) {
                TileDisplayStyle.DIGITAL -> {
                    CompactDigitalStyle(sensor = sensor, isConnected = isConnected, accentColor = animatedAccent)
                }
                TileDisplayStyle.GAUGE_HUD -> {
                    CompactGaugeStyle(sensor = sensor, isConnected = isConnected, accentColor = animatedAccent)
                }
                TileDisplayStyle.GRAPH_WAVE -> {
                    CompactGraphStyle(
                        sensor = sensor,
                        isConnected = isConnected,
                        accentColor = animatedAccent,
                        timeRangeMinutes = config.graphTimeRangeMinutes,
                        onTimeRangeChange = onTimeRangeChange
                    )
                }
                TileDisplayStyle.BAR_THERMAL -> {
                    CompactThermalBarStyle(sensor = sensor, isConnected = isConnected, accentColor = animatedAccent)
                }
            }
        }
    }
}

/**
 * EXPANDED Dashboard Tile View (Takes full 4-tile space ~ 316dp)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpandedDashboardTile(
    slotIndex: Int,
    config: DashboardTileConfig,
    sensor: ObdSensor,
    isConnected: Boolean,
    onCollapse: () -> Unit,
    onOpenCustomize: () -> Unit,
    onTimeRangeChange: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accentColor = getSensorAccentColor(sensor, isConnected)
    val animatedAccent by animateColorAsState(targetValue = accentColor, label = "expanded_accent")

    val isGraphWave = config.displayStyle == TileDisplayStyle.GRAPH_WAVE
    val tileHeight = if (isGraphWave) 500.dp else 340.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(tileHeight)
            .clip(RoundedCornerShape(24.dp))
            .border(2.dp, animatedAccent.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = onCollapse,
                onLongClick = onOpenCustomize
            )
            .testTag("dashboard_tile_expanded_$slotIndex"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Icon + Title + PID Badge + Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(animatedAccent.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getSensorIcon(sensor.pid),
                            contentDescription = null,
                            tint = animatedAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = sensor.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = getSensorSubtitle(sensor, isConnected),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Action buttons: Customize
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenCustomize,
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Настроить",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Expanded Main Body by Style
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (config.displayStyle) {
                    TileDisplayStyle.DIGITAL -> {
                        ExpandedDigitalBody(sensor = sensor, isConnected = isConnected, accentColor = animatedAccent)
                    }
                    TileDisplayStyle.GAUGE_HUD -> {
                        ExpandedGaugeBody(sensor = sensor, isConnected = isConnected, accentColor = animatedAccent)
                    }
                    TileDisplayStyle.GRAPH_WAVE -> {
                        ExpandedGraphBody(
                            sensor = sensor,
                            isConnected = isConnected,
                            accentColor = animatedAccent,
                            timeRangeMinutes = config.graphTimeRangeMinutes,
                            onTimeRangeChange = onTimeRangeChange
                        )
                    }
                    TileDisplayStyle.BAR_THERMAL -> {
                        ExpandedThermalBarBody(sensor = sensor, isConnected = isConnected, accentColor = animatedAccent)
                    }
                }
            }
        }
    }
}

/* =========================================================================
   STYLE 1: DIGITAL (COMPACT & EXPANDED)
   ========================================================================= */

@Composable
private fun CompactDigitalStyle(
    sensor: ObdSensor,
    isConnected: Boolean,
    accentColor: Color
) {
    val progress = calculateProgress(sensor, isConnected)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "digital_prog")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(11.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(accentColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getSensorIcon(sensor.pid),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = sensor.name.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(accentColor, CircleShape)
            )
        }

        // Value & unit
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = if (isConnected) sensor.formattedValue else "--",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = (-1.5).sp,
                lineHeight = 48.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = sensor.unit,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        // Progress bar & Subtitle
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.15f)
            )
            Text(
                text = getSensorSubtitle(sensor, isConnected),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ExpandedDigitalBody(
    sensor: ObdSensor,
    isConnected: Boolean,
    accentColor: Color
) {
    val progress = calculateProgress(sensor, isConnected)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "exp_digital_prog")

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isConnected) sensor.formattedValue else "--",
                fontSize = 68.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = (-2.0).sp,
                lineHeight = 68.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = sensor.unit,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Large progress bar with Min/Max
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.15f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${sensor.minVal.toInt()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(progress * 100).toInt()}% шкалы",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                )
                Text(
                    text = "${sensor.maxVal.toInt()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/* =========================================================================
   STYLE 2: GAUGE HUD (COMPACT & EXPANDED)
   ========================================================================= */

@Composable
private fun CompactGaugeStyle(
    sensor: ObdSensor,
    isConnected: Boolean,
    accentColor: Color
) {
    val progress = calculateProgress(sensor, isConnected)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "gauge_prog")
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = sensor.name.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Box(
            modifier = Modifier
                .size(125.dp, 84.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 8.dp.toPx()
                val arcSize = Size(size.width - strokeWidth, size.height * 1.6f)
                val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                // Background track arc (180 to 0 degrees / 180 sweep)
                drawArc(
                    color = trackColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Active progress arc
                drawArc(
                    color = accentColor,
                    startAngle = 180f,
                    sweepAngle = 180f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = if (isConnected) sensor.formattedValue else "--",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    lineHeight = 32.sp
                )
                Text(
                    text = sensor.unit,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${sensor.minVal.toInt()}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${sensor.maxVal.toInt()}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ExpandedGaugeBody(
    sensor: ObdSensor,
    isConnected: Boolean,
    accentColor: Color
) {
    val progress = calculateProgress(sensor, isConnected)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "exp_gauge_prog")
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(260.dp, 190.dp)) {
            val strokeWidth = 14.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height * 1.6f)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            // Outer Background track
            drawArc(
                color = trackColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active Arc
            drawArc(
                color = accentColor,
                startAngle = 180f,
                sweepAngle = 180f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Graduation Tick marks
            val cx = size.width / 2
            val cy = size.height * 0.85f
            val radius = (size.width - strokeWidth) / 2 - 14.dp.toPx()

            for (i in 0..10) {
                val angleRad = Math.toRadians((180.0 + i * 18.0))
                val x1 = cx + (radius * cos(angleRad)).toFloat()
                val y1 = cy + (radius * sin(angleRad)).toFloat()
                val x2 = cx + ((radius - 8.dp.toPx()) * cos(angleRad)).toFloat()
                val y2 = cy + ((radius - 8.dp.toPx()) * sin(angleRad)).toFloat()

                drawLine(
                    color = if (i / 10f <= animatedProgress) accentColor else trackColor,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            Text(
                text = if (isConnected) sensor.formattedValue else "--",
                fontSize = 54.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                lineHeight = 54.sp
            )
            Text(
                text = sensor.unit,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
        }
    }
}

/* =========================================================================
   STYLE 3: LIVE GRAPH WAVE (COMPACT & EXPANDED)
   ========================================================================= */

@Composable
private fun CompactGraphStyle(
    sensor: ObdSensor,
    isConnected: Boolean,
    accentColor: Color,
    timeRangeMinutes: Int = 5,
    onTimeRangeChange: ((Int) -> Unit)? = null
) {
    val windowMs = (timeRangeMinutes.coerceAtLeast(1)) * 60 * 1000L
    val now = System.currentTimeMillis()
    val rawHistory = sensor.history.filter { now - it.first <= windowMs }
    val effectivePoints = if (rawHistory.size >= 2) {
        if (now - rawHistory.last().first >= 100L) {
            rawHistory + Pair(now, sensor.value)
        } else {
            rawHistory
        }
    } else if (rawHistory.size == 1) {
        listOf(Pair(rawHistory.first().first - 3000L, rawHistory.first().second), rawHistory.first())
    } else if (sensor.value != 0.0) {
        listOf(Pair(now - 3000L, sensor.value), Pair(now, sensor.value))
    } else {
        emptyList()
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // LAYER 1: Full-width live wave graph canvas across the lower portion of the tile
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Graph occupies the lower half (y from 48% of height to bottom padding)
            val chartTop = height * 0.44f
            val chartBottom = height - 8.dp.toPx()
            val chartHeight = (chartBottom - chartTop).coerceAtLeast(10f)

            // Subtle baseline guideline
            drawLine(
                color = accentColor.copy(alpha = 0.14f),
                start = Offset(0f, chartBottom),
                end = Offset(width, chartBottom),
                strokeWidth = 1.dp.toPx()
            )

            if (effectivePoints.size >= 2) {
                val (baseMin, baseMax) = getSensibleTileScale(sensor, effectivePoints)
                val totalRange = (baseMax - baseMin).coerceAtLeast(0.01)

                val oldestTime = effectivePoints.first().first
                val newestTime = effectivePoints.last().first
                val timeSpanMs = (newestTime - oldestTime).coerceAtLeast(1L)

                val renderedPts = effectivePoints.mapIndexed { index, pt ->
                    // Ensure the points stretch across the entire horizontal width (from 0 to width)
                    val xFraction = if (timeSpanMs >= 400L) {
                        ((pt.first - oldestTime).toFloat() / timeSpanMs.toFloat()).coerceIn(0f, 1f)
                    } else {
                        (index.toFloat() / (effectivePoints.size - 1).coerceAtLeast(1)).coerceIn(0f, 1f)
                    }
                    val x = xFraction * width
                    val normalized = ((pt.second - baseMin) / totalRange).toFloat().coerceIn(0.05f, 0.95f)
                    val y = chartBottom - (normalized * chartHeight)
                    Triple(x, y, pt.second)
                }

                // LAYER 1: Multi-color gradient fill underneath each segment matching its value (like in expanded tile)
                for (i in 0 until renderedPts.size - 1) {
                    val p1 = renderedPts[i]
                    val p2 = renderedPts[i + 1]
                    val c1 = getSensorPointColor(sensor.pid, p1.third, isConnected)
                    val c2 = getSensorPointColor(sensor.pid, p2.third, isConnected)
                    val segColor = if (p2.third >= p1.third) c2 else c1

                    val segFillPath = Path().apply {
                        val midX = (p1.first + p2.first) / 2f
                        moveTo(p1.first, chartBottom)
                        lineTo(p1.first, p1.second)
                        cubicTo(midX, p1.second, midX, p2.second, p2.first, p2.second)
                        lineTo(p2.first, chartBottom)
                        close()
                    }
                    drawPath(
                        path = segFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(segColor.copy(alpha = 0.35f), Color.Transparent),
                            startY = minOf(p1.second, p2.second),
                            endY = chartBottom
                        )
                    )
                }

                // LAYER 2: Multi-color smooth wave line where each section is colored by its value
                for (i in 0 until renderedPts.size - 1) {
                    val p1 = renderedPts[i]
                    val p2 = renderedPts[i + 1]
                    val c1 = getSensorPointColor(sensor.pid, p1.third, isConnected)
                    val c2 = getSensorPointColor(sensor.pid, p2.third, isConnected)

                    val segStrokePath = Path().apply {
                        val midX = (p1.first + p2.first) / 2f
                        moveTo(p1.first, p1.second)
                        cubicTo(midX, p1.second, midX, p2.second, p2.first, p2.second)
                    }

                    drawPath(
                        path = segStrokePath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(c1, c2),
                            startX = p1.first,
                            endX = p2.first
                        ),
                        style = Stroke(
                            width = 2.8.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // LAYER 3: Current value leading pulse glow marker colored by current value
                val lastPt = renderedPts.last()
                val currentPtColor = getSensorPointColor(sensor.pid, lastPt.third, isConnected)
                drawCircle(
                    color = currentPtColor.copy(alpha = 0.35f),
                    radius = 5.5.dp.toPx(),
                    center = Offset(lastPt.first, lastPt.second)
                )
                drawCircle(
                    color = currentPtColor,
                    radius = 3.dp.toPx(),
                    center = Offset(lastPt.first, lastPt.second)
                )
                drawCircle(
                    color = Color.White,
                    radius = 1.6.dp.toPx(),
                    center = Offset(lastPt.first, lastPt.second)
                )
            } else {
                // Resting telemetry wave when awaiting live stream
                val restingPath = Path()
                val waveCount = 3
                val startY = chartBottom - chartHeight * 0.4f
                restingPath.moveTo(0f, startY)
                for (i in 0..waveCount) {
                    val segW = width / waveCount
                    val x1 = i * segW + segW * 0.5f
                    val y1 = chartBottom - chartHeight * if (i % 2 == 0) 0.55f else 0.25f
                    val x2 = (i + 1) * segW
                    val y2 = chartBottom - chartHeight * 0.4f
                    restingPath.quadraticTo(x1, y1, x2, y2)
                }
                drawPath(
                    path = restingPath,
                    color = accentColor.copy(alpha = 0.20f),
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // LAYER 2: Foreground header and large value (1/3 of tile size = 48sp)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP: Icon + Title + Live Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(accentColor.copy(alpha = 0.16f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getSensorIcon(sensor.pid),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = sensor.name.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (isConnected) StatusGreen else accentColor.copy(alpha = 0.35f),
                            CircleShape
                        )
                )
            }

            // MIDDLE-TOP: Giant prominent Value (1/3 of 152dp tile = 48sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (isConnected) sensor.formattedValue else "--",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-1.5).sp,
                    lineHeight = 48.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = sensor.unit,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            // BOTTOM: Spacer to keep room for the wave graph in the lower half
            Spacer(modifier = Modifier.height(26.dp))
        }
    }
}

private fun getSensibleTileScale(sensor: ObdSensor, points: List<Pair<Long, Double>>): Pair<Double, Double> {
    val minObserved = if (points.isNotEmpty()) points.minOf { it.second } else sensor.value
    val maxObserved = if (points.isNotEmpty()) points.maxOf { it.second } else sensor.value
    val span = maxObserved - minObserved

    // Adaptive padding so small changes produce an active visible wave, not a flat stripe
    val defaultPadding = when (sensor.pid) {
        "010C" -> 200.0          // RPM: ±200 around idle / rev
        "010D" -> 15.0           // Speed: ±15 km/h
        "0105", "015C", "0146" -> 4.0 // Temp: ±4°C
        "0142" -> 0.35           // Voltage: ±0.35V
        "0111", "012F", "0104" -> 10.0 // Percentage: ±10%
        else -> maxOf(2.0, (minObserved + maxObserved) * 0.08)
    }

    val margin = if (span < 0.001) defaultPadding else maxOf(span * 0.20, defaultPadding * 0.5)
    val baseMin = (minObserved - margin).coerceAtLeast(sensor.minVal)
    val baseMax = (maxObserved + margin).coerceAtMost(sensor.maxVal).coerceAtLeast(baseMin + 0.1)

    return Pair(baseMin, baseMax)
}

private data class AxisGridTick(
    val value: Double,
    val formattedText: String
)

private data class AxisScaleInfo(
    val baseMin: Double,
    val baseMax: Double,
    val totalRange: Double,
    val ticks: List<AxisGridTick>
)

private fun calculateNiceAxisScale(
    minVal: Double,
    maxVal: Double,
    pid: String,
    unit: String
): AxisScaleInfo {
    val scale = calculateAdequateAxisScale(minVal, maxVal, pid, unit, targetTickCount = 4)
    val cleanUnit = if (unit.isNotBlank()) " $unit" else ""
    return AxisScaleInfo(
        baseMin = scale.baseMin,
        baseMax = scale.baseMax,
        totalRange = scale.totalRange,
        ticks = scale.ticks.map { AxisGridTick(it.value, "${it.formattedText}$cleanUnit") }
    )
}

@Composable
private fun ExpandedGraphBody(
    sensor: ObdSensor,
    isConnected: Boolean,
    accentColor: Color,
    timeRangeMinutes: Int = 5,
    onTimeRangeChange: ((Int) -> Unit)? = null
) {
    val windowMs = (timeRangeMinutes.coerceAtLeast(1)) * 60 * 1000L
    val now = System.currentTimeMillis()
    val currentPt = Pair(now, sensor.value)
    val rawHistory = sensor.history.filter { now - it.first <= windowMs }
    val effectivePoints = if (rawHistory.isEmpty()) {
        listOf(Pair(now - 3000L, sensor.value), currentPt)
    } else if (now - rawHistory.last().first >= 100L) {
        rawHistory + currentPt
    } else {
        rawHistory
    }

    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textMeasurer = rememberTextMeasurer()
    val axisTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.90f)
    val axisTextStyle = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Monospace,
        color = axisTextColor
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // LAYER 1: Full-background expansive graph canvas with grid lines and value axis labels
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val paddingPx = 20.dp.toPx()

            // Dynamic scaling starting from the first and actual sensor values (not zero)
            val minV = if (effectivePoints.isNotEmpty()) effectivePoints.minOf { it.second } else sensor.minVal
            val maxV = if (effectivePoints.isNotEmpty()) effectivePoints.maxOf { it.second } else sensor.maxVal

            val axisScale = calculateNiceAxisScale(minV, maxV, sensor.pid, sensor.unit)
            val baseMin = axisScale.baseMin
            val totalRange = axisScale.totalRange

            // Draw horizontal grid lines and nice rounded tick labels
            axisScale.ticks.forEach { tick ->
                val normalized = ((tick.value - baseMin) / totalRange).toFloat().coerceIn(0f, 1f)
                val y = (height - (normalized * (height - paddingPx * 2) + paddingPx)).toFloat()

                // Horizontal dashed grid line
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )

                val labelText = tick.formattedText

                // Draw Y-axis value label chip on the left side
                val textLayout = textMeasurer.measure(
                    text = labelText,
                    style = axisTextStyle
                )
                val chipPadH = 6.dp.toPx()
                val chipPadV = 2.dp.toPx()
                val chipW = textLayout.size.width + chipPadH * 2
                val chipH = textLayout.size.height + chipPadV * 2
                val chipX = 8.dp.toPx()
                val chipY = (y - chipH / 2f).coerceIn(4.dp.toPx(), height - chipH - 4.dp.toPx())

                drawRoundRect(
                    color = surfaceColor.copy(alpha = 0.85f),
                    topLeft = Offset(chipX, chipY),
                    size = Size(chipW, chipH),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
                drawRoundRect(
                    color = gridColor,
                    topLeft = Offset(chipX, chipY),
                    size = Size(chipW, chipH),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                    style = Stroke(width = 0.8.dp.toPx())
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = labelText,
                    topLeft = Offset(chipX + chipPadH, chipY + chipPadV),
                    style = axisTextStyle
                )
            }

            if (effectivePoints.size >= 2) {
                val oldestTime = effectivePoints.first().first
                val newestTime = effectivePoints.last().first
                val timeSpanMs = (newestTime - oldestTime).coerceAtLeast(1L)

                val pts = effectivePoints.mapIndexed { index, pt ->
                    val xFraction = if (timeSpanMs >= 400L) {
                        ((pt.first - oldestTime).toFloat() / timeSpanMs.toFloat()).coerceIn(0f, 1f)
                    } else {
                        (index.toFloat() / (effectivePoints.size - 1).coerceAtLeast(1)).coerceIn(0f, 1f)
                    }
                    val x = xFraction * width
                    val normalized = ((pt.second - baseMin) / totalRange).toFloat().coerceIn(0.04f, 0.96f)
                    val y = (height - (normalized * (height - paddingPx * 2) + paddingPx)).toFloat()
                    Offset(x, y)
                }

                val renderedPts = effectivePoints.mapIndexed { index, pt ->
                    val xFraction = if (timeSpanMs >= 400L) {
                        ((pt.first - oldestTime).toFloat() / timeSpanMs.toFloat()).coerceIn(0f, 1f)
                    } else {
                        (index.toFloat() / (effectivePoints.size - 1).coerceAtLeast(1)).coerceIn(0f, 1f)
                    }
                    val x = xFraction * width
                    val normalized = ((pt.second - baseMin) / totalRange).toFloat().coerceIn(0.04f, 0.96f)
                    val y = (height - (normalized * (height - paddingPx * 2) + paddingPx)).toFloat()
                    Triple(x, y, pt.second)
                }

                // LAYER 1: Multi-color gradient fill underneath each segment matching its value
                for (i in 0 until renderedPts.size - 1) {
                    val p1 = renderedPts[i]
                    val p2 = renderedPts[i + 1]
                    val c1 = getSensorPointColor(sensor.pid, p1.third, isConnected)
                    val c2 = getSensorPointColor(sensor.pid, p2.third, isConnected)
                    val segColor = if (p2.third >= p1.third) c2 else c1

                    val segFillPath = Path().apply {
                        moveTo(p1.first, height)
                        lineTo(p1.first, p1.second)
                        lineTo(p2.first, p2.second)
                        lineTo(p2.first, height)
                        close()
                    }
                    drawPath(
                        path = segFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(segColor.copy(alpha = 0.32f), Color.Transparent),
                            startY = minOf(p1.second, p2.second),
                            endY = height
                        )
                    )
                }

                // Current level guide line
                val lastPt = renderedPts.last()
                val currentPtColor = getSensorPointColor(sensor.pid, lastPt.third, isConnected)
                drawLine(
                    color = currentPtColor.copy(alpha = 0.45f),
                    start = Offset(0f, lastPt.second),
                    end = Offset(width, lastPt.second),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                )

                // LAYER 2: Big glowing multi-color wave line where each section is colored by its value
                for (i in 0 until renderedPts.size - 1) {
                    val p1 = renderedPts[i]
                    val p2 = renderedPts[i + 1]
                    val c1 = getSensorPointColor(sensor.pid, p1.third, isConnected)
                    val c2 = getSensorPointColor(sensor.pid, p2.third, isConnected)

                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(c1, c2),
                            startX = p1.first,
                            endX = p2.first
                        ),
                        start = Offset(p1.first, p1.second),
                        end = Offset(p2.first, p2.second),
                        strokeWidth = 3.6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Latest point indicator
                drawCircle(currentPtColor, 5.5.dp.toPx(), Offset(lastPt.first, lastPt.second))
                drawCircle(Color.White, 2.8.dp.toPx(), Offset(lastPt.first, lastPt.second))
            } else {
                // Baseline placeholder if no points yet
                drawLine(
                    color = accentColor.copy(alpha = 0.25f),
                    start = Offset(0f, height * 0.7f),
                    end = Offset(width, height * 0.7f),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        // LAYER 2: Foreground overlays (Top right stats, Bottom left huge number & unit)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Right: Live status & Min / Max stats badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isConnected) StatusGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (isConnected) "● LIVE" else "○ ОФФЛАЙН",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isConnected) StatusGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (effectivePoints.isNotEmpty()) {
                        val minV = effectivePoints.minOf { it.second }
                        val maxV = effectivePoints.maxOf { it.second }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        ) {
                            Text(
                                "▼ ${String.format(java.util.Locale.US, "%.1f", minV)}   ▲ ${String.format(java.util.Locale.US, "%.1f", maxV)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Left: Huge Value & Unit Display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = if (isConnected) sensor.formattedValue else "--",
                    fontSize = 92.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    lineHeight = 92.sp,
                    letterSpacing = (-2.0).sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = sensor.unit,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
    }
}

/* =========================================================================
   STYLE 4: THERMAL / RANGE BAR (COMPACT & EXPANDED)
   ========================================================================= */

@Composable
private fun CompactThermalBarStyle(
    sensor: ObdSensor,
    isConnected: Boolean,
    accentColor: Color
) {
    val progress = calculateProgress(sensor, isConnected)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "thermal_prog")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(11.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(accentColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Thermostat,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = sensor.name.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = accentColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = if (isConnected) "${sensor.formattedValue}${sensor.unit}" else "--",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }

        // Central Value (1/3 of tile height)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = if (isConnected) sensor.formattedValue else "--",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = (-1.5).sp,
                lineHeight = 48.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = sensor.unit,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        // Segmented Multi-Color Bar
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                AutomotiveBlue,   // Прогрев / Холод
                                StatusGreen,      // Норма
                                WarningAmber,     // Нагрев
                                ClearRed          // Перегрев
                            )
                        )
                    )
            ) {
                // Pointer indicator
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .align(Alignment.CenterStart)
                        .padding(start = (animatedProgress * 120).dp.coerceIn(0.dp, 130.dp))
                        .background(Color.White, RoundedCornerShape(2.dp))
                        .border(1.dp, Color.Black, RoundedCornerShape(2.dp))
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Холод", fontSize = 9.sp, color = AutomotiveBlue, fontWeight = FontWeight.Bold)
                Text("Норма", fontSize = 9.sp, color = StatusGreen, fontWeight = FontWeight.Bold)
                Text("Перегрев", fontSize = 9.sp, color = ClearRed, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ExpandedThermalBarBody(
    sensor: ObdSensor,
    isConnected: Boolean,
    accentColor: Color
) {
    val progress = calculateProgress(sensor, isConnected)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "exp_thermal_prog")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isConnected) sensor.formattedValue else "--",
                fontSize = 62.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                lineHeight = 62.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = sensor.unit,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = accentColor.copy(alpha = 0.15f)
        ) {
            Text(
                text = getSensorSubtitle(sensor, isConnected),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Large Multi-Color Temperature / Level Bar
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                AutomotiveBlue,   // Прогрев / Холод (0 - 70°C)
                                StatusGreen,      // Оптимум (70 - 95°C)
                                WarningAmber,     // Внимание (95 - 103°C)
                                ClearRed          // Перегрев (> 103°C)
                            )
                        )
                    )
            ) {
                // Marker Needle
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(6.dp)
                        .align(Alignment.CenterStart)
                        .padding(start = (animatedProgress * 280).dp.coerceIn(0.dp, 290.dp))
                        .background(Color.White, RoundedCornerShape(3.dp))
                        .border(1.dp, Color.Black.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("🔵 Синяя зона (Прогрев)", fontSize = 11.sp, color = AutomotiveBlue, fontWeight = FontWeight.Bold)
                Text("🟢 Норма", fontSize = 11.sp, color = StatusGreen, fontWeight = FontWeight.Bold)
                Text("🔴 Перегрев", fontSize = 11.sp, color = ClearRed, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/* =========================================================================
   CUSTOMIZATION DIALOG (LONG PRESS)
   ========================================================================= */

@Composable
fun CustomizeDashboardTileDialog(
    slotIndex: Int,
    currentConfig: DashboardTileConfig,
    allSensors: List<ObdSensor>,
    onDismiss: () -> Unit,
    onSave: (newPid: String, newStyle: TileDisplayStyle, newTimeRange: Int) -> Unit
) {
    var selectedPid by remember(currentConfig.sensorPid) { mutableStateOf(currentConfig.sensorPid) }
    var selectedStyle by remember(currentConfig.displayStyle) { mutableStateOf(currentConfig.displayStyle) }
    var selectedTimeRange by remember(currentConfig.graphTimeRangeMinutes) { mutableStateOf(currentConfig.graphTimeRangeMinutes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Настройка прибора #${slotIndex + 1}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(StatusGreen, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Сохраняется автоматически",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StatusGreen
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                // SECTION 1: STYLE SELECTION (LARGE VISUAL PREVIEWS)
                Text(
                    text = "ВИД ОТОБРАЖЕНИЯ ПРИБОРА:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StyleChoiceCard(
                        title = "Цифровой",
                        styleType = TileDisplayStyle.DIGITAL,
                        isSelected = selectedStyle == TileDisplayStyle.DIGITAL,
                        onClick = {
                            selectedStyle = TileDisplayStyle.DIGITAL
                            onSave(selectedPid, TileDisplayStyle.DIGITAL, selectedTimeRange)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    StyleChoiceCard(
                        title = "HUD Шкала",
                        styleType = TileDisplayStyle.GAUGE_HUD,
                        isSelected = selectedStyle == TileDisplayStyle.GAUGE_HUD,
                        onClick = {
                            selectedStyle = TileDisplayStyle.GAUGE_HUD
                            onSave(selectedPid, TileDisplayStyle.GAUGE_HUD, selectedTimeRange)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StyleChoiceCard(
                        title = "График",
                        styleType = TileDisplayStyle.GRAPH_WAVE,
                        isSelected = selectedStyle == TileDisplayStyle.GRAPH_WAVE,
                        onClick = {
                            selectedStyle = TileDisplayStyle.GRAPH_WAVE
                            onSave(selectedPid, TileDisplayStyle.GRAPH_WAVE, selectedTimeRange)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    StyleChoiceCard(
                        title = "Термометр/Бар",
                        styleType = TileDisplayStyle.BAR_THERMAL,
                        isSelected = selectedStyle == TileDisplayStyle.BAR_THERMAL,
                        onClick = {
                            selectedStyle = TileDisplayStyle.BAR_THERMAL
                            onSave(selectedPid, TileDisplayStyle.BAR_THERMAL, selectedTimeRange)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // TIME RANGE SELECTION (Visible if GRAPH_WAVE selected - Dropdown Menu)
                AnimatedVisibility(visible = selectedStyle == TileDisplayStyle.GRAPH_WAVE) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "ДИАПАЗОН ВРЕМЕНИ ГРАФИКА:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        var showDialogTimeMenu by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                onClick = { showDialogTimeMenu = true },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().height(42.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = when (selectedTimeRange) {
                                                1 -> "1 минута"
                                                3 -> "3 минуты"
                                                5 -> "5 минут (по умолчанию)"
                                                else -> "$selectedTimeRange минут"
                                            },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Выпадающий список",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showDialogTimeMenu,
                                onDismissRequest = { showDialogTimeMenu = false },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                val ranges = listOf(
                                    1 to "1 минута",
                                    3 to "3 минуты",
                                    5 to "5 минут (по умолчанию)",
                                    10 to "10 минут",
                                    15 to "15 минут",
                                    30 to "30 минут"
                                )
                                ranges.forEach { (mins, label) ->
                                    val isSel = mins == selectedTimeRange
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Normal,
                                                    color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (isSel) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedTimeRange = mins
                                            showDialogTimeMenu = false
                                            onSave(selectedPid, selectedStyle, mins)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                // SECTION 2: SENSOR SELECTION
                Text(
                    text = "ДАТЧИК ПРИБОРА:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(allSensors) { sensor ->
                        val isSelected = sensor.pid == selectedPid
                        Card(
                            onClick = {
                                selectedPid = sensor.pid
                                onSave(sensor.pid, selectedStyle, selectedTimeRange)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = getSensorIcon(sensor.pid),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = sensor.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = sensor.category,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = "${sensor.formattedValue} ${sensor.unit}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
fun CustomizeDashboardTileDialog(
    slotIndex: Int,
    currentConfig: DashboardTileConfig,
    allSensors: List<ObdSensor>,
    onDismiss: () -> Unit,
    onSave: (newPid: String, newStyle: TileDisplayStyle) -> Unit
) {
    CustomizeDashboardTileDialog(
        slotIndex = slotIndex,
        currentConfig = currentConfig,
        allSensors = allSensors,
        onDismiss = onDismiss,
        onSave = { pid, style, _ -> onSave(pid, style) }
    )
}

@Composable
private fun StyleChoiceCard(
    title: String,
    styleType: TileDisplayStyle,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        modifier = modifier.height(100.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Large visual preview graphic
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    when (styleType) {
                        TileDisplayStyle.DIGITAL -> DigitalStyleMiniPreview(isSelected)
                        TileDisplayStyle.GAUGE_HUD -> GaugeHudStyleMiniPreview(isSelected)
                        TileDisplayStyle.GRAPH_WAVE -> GraphWaveStyleMiniPreview(isSelected)
                        TileDisplayStyle.BAR_THERMAL -> BarThermalStyleMiniPreview(isSelected)
                    }
                }

                // Title label
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }

            // Checkmark indicator badge when selected
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(18.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DigitalStyleMiniPreview(isSelected: Boolean) {
    val accent = if (isSelected) MaterialTheme.colorScheme.primary else StatusGreen
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = "120",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = accent
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "км/ч",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 2.dp)
        )
    }
}

@Composable
private fun GaugeHudStyleMiniPreview(isSelected: Boolean) {
    val primaryColor = if (isSelected) MaterialTheme.colorScheme.primary else AutomotiveBlue
    val endColor = WarningAmber
    Canvas(modifier = Modifier.size(44.dp)) {
        val strokeWidth = 3.5.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2f
        val center = Offset(size.width / 2f, size.height / 2f + 2.dp.toPx())

        // Track arc
        drawArc(
            color = Color.Gray.copy(alpha = 0.25f),
            startAngle = 140f,
            sweepAngle = 260f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Value arc
        drawArc(
            brush = Brush.sweepGradient(
                listOf(primaryColor, endColor),
                center = center
            ),
            startAngle = 140f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Center needle / dot
        drawCircle(
            color = primaryColor,
            radius = 3.dp.toPx(),
            center = center
        )
        val needleAngleRad = Math.toRadians((140 + 180).toDouble())
        val needleEnd = Offset(
            (center.x + (radius - 2.dp.toPx()) * Math.cos(needleAngleRad)).toFloat(),
            (center.y + (radius - 2.dp.toPx()) * Math.sin(needleAngleRad)).toFloat()
        )
        drawLine(
            color = Color.White,
            start = center,
            end = needleEnd,
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun GraphWaveStyleMiniPreview(isSelected: Boolean) {
    val lineCol = if (isSelected) MaterialTheme.colorScheme.primary else AutomotiveBlue
    Canvas(modifier = Modifier.fillMaxWidth().height(36.dp).padding(horizontal = 6.dp)) {
        val w = size.width
        val h = size.height
        val gridCol = Color.Gray.copy(alpha = 0.25f)

        // Grid lines
        drawLine(
            color = gridCol,
            start = Offset(0f, h * 0.3f),
            end = Offset(w, h * 0.3f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
        )
        drawLine(
            color = gridCol,
            start = Offset(0f, h * 0.7f),
            end = Offset(w, h * 0.7f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
        )

        // Wave path
        val path = Path()
        val fillPath = Path()
        val points = listOf(
            Offset(0f, h * 0.8f),
            Offset(w * 0.25f, h * 0.4f),
            Offset(w * 0.5f, h * 0.7f),
            Offset(w * 0.75f, h * 0.2f),
            Offset(w, h * 0.35f)
        )
        path.moveTo(points[0].x, points[0].y)
        fillPath.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val cx = (prev.x + curr.x) / 2f
            path.cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
            fillPath.cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
        }
        fillPath.lineTo(w, h)
        fillPath.lineTo(0f, h)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                listOf(lineCol.copy(alpha = 0.35f), Color.Transparent)
            )
        )
        drawPath(
            path = path,
            color = lineCol,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun BarThermalStyleMiniPreview(isSelected: Boolean) {
    Column(
        modifier = Modifier.padding(horizontal = 6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val colors = listOf(
            StatusGreen,
            StatusGreen,
            WarningAmber,
            ClearRed
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.fillMaxWidth().height(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            colors.forEachIndexed { index, color ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (index < 3) color else color.copy(alpha = 0.2f))
                )
            }
        }
        Text(
            text = "90 °C",
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isSelected) MaterialTheme.colorScheme.primary else StatusGreen
        )
    }
}

/* =========================================================================
   HELPER UTILITIES
   ========================================================================= */

private fun calculateProgress(sensor: ObdSensor, isConnected: Boolean): Float {
    if (!isConnected) return 0f
    val range = (sensor.maxVal - sensor.minVal)
    if (range <= 0.0) return 0f
    return ((sensor.value - sensor.minVal) / range).toFloat().coerceIn(0f, 1f)
}

private fun getSensorAccentColor(sensor: ObdSensor, isConnected: Boolean): Color {
    if (!isConnected) return Color.Gray

    return when (sensor.pid.uppercase()) {
        "010D" -> if (sensor.value > 130) WarningAmber else AutomotiveBlue
        "010C" -> when {
            sensor.value >= 5500 -> ClearRed
            sensor.value >= 3500 -> WarningAmber
            else -> StatusGreen
        }
        "0105" -> when {
            sensor.value > 103 -> ClearRed
            sensor.value > 96 -> WarningAmber
            sensor.value >= 75 -> StatusGreen
            else -> AutomotiveBlue
        }
        "0142" -> when {
            sensor.value < 11.9 -> ClearRed
            sensor.value in 13.5..14.8 -> StatusGreen
            else -> WarningAmber
        }
        "012F" -> when {
            sensor.value < 15.0 -> ClearRed
            sensor.value < 30.0 -> WarningAmber
            else -> StatusGreen
        }
        else -> AutomotiveBlue
    }
}

private fun getSensorSubtitle(sensor: ObdSensor, isConnected: Boolean): String {
    if (!isConnected) return "Нет связи"

    return when (sensor.pid.uppercase()) {
        "010D" -> if (sensor.value > 0) "В движении" else "Остановка"
        "010C" -> when {
            sensor.value == 0.0 -> "Двигатель заглушен"
            sensor.value < 1000 -> "Холостой ход"
            sensor.value >= 5500 -> "⚠️ Высокие обороты"
            else -> "Рабочие обороты"
        }
        "0105" -> when {
            sensor.value > 103 -> "⚠️ ПЕРЕГРЕВ!"
            sensor.value > 96 -> "Повышенный нагрев"
            sensor.value >= 75 -> "Рабочая норма"
            else -> "Прогрев мотора"
        }
        "0142" -> when {
            sensor.value in 13.5..14.8 -> "Генератор в норме"
            sensor.value < 11.9 -> "⚠️ Низкий заряд АКБ"
            else -> "Зажигание / АКБ"
        }
        "012F" -> when {
            sensor.value < 15.0 -> "⚠️ Мало топлива"
            sensor.value < 35.0 -> "Остаток 1/4 бака"
            else -> "Уровень в норме"
        }
        else -> "Параметр в норме"
    }
}

private fun getSensorIcon(pid: String): ImageVector {
    return when (pid.uppercase()) {
        "010D" -> Icons.Default.Speed
        "010C" -> Icons.Default.Timeline
        "0105", "010F" -> Icons.Default.Thermostat
        "0142" -> Icons.Default.ElectricBolt
        "012F" -> Icons.Default.LocalGasStation
        "0110" -> Icons.Default.Air
        "0104", "0111" -> Icons.Default.Equalizer
        else -> Icons.Default.Sensors
    }
}

private fun fallbackSensorForPid(pid: String): ObdSensor {
    return when (pid.uppercase()) {
        "010D" -> ObdSensor("010D", "Скорость", 0.0, "км/ч", 0.0, 260.0, "Движение")
        "010C" -> ObdSensor("010C", "Обороты (RPM)", 0.0, "об/мин", 0.0, 8000.0, "Двигатель")
        "0105" -> ObdSensor("0105", "Температура ОЖ", 0.0, "°C", -40.0, 140.0, "Температура")
        "0142" -> ObdSensor("0142", "Напряжение АКБ", 0.0, "В", 9.0, 16.0, "Электрика")
        "012F" -> ObdSensor("012F", "Уровень топлива", 0.0, "%", 0.0, 100.0, "Топливо")
        else -> ObdSensor(pid, "Датчик $pid", 0.0, "", 0.0, 100.0, "Датчики")
    }
}
