package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AutomotiveBlue
import com.example.ui.theme.ClearRed

@Composable
fun HUDGauge(
    label: String,
    value: Double,
    unit: String,
    minVal: Double,
    maxVal: Double,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    warningThreshold: Double? = null
) {
    val progress = ((value - minVal) / (maxVal - minVal)).coerceIn(0.0, 1.0).toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 250),
        label = "GaugeArc"
    )

    val isWarning = warningThreshold != null && value >= warningThreshold
    val activeColor = if (isWarning) ClearRed else accentColor
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    Box(
        modifier = modifier
            .aspectRatio(1.1f)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(12.dp)
            .testTag("hud_gauge_$label"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
            val startAngle = 135f
            val sweepAngle = 270f

            // Track arc
            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress arc
            drawArc(
                color = activeColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = com.example.data.formatSensorValue(value, unit = unit),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isWarning) ClearRed else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = unit,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = activeColor
            )
        }
    }
}
