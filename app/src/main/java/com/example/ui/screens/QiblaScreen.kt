package com.example.ui.screens

import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.QiblaSensorManager
import com.example.ui.viewmodel.PrayerViewModel
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun QiblaScreen(
    viewModel: PrayerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsState()
    val qiblaManager = remember { QiblaSensorManager(context) }
    val qiblaState by qiblaManager.state.collectAsState()

    LaunchedEffect(uiState.settings.selectedLocation) {
        qiblaManager.updateLocation(
            uiState.settings.selectedLocation.latitude,
            uiState.settings.selectedLocation.longitude
        )
    }

    DisposableEffect(Unit) {
        qiblaManager.startListening()
        onDispose {
            qiblaManager.stopListening()
        }
    }

    // Trigger haptic feedback when aligning with Qibla
    var wasFacing by remember { mutableStateOf(false) }
    LaunchedEffect(qiblaState.isFacingQibla) {
        if (qiblaState.isFacingQibla && !wasFacing) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        wasFacing = qiblaState.isFacingQibla
    }

    val animatedNeedleAngle by animateFloatAsState(
        targetValue = qiblaState.needleAngle,
        animationSpec = tween(durationMillis = 150),
        label = "needle_anim"
    )

    val animatedAzimuth by animateFloatAsState(
        targetValue = qiblaState.deviceAzimuth,
        animationSpec = tween(durationMillis = 150),
        label = "azimuth_anim"
    )

    val dialBorderColor by animateColorAsState(
        targetValue = if (qiblaState.isFacingQibla) Color(0xFF22C55E) else Color(0xFFFFD166).copy(alpha = 0.5f),
        label = "status_color"
    )

    // Calculate turning advice
    val rawDiff = ((qiblaState.needleAngle + 180f) % 360f) - 180f
    val adviceText = when {
        abs(rawDiff) <= 4f -> "أنت مواجه للقبلة المشرفة مباشرة 🕋"
        rawDiff > 4f -> "أدر الهاتف ${rawDiff.roundToInt()}° جهة اليمين ↻"
        else -> "أدر الهاتف ${abs(rawDiff).roundToInt()}° جهة اليسار ↺"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("qibla_screen")
    ) {
        // Location & Distance Header
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = uiState.settings.selectedLocation.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = String.format(Locale.US, "%.0f كم إلى مكة", qiblaState.distanceKm),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dynamic Turn Advice Pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (qiblaState.isFacingQibla) Color(0xFF134E3F)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = adviceText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (qiblaState.isFacingQibla) Color(0xFF86EFAC) else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // High-Precision Compass Dial Container
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(316.dp)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0F2620), // Deep emerald center
                            Color(0xFF081814), // Dark celestial outer
                            Color(0xFF040B0A)
                        )
                    )
                )
                .border(width = 3.5.dp, color = dialBorderColor, shape = CircleShape)
                .padding(14.dp)
        ) {
            val isFacing = qiblaState.isFacingQibla

            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f

                // 1. Draw outer ticks and cardinal points (rotated by -deviceAzimuth so North points to real North)
                rotate(degrees = -animatedAzimuth, pivot = center) {
                    drawEnhancedCompassRose(
                        center = center,
                        radius = radius,
                        textColor = Color.White
                    )

                    // Draw static Kaaba marker on the dial at the exact Qibla bearing
                    rotate(degrees = qiblaState.qiblaBearing, pivot = center) {
                        val kaabaMarkerY = center.y - (radius - 12f)
                        drawCircle(
                            color = Color(0xFFFFD166),
                            radius = 6f,
                            center = Offset(center.x, kaabaMarkerY)
                        )
                    }
                }

                // 2. Draw Qibla Needle pointing towards Kaaba
                rotate(degrees = animatedNeedleAngle, pivot = center) {
                    drawEnhancedQiblaNeedle(
                        center = center,
                        radius = radius * 0.84f,
                        isFacing = isFacing
                    )
                }

                // 3. Center pivot jewel
                drawCircle(
                    color = if (isFacing) Color(0xFF22C55E) else Color(0xFFFFD166),
                    radius = 12f,
                    center = center
                )
                drawCircle(
                    color = Color.White,
                    radius = 5f,
                    center = center
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Bearing Info Cards
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(14.dp).fillMaxWidth()
                ) {
                    Text(
                        text = "زاوية القبلة",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f°", qiblaState.qiblaBearing),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(14.dp).fillMaxWidth()
                ) {
                    Text(
                        text = "اتجاه الهاتف",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f°", qiblaState.deviceAzimuth),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Facing Qibla Success Banner
        AnimatedVisibility(visible = qiblaState.isFacingQibla) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF134E3F)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4ADE80),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "أنت تتجه الآن نحو القبلة ومكة المكرمة بدقة!",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDCFCE7)
                    )
                }
            }
        }

        if (!qiblaState.hasSensor) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "تنبيه: لا يتوفر مستشعر بوصلة مغناطيسية على هذا الجهاز، زاوية القبلة الثابتة لموقعك هي ${String.format(Locale.US, "%.1f°", qiblaState.qiblaBearing)} من الشمال.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CompassCalibration,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ضع الهاتف بشكل مسطح وأدره بشكل الرقم (8) للمعايرة الدقيقة",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

private fun DrawScope.drawEnhancedCompassRose(
    center: Offset,
    radius: Float,
    textColor: Color
) {
    // Outer dial circle
    drawCircle(
        color = Color(0x33FFFFFF),
        radius = radius - 8f,
        center = center,
        style = Stroke(width = 1.5f)
    )

    // Inner dial circle
    drawCircle(
        color = Color(0x22FFFFFF),
        radius = radius - 30f,
        center = center,
        style = Stroke(width = 1f)
    )

    val paint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = 28f
        isFakeBoldText = true
    }

    // Ticks around 360 degrees
    for (deg in 0 until 360 step 5) {
        val isCardinal = (deg % 90 == 0)
        val isMajor = (deg % 30 == 0)
        val tickLen = if (isCardinal) 16f else if (isMajor) 10f else 5f
        val rad = Math.toRadians(deg.toDouble())
        val outerX = center.x + (radius - 10f) * sin(rad).toFloat()
        val outerY = center.y - (radius - 10f) * cos(rad).toFloat()
        val innerX = center.x + (radius - 10f - tickLen) * sin(rad).toFloat()
        val innerY = center.y - (radius - 10f - tickLen) * cos(rad).toFloat()

        val tickColor = when {
            deg == 0 -> Color(0xFFEF4444) // North in Red
            isCardinal -> Color(0xFFFFD166) // Gold for cardinal
            isMajor -> Color(0xFFCBD5E1)
            else -> Color(0x55FFFFFF)
        }

        drawLine(
            color = tickColor,
            start = Offset(innerX, innerY),
            end = Offset(outerX, outerY),
            strokeWidth = if (isCardinal) 2.5f else if (isMajor) 1.8f else 1f
        )

        // Draw Cardinal labels (N, E, S, W)
        if (isCardinal) {
            val label = when (deg) {
                0 -> "N"
                90 -> "E"
                180 -> "S"
                270 -> "W"
                else -> ""
            }
            paint.color = if (deg == 0) android.graphics.Color.RED else android.graphics.Color.WHITE
            val textDist = radius - 44f
            val textX = center.x + textDist * sin(rad).toFloat()
            val textY = center.y - textDist * cos(rad).toFloat() + 10f
            drawContext.canvas.nativeCanvas.drawText(label, textX, textY, paint)
        }
    }
}

private fun DrawScope.drawEnhancedQiblaNeedle(
    center: Offset,
    radius: Float,
    isFacing: Boolean
) {
    val tip = Offset(center.x, center.y - radius)
    val baseLeft = Offset(center.x - 20f, center.y + 24f)
    val baseRight = Offset(center.x + 20f, center.y + 24f)
    val tail = Offset(center.x, center.y + 45f)

    val needleColor = if (isFacing) Color(0xFF22C55E) else Color(0xFFFFD166)
    val secondaryColor = if (isFacing) Color(0xFF15803D) else Color(0xFFD97706)

    // Left needle wing
    val leftPath = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(center.x, center.y)
        lineTo(baseLeft.x, baseLeft.y)
        lineTo(tail.x, tail.y)
        close()
    }
    drawPath(path = leftPath, color = needleColor)

    // Right needle wing with shading
    val rightPath = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(baseRight.x, baseRight.y)
        lineTo(tail.x, tail.y)
        lineTo(center.x, center.y)
        close()
    }
    drawPath(path = rightPath, color = secondaryColor)

    // Kaaba icon marker at the needle head
    val kaabaSize = 28f
    val kaabaTopY = tip.y - kaabaSize - 8f
    drawRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(tip.x - kaabaSize / 2f, kaabaTopY),
        size = androidx.compose.ui.geometry.Size(kaabaSize, kaabaSize)
    )
    // Golden Kiswa band on Kaaba
    drawRect(
        color = Color(0xFFFFD166),
        topLeft = Offset(tip.x - kaabaSize / 2f, kaabaTopY + 6f),
        size = androidx.compose.ui.geometry.Size(kaabaSize, 4f)
    )
}
