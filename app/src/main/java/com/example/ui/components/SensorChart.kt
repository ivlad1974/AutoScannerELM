package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ObdSensor

@Composable
fun SensorChart(
    sensor: ObdSensor,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    onPeriodChange: ((Int) -> Unit)? = null
) {
    val now = System.currentTimeMillis()
    val periodSec = if (sensor.graphPeriodSec > 0) sensor.graphPeriodSec else 120
    val cutoff = now - (periodSec * 1000L)
    val currentPt = Pair(now, sensor.value)
    val rawFiltered = sensor.history.filter { it.first >= cutoff }
    val effectiveHistory = if (rawFiltered.isEmpty()) {
        listOf(Pair(now - 3000L, sensor.value), currentPt)
    } else if (now - rawFiltered.last().first >= 100L) {
        rawFiltered + currentPt
    } else {
        rawFiltered
    }

    val history = effectiveHistory
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    val textMeasurer = rememberTextMeasurer()
    val axisTextStyle = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
    )

    val periodOptions = listOf(
        30 to "30с",
        60 to "1м",
        120 to "2м",
        300 to "5м",
        600 to "10м",
        1800 to "30м"
    )

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, outlineColor, RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("sensor_chart_${sensor.pid}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sensor.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = sensor.category,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val currentValueColor = getSensorPointColor(sensor.pid, sensor.value, true)
            Text(
                text = "${sensor.value} ${sensor.unit}",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = currentValueColor
            )
        }

        // Per-sensor configurable time period selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Период:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
            periodOptions.forEach { (sec, label) ->
                val isSelected = periodSec == sec
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onPeriodChange?.invoke(sec) }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
                .padding(top = 8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                val minVal = if (history.isNotEmpty()) history.minOf { it.second } else sensor.minVal
                val maxVal = if (history.isNotEmpty()) history.maxOf { it.second } else sensor.maxVal

                val scale = calculateAdequateAxisScale(
                    minVal = minVal,
                    maxVal = maxVal,
                    pid = sensor.pid,
                    unit = sensor.unit,
                    targetTickCount = 3
                )
                val baseMin = scale.baseMin
                val totalRange = scale.totalRange

                // Measure max width of Y-axis labels on the left
                var maxYLabelWidth = 0f
                val labelLayouts = scale.ticks.map { tick ->
                    val measured = textMeasurer.measure(tick.formattedText, axisTextStyle)
                    if (measured.size.width > maxYLabelWidth) {
                        maxYLabelWidth = measured.size.width.toFloat()
                    }
                    tick to measured
                }

                val leftAxisWidth = (maxYLabelWidth + 8.dp.toPx()).coerceAtLeast(32.dp.toPx())
                val chartLeft = leftAxisWidth
                val chartRight = width
                val chartWidth = (chartRight - chartLeft).coerceAtLeast(10f)
                val topPad = 8.dp.toPx()
                val bottomPad = 8.dp.toPx()
                val effectiveHeight = (height - topPad - bottomPad).coerceAtLeast(10f)

                // 1. Draw horizontal grid lines and left Y-axis labels
                labelLayouts.forEach { (tick, layout) ->
                    val normY = ((tick.value - baseMin) / totalRange).toFloat().coerceIn(0f, 1f)
                    val y = height - bottomPad - (normY * effectiveHeight)

                    // Left-aligned Y-axis value label
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(0f, (y - layout.size.height / 2f).coerceIn(0f, height - layout.size.height))
                    )

                    // Horizontal dashed grid line
                    drawLine(
                        color = gridColor,
                        start = Offset(chartLeft, y),
                        end = Offset(chartRight, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                }

                // Y-axis separator line
                drawLine(
                    color = gridColor,
                    start = Offset(chartLeft, 0f),
                    end = Offset(chartLeft, height),
                    strokeWidth = 1.dp.toPx()
                )

                if (history.size < 2) {
                    drawLine(
                        color = outlineColor,
                        start = Offset(chartLeft, height / 2),
                        end = Offset(chartRight, height / 2),
                        strokeWidth = 2.dp.toPx()
                    )
                    return@Canvas
                }

                val oldestTime = history.first().first
                val activeSpanMs = (now - oldestTime).toFloat().coerceAtLeast(10_000f)
                val totalWindowMs = if (now - oldestTime >= (periodSec * 1000L)) (periodSec * 1000L).toFloat() else activeSpanMs
                val startTime = now - totalWindowMs

                val renderedPts = history.map { pair ->
                    val normY = ((pair.second - baseMin) / totalRange).toFloat().coerceIn(0.02f, 0.98f)
                    val xFraction = ((pair.first - startTime) / totalWindowMs).coerceIn(0f, 1f)
                    val x = chartLeft + (xFraction * chartWidth)
                    val y = height - bottomPad - (normY * effectiveHeight)
                    Triple(x, y, pair.second)
                }

                // Draw segmented gradient fill
                for (i in 0 until renderedPts.size - 1) {
                    val p1 = renderedPts[i]
                    val p2 = renderedPts[i + 1]
                    val c1 = getSensorPointColor(sensor.pid, p1.third, true)
                    val c2 = getSensorPointColor(sensor.pid, p2.third, true)
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
                            colors = listOf(segColor.copy(alpha = 0.28f), Color.Transparent),
                            startY = minOf(p1.second, p2.second),
                            endY = height
                        )
                    )
                }

                // Draw multi-color line stroke
                for (i in 0 until renderedPts.size - 1) {
                    val p1 = renderedPts[i]
                    val p2 = renderedPts[i + 1]
                    val c1 = getSensorPointColor(sensor.pid, p1.third, true)
                    val c2 = getSensorPointColor(sensor.pid, p2.third, true)

                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(c1, c2),
                            startX = p1.first,
                            endX = p2.first
                        ),
                        start = Offset(p1.first, p1.second),
                        end = Offset(p2.first, p2.second),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
