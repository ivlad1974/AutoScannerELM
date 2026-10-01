package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AutomotiveBlue
import com.example.ui.theme.ClearRed
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.WarningAmber
import kotlin.math.cos
import kotlin.math.sin

/**
 * Modern Automotive Tachometer Card.
 * Equal fixed height, sleek layout with top arc gauge and CLEAN digital readout underneath (no line/needle overlap).
 */
@Composable
fun TachometerCard(
    rpmValue: Double,
    maxRpm: Double = 8000.0,
    modifier: Modifier = Modifier
) {
    val rpmClamped = rpmValue.coerceIn(0.0, maxRpm)
    val progress = (rpmClamped / maxRpm).toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 200),
        label = "TachometerAnimation"
    )

    val isRedline = rpmClamped >= 6000.0
    val activeColor = if (isRedline) ClearRed else AutomotiveBlue

    Card(
        modifier = modifier
            .height(180.dp)
            .testTag("card_tachometer_gauge"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Тахометр",
                        tint = activeColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ТАХОМЕТР",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = activeColor
                    )
                }
                Box(
                    modifier = Modifier
                        .background(activeColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isRedline) "КРАСНАЯ ЗОНА" else "ОБ/МИН",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeColor
                    )
                }
            }

            // Top Semi-Circle Arc Gauge Canvas (Clean, compact, top area only)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                val trackBorderColor = MaterialTheme.colorScheme.outline
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val center = Offset(w / 2f, h - 2.dp.toPx())
                    val radius = minOf(w / 2.2f, h * 1.3f)
                    val strokeWidth = 8.dp.toPx()

                    val startAngle = 180f
                    val sweepAngle = 180f

                    // Background Track Arc
                    drawArc(
                        color = trackBorderColor.copy(alpha = 0.25f),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Redline Zone Arc (last 25%)
                    drawArc(
                        color = ClearRed.copy(alpha = 0.35f),
                        startAngle = startAngle + (sweepAngle * 0.75f),
                        sweepAngle = sweepAngle * 0.25f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Active Progress Arc
                    val currentSweep = sweepAngle * animatedProgress
                    drawArc(
                        color = activeColor,
                        startAngle = startAngle,
                        sweepAngle = currentSweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Major Tick Marks (0, 2, 4, 6, 8)
                    for (i in 0..4) {
                        val tickAngle = startAngle + (sweepAngle * (i / 4f))
                        val rad = Math.toRadians(tickAngle.toDouble())
                        val innerR = radius - 10.dp.toPx()
                        val outerR = radius + 2.dp.toPx()

                        val startPt = Offset(
                            x = center.x + (innerR * cos(rad)).toFloat(),
                            y = center.y + (innerR * sin(rad)).toFloat()
                        )
                        val endPt = Offset(
                            x = center.x + (outerR * cos(rad)).toFloat(),
                            y = center.y + (outerR * sin(rad)).toFloat()
                        )

                        drawLine(
                            color = if (i >= 3) ClearRed else trackBorderColor,
                            start = startPt,
                            end = endPt,
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // Needle pointer
                    val needleAngle = startAngle + (sweepAngle * animatedProgress)
                    val needleRad = Math.toRadians(needleAngle.toDouble())
                    val needleLength = radius - 4.dp.toPx()
                    val needleEnd = Offset(
                        x = center.x + (needleLength * cos(needleRad)).toFloat(),
                        y = center.y + (needleLength * sin(needleRad)).toFloat()
                    )

                    drawLine(
                        color = activeColor,
                        start = center,
                        end = needleEnd,
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = activeColor, radius = 4.dp.toPx(), center = center)
                }
            }

            // CLEAN DIGITAL READOUT AREA (Completely separated below the gauge canvas)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(vertical = 6.dp, horizontal = 8.dp)
            ) {
                Text(
                    text = rpmValue.toInt().toString(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isRedline) ClearRed else MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "об/мин",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Modern Automotive Thermometer Card.
 * Equal fixed height (180dp), neat vertical meter and CLEAN digital readout.
 */
@Composable
fun ThermometerCard(
    tempValue: Double,
    modifier: Modifier = Modifier
) {
    val tempClamped = tempValue.coerceIn(-20.0, 130.0)
    val progress = ((tempClamped - (-20.0)) / (130.0 - (-20.0))).coerceIn(0.0, 1.0).toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 250),
        label = "TempAnimation"
    )

    val (tempColor, statusText) = when {
        tempClamped < 70.0 -> Pair(AutomotiveBlue, "Холодный")
        tempClamped <= 96.0 -> Pair(StatusGreen, "Норма")
        tempClamped <= 104.0 -> Pair(WarningAmber, "Высокая")
        else -> Pair(ClearRed, "ПЕРЕГРЕВ!")
    }

    Card(
        modifier = modifier
            .height(180.dp)
            .testTag("card_thermometer_gauge"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Thermostat,
                        contentDescription = "Термометр",
                        tint = tempColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ТЕМПЕРАТУРА",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = tempColor
                    )
                }
                Box(
                    modifier = Modifier
                        .background(tempColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = tempColor
                    )
                }
            }

            // Main Body: Vertical Bar + Clean Digital Readout Side-by-Side
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Sleek Vertical Thermometer Bar
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(70.dp)
                        .background(
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            RoundedCornerShape(10.dp)
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .padding(2.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(animatedProgress)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        tempColor,
                                        tempColor.copy(alpha = 0.8f)
                                    )
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                    )
                }

                // CLEAN DIGITAL READOUT AREA
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(vertical = 10.dp, horizontal = 14.dp)
                ) {
                    Text(
                        text = "${tempValue.toInt()}°C",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = tempColor,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Двигатель (ОЖ)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Bottom Info Label
            Text(
                text = "Норма: 85°C – 95°C",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
