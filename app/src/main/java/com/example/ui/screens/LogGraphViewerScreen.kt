package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.logging.LogSensorSeries
import com.example.ui.MainViewModel
import com.example.ui.components.calculateAdequateAxisScale
import com.example.ui.components.getSensorPointColor
import com.example.ui.theme.AutomotiveBlue
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.StatusGreen
import java.util.Locale

enum class ChartTouchMode {
    GESTURE_ZOOM_PAN,
    SCRUBBER_INSPECT
}

private fun Float.safeCoerceIn(min: Float, max: Float): Float {
    if (this.isNaN()) return min
    return if (max <= min) min else this.coerceIn(min, max)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogGraphViewerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateAiChat: () -> Unit = {}
) {
    val activeSession by viewModel.selectedLogSession.collectAsState()

    if (activeSession == null || activeSession?.seriesList.isNullOrEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090D14))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = AutomotiveBlue,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Лог-файл не выбран или пуст",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Выберите сохраненный CSV файл из журнала для просмотра интерактивного графика.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = AutomotiveBlue)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Вернуться к списку логов", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    val session = activeSession!!

    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text("Удалить этот лог?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Файл «${session.fileName}» будет удален. Вы вернетесь к журналу поездок.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.deleteCurrentLog()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // ПРИ ОТКРЫТИИ ЛОГА: СКРЫВАЕМ ДАТЧИКИ, КОТОРЫЕ НЕ ПИСАЛИСЬ
    // Оставляем только те датчики, у которых есть реальные записанные точки
    val recordedSeries: List<LogSensorSeries> = remember(session) {
        val filtered = session.seriesList.filter { series ->
            series.points.isNotEmpty() && (
                series.points.any { it.value != 0.0 } ||
                (series.maxValue > series.minValue) ||
                series.sensorName.contains("скорост", ignoreCase = true) ||
                series.sensorName.contains("дроссел", ignoreCase = true)
            )
        }
        if (filtered.isNotEmpty()) filtered else session.seriesList
    }

    // По умолчанию выбираем первые 2 записанных датчика или все если их мало
    val selectedSensorIndices = remember(recordedSeries) {
        mutableStateListOf<Int>().apply {
            if (recordedSeries.isNotEmpty()) {
                add(0)
                if (recordedSeries.size > 1) add(1)
            }
        }
    }

    // 2-Axis Zoom & Pan State (Масштабирование руками по 2м осям)
    var zoomX by remember { mutableFloatStateOf(1.0f) } // Ось X: время (1.0x to 30.0x)
    var scrollOffsetRatioX by remember { mutableFloatStateOf(0.0f) } // Панорамирование X
    var zoomY by remember { mutableFloatStateOf(1.0f) } // Ось Y: амплитуда (0.5x to 15.0x)
    var panOffsetY by remember { mutableFloatStateOf(0.0f) } // Вертикальный сдвиг Y

    // Режим взаимодействия: 2-осевые жесты (Pinch & Pan) против Визира (Scrubber point inspection)
    var touchMode by remember { mutableStateOf(ChartTouchMode.GESTURE_ZOOM_PAN) }

    // Визир (Scrubber)
    var scrubberFraction by remember { mutableStateOf<Float?>(null) }

    val totalPointsCount = recordedSeries.maxOfOrNull { it.points.size } ?: 0
    val safeTotal = totalPointsCount.coerceAtLeast(1)

    // Расчет видимого окна данных с учетом зума X и панорамирования X
    val visiblePointsCount = (safeTotal / zoomX).toInt().coerceIn(1, safeTotal)
    val maxStart = (safeTotal - visiblePointsCount).coerceAtLeast(0)
    val startIndex = (scrollOffsetRatioX * maxStart).toInt().coerceIn(0, maxStart)
    val endIndex = (startIndex + visiblePointsCount).coerceIn(startIndex + 1, safeTotal)
    val sliceSize = (endIndex - startIndex).coerceAtLeast(1)

    val activeSensors = selectedSensorIndices.filter { it in recordedSeries.indices }.map { recordedSeries[it] }.ifEmpty {
        if (recordedSeries.isNotEmpty()) listOf(recordedSeries.first()) else emptyList()
    }

    // Расчет точки под визиром
    val currentScrubIndex = scrubberFraction?.let { frac ->
        (startIndex + (frac * (sliceSize - 1)).toInt()).coerceIn(0, (safeTotal - 1).coerceAtLeast(0))
    }

    val firstPt = recordedSeries.firstOrNull()?.points?.getOrNull(startIndex)
    val lastPt = recordedSeries.firstOrNull()?.points?.getOrNull((endIndex - 1).coerceAtLeast(0))
    val timeRangeLabel = if (firstPt != null && lastPt != null) "${firstPt.timeLabel} — ${lastPt.timeLabel}" else ""

    // БОЛЬШОЙ ГРАФИК НА ВЕСЬ ЭКРАН (Column с weight(1f), убрано все лишнее)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B12))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        // 1. КОМПАКТНАЯ ВЕРХНЯЯ СТРОКА (НАЗАД, ИМЯ ЛОГА, ЗУМ, СБРОС 1X, ВИЗИР/МАСШТАБ, ПОДЕЛИТЬСЯ)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.fileName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = "Записано: ${session.totalRecords} точек • ${session.totalDurationSeconds} сек",
                    fontSize = 11.sp,
                    color = Color(0xFF90A4AE)
                )
            }

            // Индикатор текущего масштаба по двум осям
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF141E2E),
                border = androidx.compose.foundation.BorderStroke(1.dp, AutomotiveBlue.copy(alpha = 0.4f)),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = "X:${String.format(Locale.US, "%.1f", zoomX)}x | Y:${String.format(Locale.US, "%.1f", zoomY)}x",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = AutomotiveBlue,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }

            // Переключатель режима: Масштаб жестами vs Визир
            Surface(
                onClick = {
                    touchMode = if (touchMode == ChartTouchMode.GESTURE_ZOOM_PAN) {
                        ChartTouchMode.SCRUBBER_INSPECT
                    } else {
                        ChartTouchMode.GESTURE_ZOOM_PAN
                    }
                },
                shape = RoundedCornerShape(8.dp),
                color = if (touchMode == ChartTouchMode.GESTURE_ZOOM_PAN) AutomotiveBlue else StatusGreen,
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (touchMode == ChartTouchMode.GESTURE_ZOOM_PAN) Icons.Default.PanTool else Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (touchMode == ChartTouchMode.GESTURE_ZOOM_PAN) "Жесты" else "Визир",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Кнопка быстрого сброса масштаба (1x)
            IconButton(
                onClick = {
                    zoomX = 1.0f
                    zoomY = 1.0f
                    scrollOffsetRatioX = 0.0f
                    panOffsetY = 0.0f
                    scrubberFraction = null
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Сброс масштаба 1x",
                    tint = AutomotiveBlue
                )
            }

            IconButton(
                onClick = { showDeleteConfirmation = true },
                modifier = Modifier.size(36.dp).testTag("delete_current_log_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Удалить лог",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 2. ИНТЕРАКТИВНЫЙ ГРАФИК
        val textMeasurer = rememberTextMeasurer()
        val axisLabelStyle = TextStyle(
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = Color.White.copy(alpha = 0.85f)
        )
        val axisTitleStyle = TextStyle(
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AutomotiveBlue
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.15f)
                .background(Color(0xFF090D14), RoundedCornerShape(14.dp))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                .testTag("big_interactive_log_chart")
                // Масштабирование руками по 2м осям (pinch-to-zoom и pan) непосредственно на холсте!
                .pointerInput(touchMode) {
                    if (touchMode == ChartTouchMode.GESTURE_ZOOM_PAN) {
                        detectTransformGestures(panZoomLock = false) { _, pan, zoom, _ ->
                            if (zoom != 1.0f) {
                                zoomX = (zoomX * zoom).coerceIn(1.0f, 30.0f)
                                zoomY = (zoomY * zoom).coerceIn(0.5f, 15.0f)
                            }
                            val w = size.width.takeIf { it > 10 } ?: 1000
                            val h = size.height.takeIf { it > 10 } ?: 1000
                            if (pan.x != 0f) {
                                val panDeltaX = pan.x / (w * zoomX.coerceAtLeast(1.0f))
                                scrollOffsetRatioX = (scrollOffsetRatioX - panDeltaX).coerceIn(0.0f, 1.0f)
                            }
                            if (pan.y != 0f) {
                                val panDeltaY = pan.y / (h * zoomY.coerceAtLeast(1.0f))
                                panOffsetY = (panOffsetY - panDeltaY).coerceIn(-1.0f, 1.0f)
                            }
                        }
                    } else {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val chartLeft = 52.dp.toPx()
                                val chartRight = size.width - 12.dp.toPx()
                                val chartW = (chartRight - chartLeft).coerceAtLeast(1f)
                                scrubberFraction = ((offset.x - chartLeft) / chartW).coerceIn(0f, 1f)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val chartLeft = 52.dp.toPx()
                                val chartRight = size.width - 12.dp.toPx()
                                val chartW = (chartRight - chartLeft).coerceAtLeast(1f)
                                scrubberFraction = ((change.position.x - chartLeft) / chartW).coerceIn(0f, 1f)
                            }
                        )
                    }
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                if (canvasWidth <= 30f || canvasHeight <= 30f) return@Canvas

                val primarySeries = activeSensors.firstOrNull()
                val primaryScale = primarySeries?.let {
                    calculateAdequateAxisScale(
                        minVal = it.minValue,
                        maxVal = it.maxValue,
                        pid = "",
                        unit = it.unit,
                        targetTickCount = 5
                    )
                }

                // Отступы для подписанных осей
                val leftAxisWidth = 52.dp.toPx()
                val bottomAxisHeight = 28.dp.toPx()
                val topPad = 22.dp.toPx()
                val rightPad = 14.dp.toPx()

                val chartLeft = leftAxisWidth
                val chartRight = (canvasWidth - rightPad).coerceAtLeast(chartLeft + 10f)
                val chartTop = topPad
                val chartBottom = (canvasHeight - bottomAxisHeight).coerceAtLeast(chartTop + 10f)
                val chartWidth = (chartRight - chartLeft).coerceAtLeast(10f)
                val chartHeight = (chartBottom - chartTop).coerceAtLeast(10f)

                // 1. РАСЧЕТ И ОТРИСОВКА ВЕРТИКАЛЬНОЙ ОСИ Y С ЗУМОМ Y И СДВИГОМ Y
                val baseMin = primaryScale?.baseMin ?: 0.0
                val baseTotalRange = (primaryScale?.totalRange ?: 100.0).coerceAtLeast(1.0)
                val effTotalRange = (baseTotalRange / zoomY).coerceAtLeast(0.001)
                val centerY = baseMin + (baseTotalRange / 2.0) - (panOffsetY * (baseTotalRange * 0.5))
                val effMinY = centerY - (effTotalRange / 2.0)
                val effMaxY = centerY + (effTotalRange / 2.0)

                // Подпись Оси Y (Имя датчика и единица)
                val yTitle = if (primarySeries != null) {
                    if (activeSensors.size == 1) {
                        "Ось Y: ${primarySeries.sensorName} [${primarySeries.unit}]"
                    } else {
                        "Ось Y: ${primarySeries.sensorName} [${primarySeries.unit}] (+${activeSensors.size - 1})"
                    }
                } else {
                    "Ось Y: Значения"
                }
                val yTitleLayout = textMeasurer.measure(yTitle, axisTitleStyle)
                drawText(
                    textLayoutResult = yTitleLayout,
                    topLeft = Offset(chartLeft, 2.dp.toPx())
                )

                // Сетка и засечки Оси Y
                val yTickCount = 5
                for (t in 0 until yTickCount) {
                    val tickFrac = t.toFloat() / (yTickCount - 1).coerceAtLeast(1)
                    val tickVal = effMinY + (tickFrac * effTotalRange)
                    val y = chartBottom - (tickFrac * chartHeight)

                    val tickStr = if (Math.abs(tickVal) < 10.0) {
                        String.format(Locale.US, "%.1f", tickVal)
                    } else {
                        String.format(Locale.US, "%.0f", tickVal)
                    }
                    val textLayout = textMeasurer.measure(tickStr, axisLabelStyle)
                    val labelX = (chartLeft - textLayout.size.width - 6.dp.toPx()).coerceAtLeast(0f)
                    val labelY = (y - textLayout.size.height / 2f).safeCoerceIn(0f, (canvasHeight - textLayout.size.height).coerceAtLeast(0f))

                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(labelX, labelY)
                    )

                    // Засечка на оси Y
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(chartLeft - 4.dp.toPx(), y),
                        end = Offset(chartLeft, y),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Горизонтальная пунктирная линия сетки
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = Offset(chartLeft, y),
                        end = Offset(chartRight, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                }

                // Линия Оси Y
                drawLine(
                    color = AutomotiveBlue.copy(alpha = 0.6f),
                    start = Offset(chartLeft, chartTop),
                    end = Offset(chartLeft, chartBottom),
                    strokeWidth = 1.5.dp.toPx()
                )

                // Линия Оси X
                drawLine(
                    color = AutomotiveBlue.copy(alpha = 0.6f),
                    start = Offset(chartLeft, chartBottom),
                    end = Offset(chartRight, chartBottom),
                    strokeWidth = 1.5.dp.toPx()
                )

                // 2. ОТРИСОВКА ПОДПИСАННОЙ ОСИ X (ВРЕМЯ ПОЕЗДКИ)
                val xTickCount = 5
                for (c in 0 until xTickCount) {
                    val frac = c.toFloat() / (xTickCount - 1).coerceAtLeast(1)
                    val x = chartLeft + (frac * chartWidth)

                    val ptIdx = (startIndex + (frac * (sliceSize - 1)).toInt()).coerceIn(0, (safeTotal - 1).coerceAtLeast(0))
                    val ptTime = recordedSeries.firstOrNull()?.points?.getOrNull(ptIdx)?.timeLabel ?: ""

                    // Вертикальная линия сетки
                    drawLine(
                        color = Color.White.copy(alpha = 0.07f),
                        start = Offset(x, chartTop),
                        end = Offset(x, chartBottom),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )

                    // Засечка на оси X
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(x, chartBottom),
                        end = Offset(x, chartBottom + 4.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Подпись времени
                    if (ptTime.isNotBlank()) {
                        val xTimeLayout = textMeasurer.measure(ptTime, axisLabelStyle)
                        val minX = chartLeft - 10f
                        val maxX = (chartRight - xTimeLayout.size.width + 10f).coerceAtLeast(minX)
                        val labelX = (x - xTimeLayout.size.width / 2f).safeCoerceIn(minX, maxX)
                        drawText(
                            textLayoutResult = xTimeLayout,
                            topLeft = Offset(labelX, chartBottom + 5.dp.toPx())
                        )
                    }
                }

                // Подпись Оси X
                val xTitle = "Время поездки (мин:сек) →"
                val xTitleLayout = textMeasurer.measure(xTitle, axisTitleStyle)
                drawText(
                    textLayoutResult = xTitleLayout,
                    topLeft = Offset(
                        (chartLeft + (chartWidth - xTitleLayout.size.width) / 2f).coerceAtLeast(chartLeft),
                        chartBottom + 16.dp.toPx()
                    )
                )

                if (activeSensors.isEmpty() || totalPointsCount < 1) {
                    return@Canvas
                }

                // 3. ОТРИСОВКА КРИВЫХ ВЫБРАННЫХ ДАТЧИКОВ
                activeSensors.forEach { series ->
                    val fromIdx = startIndex.coerceIn(0, series.points.size)
                    val toIdx = endIndex.coerceIn(fromIdx, series.points.size)
                    val slice = if (fromIdx < toIdx) series.points.subList(fromIdx, toIdx) else emptyList()
                    if (slice.isEmpty()) return@forEach

                    val seriesScale = calculateAdequateAxisScale(
                        minVal = series.minValue,
                        maxVal = series.maxValue,
                        pid = "",
                        unit = series.unit,
                        targetTickCount = 5
                    )

                    val sMin = if (activeSensors.size == 1) effMinY else {
                        val bMin = seriesScale.baseMin
                        val bRange = seriesScale.totalRange
                        val effRange = (bRange / zoomY).coerceAtLeast(0.001)
                        val cY = bMin + (bRange / 2.0) - (panOffsetY * (bRange * 0.5))
                        cY - (effRange / 2.0)
                    }
                    val sMax = if (activeSensors.size == 1) effMaxY else {
                        val bMin = seriesScale.baseMin
                        val bRange = seriesScale.totalRange
                        val effRange = (bRange / zoomY).coerceAtLeast(0.001)
                        val cY = bMin + (bRange / 2.0) - (panOffsetY * (bRange * 0.5))
                        cY + (effRange / 2.0)
                    }
                    val sRange = (sMax - sMin).coerceAtLeast(0.001)

                    val count = slice.size
                    val stepX = if (count > 1) chartWidth / (count - 1).toFloat() else chartWidth

                    val renderedPts = slice.mapIndexed { idx, pt ->
                        val safeVal = if (pt.value.isFinite()) pt.value else 0.0
                        val normVal = ((safeVal - sMin) / sRange).toFloat().safeCoerceIn(-0.2f, 1.2f)
                        val x = (chartLeft + (idx * stepX)).coerceIn(chartLeft, chartRight)
                        val y = (chartBottom - (normVal * chartHeight)).safeCoerceIn(chartTop - 10f, (chartBottom + 10f).coerceAtLeast(chartTop - 10f))
                        Triple(x, y, safeVal)
                    }

                    // 1. Градиентная заливка по сегментам в зависимости от значений точек
                    if (chartBottom > chartTop + 5f && activeSensors.size <= 2 && renderedPts.size > 1) {
                        for (i in 0 until renderedPts.size - 1) {
                            val p1 = renderedPts[i]
                            val p2 = renderedPts[i + 1]
                            val c1 = getSensorPointColor(series.sensorName, p1.third, true)
                            val c2 = getSensorPointColor(series.sensorName, p2.third, true)
                            val segColor = if (p2.third >= p1.third) c2 else c1

                            val segFillPath = Path().apply {
                                moveTo(p1.first, chartBottom)
                                lineTo(p1.first, p1.second)
                                lineTo(p2.first, p2.second)
                                lineTo(p2.first, chartBottom)
                                close()
                            }
                            drawPath(
                                path = segFillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(segColor.copy(alpha = 0.22f), Color.Transparent),
                                    startY = minOf(p1.second, p2.second),
                                    endY = chartBottom
                                )
                            )
                        }
                    }

                    // 2. Линия графика, окрашивающая только свои участки в цвет соответствующих значений
                    if (renderedPts.size > 1) {
                        for (i in 0 until renderedPts.size - 1) {
                            val p1 = renderedPts[i]
                            val p2 = renderedPts[i + 1]
                            val c1 = getSensorPointColor(series.sensorName, p1.third, true)
                            val c2 = getSensorPointColor(series.sensorName, p2.third, true)

                            drawLine(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(c1, c2),
                                    startX = p1.first,
                                    endX = p2.first
                                ),
                                start = Offset(p1.first, p1.second),
                                end = Offset(p2.first, p2.second),
                                strokeWidth = 2.8.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    } else if (renderedPts.size == 1) {
                        val pt = renderedPts.first()
                        val ptColor = getSensorPointColor(series.sensorName, pt.third, true)
                        drawCircle(
                            color = ptColor,
                            radius = 4.dp.toPx(),
                            center = Offset(pt.first, pt.second)
                        )
                    }

                    // 3. Точки измерений
                    if (count < 60) {
                        renderedPts.forEach { pt ->
                            val ptColor = getSensorPointColor(series.sensorName, pt.third, true)
                            drawCircle(
                                color = ptColor,
                                radius = 2.5.dp.toPx(),
                                center = Offset(pt.first, pt.second)
                            )
                        }
                    }
                }

                // 4. ОТРИСОВКА ВИЗИРА (С КРЕСТИКОМ И ЛИНИЕЙ)
                scrubberFraction?.let { frac ->
                    val scrubX = (chartLeft + frac * chartWidth).coerceIn(chartLeft, chartRight)
                    drawLine(
                        color = Color.White,
                        start = Offset(scrubX, chartTop),
                        end = Offset(scrubX, chartBottom),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    drawCircle(
                        color = AutomotiveBlue,
                        radius = 4.5.dp.toPx(),
                        center = Offset(scrubX, chartTop + 6.dp.toPx())
                    )
                }
            }

            // ПЛАВАЮЩИЕ КНОПКИ ЗУМА В УГЛУ ГРАФИКА ДЛЯ УДОБСТВА УПРАВЛЕНИЯ
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    onClick = {
                        zoomX = (zoomX - 0.5f).coerceAtLeast(1.0f)
                        zoomY = (zoomY - 0.25f).coerceAtLeast(0.5f)
                    },
                    shape = CircleShape,
                    color = Color(0xFF141E2E).copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF37474F)),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Surface(
                    onClick = {
                        zoomX = (zoomX + 0.5f).coerceAtMost(30.0f)
                        zoomY = (zoomY + 0.25f).coerceAtMost(15.0f)
                    },
                    shape = CircleShape,
                    color = Color(0xFF141E2E).copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF37474F)),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // ПЛАВАЮЩИЙ ИНФОРМАТИВНЫЙ ТУЛТИП ВИЗИРА (В РЕАЛЬНОМ ВРЕМЕНИ ПРИ КАСАНИИ)
            if (currentScrubIndex != null) {
                val scrubPt = activeSensors.firstOrNull()?.points?.getOrNull(currentScrubIndex)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.92f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AutomotiveBlue.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⏱ ${scrubPt?.timeLabel ?: "--"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AutomotiveBlue
                        )
                        activeSensors.forEach { series ->
                            val pt = series.points.getOrNull(currentScrubIndex)
                            if (pt != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(7.dp).background(series.color, CircleShape))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${series.sensorName}: ${pt.value} ${series.unit}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 3. КОМПАКТНАЯ ПЛАШКА: ДИАПАЗОН ВРЕМЕНИ
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0E1420), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (timeRangeLabel.isNotBlank()) "Окно данных: $timeRangeLabel" else "Весь заезд",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AutomotiveBlue
            )

            Text(
                text = "Точек в окне: $sliceSize из $totalPointsCount",
                fontSize = 10.sp,
                color = Color(0xFF90A4AE)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 4. ВЕРТИКАЛЬНЫЙ СПИСОК ВЫБОРА ДАТЧИКОВ ПОД ГРАФИКОМ
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.85f)
                .background(Color(0xFF090E17), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ВЫБОР ДАТЧИКОВ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AutomotiveBlue
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF141E2E)
                    ) {
                        Text(
                            text = "${selectedSensorIndices.size} из ${recordedSeries.size}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF90A4AE),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        onClick = {
                            selectedSensorIndices.clear()
                            selectedSensorIndices.addAll(recordedSeries.indices)
                        },
                        shape = RoundedCornerShape(6.dp),
                        color = SoftBlueContainer,
                        modifier = Modifier.testTag("select_all_sensors_btn")
                    ) {
                        Text(
                            text = "Выбрать все",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AutomotiveBlue,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        onClick = {
                            if (recordedSeries.isNotEmpty()) {
                                selectedSensorIndices.clear()
                                selectedSensorIndices.add(0)
                            }
                        },
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF141E2E),
                        modifier = Modifier.testTag("reset_sensors_btn")
                    ) {
                        Text(
                            text = "Сброс (1)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFB0BEC5),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("vertical_sensor_list"),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(recordedSeries) { idx, series ->
                    val isSelected = selectedSensorIndices.contains(idx)
                    val scrubVal = if (currentScrubIndex != null) series.points.getOrNull(currentScrubIndex)?.value else null

                    Surface(
                        onClick = {
                            if (isSelected) {
                                if (selectedSensorIndices.size > 1) {
                                    selectedSensorIndices.remove(idx)
                                }
                            } else {
                                selectedSensorIndices.add(idx)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) series.color.copy(alpha = 0.14f) else Color(0xFF111722),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) series.color else Color(0xFF1F293D)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sensor_item_row_${idx}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (!selectedSensorIndices.contains(idx)) selectedSensorIndices.add(idx)
                                    } else {
                                        if (selectedSensorIndices.size > 1) selectedSensorIndices.remove(idx)
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = series.color,
                                    uncheckedColor = Color(0xFF455A64),
                                    checkmarkColor = Color.White
                                ),
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Box(
                                modifier = Modifier
                                    .size(width = 4.dp, height = 22.dp)
                                    .background(series.color, RoundedCornerShape(2.dp))
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = series.sensorName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFFCFD8DC),
                                        maxLines = 1
                                    )
                                    if (series.unit.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "[${series.unit}]",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = series.color
                                        )
                                    }
                                }
                                Text(
                                    text = "Мин: ${Math.round(series.minValue * 10.0) / 10.0} • Макс: ${Math.round(series.maxValue * 10.0) / 10.0}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF78909C)
                                )
                            }

                            if (scrubVal != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = series.color.copy(alpha = 0.22f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, series.color.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "$scrubVal ${series.unit}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
