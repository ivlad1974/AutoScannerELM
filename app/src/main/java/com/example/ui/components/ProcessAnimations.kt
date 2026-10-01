package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AutomotiveBlue
import com.example.ui.theme.ClearRed
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.WarningAmber
import kotlin.math.cos
import kotlin.math.sin

/**
 * Compact animated process widget placed in the connection button slot.
 * Replaces full-screen blocking overlays with an inline cockpit radar & shimmer progress indicator,
 * filling the ENTIRE button with vibrant, high-visibility automotive animations across the full button area.
 */
@Composable
fun InlineConnectingProcessWidget(
    statusText: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "InlineConnectingTransition")

    // Full button glowing background gradient pulse and sweep
    val bgSweepOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "BgSweep"
    )

    // Glowing border pulse
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BorderPulse"
    )

    // Radar pulse rings
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, delayMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale2"
    )
    val pulseAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, delayMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha2"
    )

    // Particle waves across entire button
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveOffset"
    )

    // Vibrant rainbow / chromatic shifting phase across entire button
    val rainbowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RainbowPhase"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitRotation"
    )

    val chromaticSpectrum = listOf(
        Color(0xFF00E5FF), // Cyan
        Color(0xFF3B82F6), // Blue
        Color(0xFF8B5CF6), // Purple
        Color(0xFFEC4899), // Pink
        Color(0xFFF59E0B), // Amber
        Color(0xFF10B981), // Green
        Color(0xFF00E5FF)  // Loop
    )

    Card(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(
                2.2.dp,
                Brush.horizontalGradient(
                    colors = chromaticSpectrum,
                    startX = rainbowPhase * 500f,
                    endX = (rainbowPhase * 500f) + 300f
                ),
                RoundedCornerShape(14.dp)
            )
            .testTag("inline_connecting_process_widget"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF080E20))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // LAYER 1: Full-button animated background canvas (glowing multicolor wave sweep, particles, chromatic shift)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Full-surface multi-color iridescent aurora background
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = chromaticSpectrum.map { it.copy(alpha = 0.22f) },
                        startX = (rainbowPhase * w * 2f) - w,
                        endX = (rainbowPhase * w * 2f) + w
                    ),
                    size = size
                )

                // Moving bright laser gradient beam across the whole button
                val beamCenter = w * bgSweepOffset
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF3B82F6).copy(alpha = 0.30f),
                            Color(0xFFEC4899).copy(alpha = 0.45f),
                            Color(0xFF00E5FF).copy(alpha = 0.50f),
                            Color.White.copy(alpha = 0.35f),
                            Color(0xFF00E5FF).copy(alpha = 0.50f),
                            Color(0xFF10B981).copy(alpha = 0.30f),
                            Color.Transparent
                        ),
                        startX = beamCenter - 140.dp.toPx(),
                        endX = beamCenter + 140.dp.toPx()
                    ),
                    size = size
                )

                // High-tech subtle data grid pattern across entire button
                val gridStep = 16.dp.toPx()
                var gx = (w * (bgSweepOffset * 0.4f)) % gridStep
                while (gx < w) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = Offset(gx, 0f),
                        end = Offset(gx, h),
                        strokeWidth = 1.dp.toPx()
                    )
                    gx += gridStep
                }

                // Dynamic glowing sine wave flowing through the whole background
                val wavePath = Path()
                val waveY = h * 0.72f
                val waveAmplitude = 6.dp.toPx()
                val waveFreq = 0.022f
                val phase = waveOffset * kotlin.math.PI.toFloat() * 2f

                for (x in 0..w.toInt() step 6) {
                    val y = waveY + kotlin.math.sin(x * waveFreq + phase) * waveAmplitude
                    if (x == 0) wavePath.moveTo(0f, y) else wavePath.lineTo(x.toFloat(), y)
                }
                drawPath(
                    path = wavePath,
                    brush = Brush.horizontalGradient(
                        colors = chromaticSpectrum,
                        startX = rainbowPhase * w,
                        endX = (rainbowPhase * w) + w
                    ),
                    style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                )

                // Energy dot riding the wave
                val dotX = (waveOffset * w).coerceIn(0f, w)
                val dotY = waveY + kotlin.math.sin(dotX * waveFreq + phase) * waveAmplitude
                drawCircle(
                    color = Color.White,
                    radius = 3.5.dp.toPx(),
                    center = Offset(dotX, dotY)
                )
                drawCircle(
                    color = Color(0xFF00E5FF),
                    radius = 5.dp.toPx(),
                    center = Offset(dotX, dotY)
                )
            }

            // LAYER 2: Foreground content with radar, texts and cancel button
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left High-Tech Animated Radar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("inline_radar_canvas"),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val baseRadius = size.minDimension * 0.46f

                        // Outer pulsing radar waves
                        drawCircle(
                            color = AutomotiveBlue.copy(alpha = pulseAlpha),
                            radius = baseRadius * pulseScale,
                            center = center,
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                        drawCircle(
                            color = Color(0xFF00E5FF).copy(alpha = pulseAlpha2),
                            radius = baseRadius * pulseScale2,
                            center = center,
                            style = Stroke(width = 1.8.dp.toPx())
                        )

                        // Base radar scope ring
                        drawCircle(
                            color = Color(0xFF00E5FF).copy(alpha = 0.4f),
                            radius = baseRadius * 0.78f,
                            center = center,
                            style = Stroke(width = 1.2.dp.toPx())
                        )

                        // Rotating scanning satellite
                        rotate(rotationAngle, pivot = center) {
                            drawCircle(
                                color = Color(0xFF00E5FF),
                                radius = 3.dp.toPx(),
                                center = Offset(center.x + baseRadius * 0.85f, center.y)
                            )
                            drawLine(
                                color = Color(0xFF00E5FF).copy(alpha = 0.6f),
                                start = center,
                                end = Offset(center.x + baseRadius * 0.85f, center.y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }

                    // Center glowing Bluetooth chip
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(0xFF0F244A), CircleShape)
                            .border(1.2.dp, Color(0xFF00E5FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BluetoothSearching,
                            contentDescription = "Connecting",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Center Text Info & Animated Neon Bar
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(Color(0xFF00E5FF), CircleShape)
                        )
                        Text(
                            text = "ПОДКЛЮЧЕНИЕ К ELM327",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.6.sp,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = statusText.ifBlank { "Синхронизация протокола..." },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    // Neon animated full-width progress line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF162544))
                    ) {
                        val barOffset by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "BarShimmer"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            AutomotiveBlue.copy(alpha = 0.2f),
                                            Color(0xFF00E5FF),
                                            Color.White,
                                            Color(0xFF00E5FF),
                                            AutomotiveBlue.copy(alpha = 0.2f)
                                        ),
                                        startX = barOffset * 400f,
                                        endX = (barOffset * 400f) + 140f
                                    )
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Cancel button with clear red accent
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0xFF1E293B), RoundedCornerShape(9.dp))
                        .border(1.2.dp, ClearRed.copy(alpha = 0.7f), RoundedCornerShape(9.dp))
                        .testTag("btn_connecting_cancel")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Отмена",
                        tint = ClearRed,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

/**
 * Compact animated scan process widget placed in the connection button slot.
 * Fills the ENTIRE button with high-visibility automotive scanning sweep and radar animations.
 */
@Composable
fun InlineScanningProcessWidget(
    progressText: String,
    progressFraction: Float,
    onStopScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "InlineScanningTransition")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarSweep"
    )

    // Full button scan wave sweep
    val scanBeamOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ScanBeam"
    )

    val scanRainbowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ScanRainbowPhase"
    )

    val scanChromaticSpectrum = listOf(
        Color(0xFF00E5FF),
        Color(0xFF3B82F6),
        Color(0xFF8B5CF6),
        Color(0xFFEC4899),
        Color(0xFFF59E0B),
        Color(0xFF10B981),
        Color(0xFF00E5FF)
    )

    val percent = (progressFraction * 100).toInt().coerceIn(0, 100)

    Card(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(
                2.2.dp,
                Brush.horizontalGradient(
                    colors = scanChromaticSpectrum,
                    startX = scanRainbowPhase * 500f,
                    endX = (scanRainbowPhase * 500f) + 300f
                ),
                RoundedCornerShape(14.dp)
            )
            .testTag("inline_scanning_process_widget"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF080E20))
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // LAYER 1: Full-button animated background radar beam sweep & rainbow aurora
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Full-surface multi-color iridescent aurora background
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = scanChromaticSpectrum.map { it.copy(alpha = 0.22f) },
                        startX = (scanRainbowPhase * w * 2f) - w,
                        endX = (scanRainbowPhase * w * 2f) + w
                    ),
                    size = size
                )

                // Full-width scan sweep beam
                val beamX = w * scanBeamOffset
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF00E5FF).copy(alpha = 0.25f),
                            Color(0xFFEC4899).copy(alpha = 0.40f),
                            Color.White.copy(alpha = 0.45f),
                            Color(0xFF00E5FF).copy(alpha = 0.40f),
                            Color(0xFF10B981).copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        startX = beamX - 120.dp.toPx(),
                        endX = beamX + 120.dp.toPx()
                    ),
                    size = size
                )

                // Digital grid marks along the top and bottom
                val markStep = 24.dp.toPx()
                var mx = 0f
                while (mx < w) {
                    drawLine(
                        color = Color(0xFF00E5FF).copy(alpha = 0.25f),
                        start = Offset(mx, 0f),
                        end = Offset(mx, 4.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFF00E5FF).copy(alpha = 0.25f),
                        start = Offset(mx, h - 4.dp.toPx()),
                        end = Offset(mx, h),
                        strokeWidth = 1.dp.toPx()
                    )
                    mx += markStep
                }
            }

            // LAYER 2: Foreground UI
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Radar Sweep
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("inline_scan_radar_canvas"),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val maxRadius = size.minDimension * 0.46f

                        // Radar circles
                        drawCircle(
                            color = Color(0xFF00E5FF).copy(alpha = 0.35f),
                            radius = maxRadius,
                            center = center,
                            style = Stroke(width = 1.2.dp.toPx())
                        )
                        drawCircle(
                            color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                            radius = maxRadius * 0.55f,
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )

                        // Sweeping beam
                        val rad = Math.toRadians(sweepAngle.toDouble())
                        val beamEnd = Offset(
                            (center.x + maxRadius * cos(rad)).toFloat(),
                            (center.y + maxRadius * sin(rad)).toFloat()
                        )
                        drawLine(
                            color = Color(0xFF00E5FF),
                            start = center,
                            end = beamEnd,
                            strokeWidth = 2.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Sweep sector
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(Color(0xFF00E5FF).copy(alpha = 0.45f), Color.Transparent),
                                center = center
                            ),
                            startAngle = sweepAngle - 50f,
                            sweepAngle = 50f,
                            useCenter = true
                        )
                    }

                    // Center Car Icon
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(0xFF091E42), CircleShape)
                            .border(1.2.dp, Color(0xFF00E5FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Center Status & Linear Progress
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ОПРОС ЭБУ CAN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00E5FF),
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = "$percent%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = progressText.ifBlank { "Считывание кодов неисправностей..." },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFCBD5E1),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = { progressFraction.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.5.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color(0xFF00E5FF),
                        trackColor = Color(0xFF1E293B)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Stop Button
                IconButton(
                    onClick = onStopScan,
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0xFF1E293B), RoundedCornerShape(9.dp))
                        .border(1.2.dp, ClearRed.copy(alpha = 0.7f), RoundedCornerShape(9.dp))
                        .testTag("btn_stop_active_scan")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Остановить опрос",
                        tint = ClearRed,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

/**
 * Large, eye-catching animated overlay for ELM327 adapter connection process.
 */
@Composable
fun LargeConnectingProcessOverlay(
    visible: Boolean,
    statusText: String = "Установка соединения с ELM327 v1.5...",
    onCancel: () -> Unit
) {
    if (!visible) return

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(20.dp)
                .testTag("connecting_process_overlay"),
            contentAlignment = Alignment.Center
        ) {
            ConnectingAnimationCard(
                statusText = statusText,
                onCancel = onCancel
            )
        }
    }
}

@Composable
fun ConnectingAnimationCard(
    statusText: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ConnectingTransition")

    // Radar pulse wave
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    // Second pulse wave with offset
    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale2"
    )
    val pulseAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha2"
    )

    // Rotation of outer orbit ring
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitRotation"
    )

    // Shimmer bar
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Shimmer"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .border(2.dp, AutomotiveBlue.copy(alpha = 0.6f), RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Badge
            Surface(
                color = AutomotiveBlue.copy(alpha = 0.18f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AutomotiveBlue.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(AutomotiveBlue, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ПОДКЛЮЧЕНИЕ К ELM327",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = AutomotiveBlue,
                        letterSpacing = 1.2.sp
                    )
                }
            }

            // Central Large Radar Animation Canvas
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .testTag("large_connecting_radar_canvas"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val baseRadius = size.minDimension * 0.42f

                    // Pulsing Outer Rings
                    drawCircle(
                        color = AutomotiveBlue.copy(alpha = pulseAlpha),
                        radius = baseRadius * pulseScale,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = pulseAlpha2),
                        radius = baseRadius * pulseScale2,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Concentric static radar circles
                    drawCircle(
                        color = AutomotiveBlue.copy(alpha = 0.2f),
                        radius = baseRadius * 0.75f,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = AutomotiveBlue.copy(alpha = 0.35f),
                        radius = baseRadius,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // Crosshair lines
                    drawLine(
                        color = AutomotiveBlue.copy(alpha = 0.25f),
                        start = Offset(center.x - baseRadius, center.y),
                        end = Offset(center.x + baseRadius, center.y),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = AutomotiveBlue.copy(alpha = 0.25f),
                        start = Offset(center.x, center.y - baseRadius),
                        end = Offset(center.x, center.y + baseRadius),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Orbiting satellites/data packets
                    rotate(rotationAngle, pivot = center) {
                        val orbitRadius = baseRadius * 0.88f
                        drawCircle(
                            color = Color(0xFF00E5FF),
                            radius = 6.dp.toPx(),
                            center = Offset(center.x + orbitRadius, center.y)
                        )
                        drawCircle(
                            color = AutomotiveBlue,
                            radius = 4.dp.toPx(),
                            center = Offset(center.x - orbitRadius, center.y)
                        )
                    }
                }

                // Center Icon with Glowing background
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(AutomotiveBlue.copy(alpha = 0.4f), Color(0xFF1E293B))
                            ),
                            CircleShape
                        )
                        .border(2.dp, AutomotiveBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BluetoothSearching,
                        contentDescription = "Bluetooth Searching",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            // Connection Details & Animated Progress
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Инициализация адаптера...",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Custom Glowing Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1E293B))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        AutomotiveBlue.copy(alpha = 0.3f),
                                        Color(0xFF00E5FF),
                                        AutomotiveBlue.copy(alpha = 0.3f)
                                    ),
                                    startX = shimmerOffset * 500f,
                                    endX = (shimmerOffset * 500f) + 300f
                                )
                            )
                    )
                }
            }

            // Process steps list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StepRow(stepNumber = "1", title = "Поиск адаптера Bluetooth SPP", isDone = true)
                StepRow(stepNumber = "2", title = "Сброс и настройка чипа (ATZ, ATE0, ATSP0)", isDone = true)
                StepRow(stepNumber = "3", title = "Определение протокола шины и напряжения", isDone = false, isCurrent = true)
                StepRow(stepNumber = "4", title = "Синхронизация с блоками управления (ECU)", isDone = false)
            }

            // Cancel Button
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_cancel_connecting"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Отменить подключение", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StepRow(
    stepNumber: String,
    title: String,
    isDone: Boolean,
    isCurrent: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(
                    when {
                        isDone -> StatusGreen
                        isCurrent -> Color(0xFF00E5FF)
                        else -> Color(0xFF475569)
                    },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDone || isCurrent) Color.Black else Color.White
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isDone -> StatusGreen
                isCurrent -> Color(0xFF00E5FF)
                else -> Color(0xFF94A3B8)
            }
        )
    }
}

/**
 * Large, eye-catching animated component for DTC ECU scan process.
 */
@Composable
fun LargeDtcScanningProcessOverlay(
    visible: Boolean,
    progressText: String = "Сканирование блоков управления...",
    progressFraction: Float = 0.5f,
    onStopScan: () -> Unit = {}
) {
    if (!visible) return

    Dialog(
        onDismissRequest = { /* Don't dismiss by tap outside during scan */ },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f))
                .padding(20.dp)
                .testTag("dtc_scanning_process_overlay"),
            contentAlignment = Alignment.Center
        ) {
            ScanningAnimationCard(
                progressText = progressText,
                progressFraction = progressFraction,
                onStopScan = onStopScan
            )
        }
    }
}

@Composable
fun ScanningAnimationCard(
    progressText: String,
    progressFraction: Float,
    onStopScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ScanningTransition")

    // Radar beam sweeping angle (0 to 360)
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarSweep"
    )

    // Pulse for ECU node beacons
    val beaconPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BeaconPulse"
    )

    val percent = (progressFraction * 100).toInt().coerceIn(0, 100)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .border(2.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1329))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header Badge
            Surface(
                color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF00E5FF), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ГЛУБОКИЙ ОПРОС ЭБУ (MODE 03 / 07 / 0A)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00E5FF),
                        letterSpacing = 1.2.sp
                    )
                }
            }

            // Radar Sweep & ECU Topology Canvas
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .testTag("large_dtc_scan_radar_canvas"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val maxRadius = size.minDimension * 0.46f

                    // Grid circles
                    for (i in 1..4) {
                        drawCircle(
                            color = Color(0xFF00E5FF).copy(alpha = 0.12f * i),
                            radius = maxRadius * (i / 4f),
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    // Crosshair coordinate lines
                    drawLine(
                        color = Color(0xFF00E5FF).copy(alpha = 0.25f),
                        start = Offset(center.x - maxRadius, center.y),
                        end = Offset(center.x + maxRadius, center.y),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFF00E5FF).copy(alpha = 0.25f),
                        start = Offset(center.x, center.y - maxRadius),
                        end = Offset(center.x, center.y + maxRadius),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Sweeping radar beam line
                    val rad = Math.toRadians(sweepAngle.toDouble())
                    val beamEnd = Offset(
                        (center.x + maxRadius * cos(rad)).toFloat(),
                        (center.y + maxRadius * sin(rad)).toFloat()
                    )
                    drawLine(
                        color = Color(0xFF00E5FF),
                        start = center,
                        end = beamEnd,
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Sweeping radar sector gradient
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color(0xFF00E5FF).copy(alpha = 0.35f),
                                Color.Transparent
                            ),
                            center = center
                        ),
                        startAngle = sweepAngle - 45f,
                        sweepAngle = 45f,
                        useCenter = true
                    )

                    // ECU Topology Nodes on the Radar (ECM, TCM, ABS, SRS, BCM, IPC)
                    val nodes = listOf(
                        Pair(0.35f, 30.0),   // ECM 7E0
                        Pair(0.65f, 110.0),  // TCM 7E1
                        Pair(0.78f, 210.0),  // ABS 7E2
                        Pair(0.55f, 280.0),  // SRS 7E3
                        Pair(0.85f, 330.0),  // BCM 7E4
                        Pair(0.45f, 170.0)   // IPC 7E5
                    )

                    nodes.forEachIndexed { idx, node ->
                        val nodeRad = Math.toRadians(node.second)
                        val nx = (center.x + maxRadius * node.first * cos(nodeRad)).toFloat()
                        val ny = (center.y + maxRadius * node.first * sin(nodeRad)).toFloat()
                        val isScanned = (idx.toFloat() / nodes.size.toFloat()) <= progressFraction

                        drawCircle(
                            color = if (isScanned) StatusGreen else Color(0xFF00E5FF).copy(alpha = beaconPulse),
                            radius = if (isScanned) 6.dp.toPx() else (5.dp.toPx() * beaconPulse),
                            center = Offset(nx, ny)
                        )
                        drawCircle(
                            color = if (isScanned) StatusGreen.copy(alpha = 0.4f) else Color(0xFF00E5FF).copy(alpha = 0.3f),
                            radius = 10.dp.toPx(),
                            center = Offset(nx, ny),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }

                // Center Car Silhouette with Glow
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color(0xFF0F172A), CircleShape)
                        .border(1.5.dp, Color(0xFF00E5FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "Scanning Car",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Percentage and Live Status
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$percent",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF),
                        modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                    )
                }

                Text(
                    text = progressText.ifBlank { "Опрос шины CAN на наличие кодов неисправностей..." },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center,
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Smooth Progress Bar
                LinearProgressIndicator(
                    progress = { progressFraction.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF00E5FF),
                    trackColor = Color(0xFF1E293B)
                )
            }

            // Live CAN frame telemetry feed
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF050B18), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CAN > 7E0 [Mode 03] → 43 00 00 [OK]",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = StatusGreen
                    )
                    Text(
                        text = "500 kbps",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Stop Scan Button
            Button(
                onClick = onStopScan,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_stop_active_scan"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = ClearRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = ClearRed)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Остановить опрос", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ClearRed)
            }
        }
    }
}
