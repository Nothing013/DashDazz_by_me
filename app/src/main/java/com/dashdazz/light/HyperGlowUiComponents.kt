package com.dashdazz.light

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.cos
import kotlin.math.sin

/**
 * Task 1: Modernized "Hyper-Glow" UI Components & Interactive Live Preview Header
 */

@Composable
fun HyperGlowCard(
    accentColor: Color,
    glowIntensity: Float = 0.8f,
    isCompact: Boolean = false,
    isAmoled: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val cornerRadius = if (isCompact) 14.dp else 20.dp
    val padding = if (isCompact) 12.dp else 18.dp
    val bgColor = if (isAmoled) Color(0xFF070709) else Color(0xFF141721).copy(alpha = 0.85f)
    val borderColor = accentColor.copy(alpha = 0.25f * glowIntensity.coerceIn(0.2f, 1f))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(bgColor)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(borderColor, accentColor.copy(alpha = 0.05f), borderColor)
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(padding)
    ) {
        Column(content = content)
    }
}

/**
 * Interactive Preview Header:
 * Simulates the camera-ring LED on Xiaomi HyperOS devices in real time using Compose Canvas.
 */
@Composable
fun InteractiveRingPreviewHeader(
    mode: Int,
    currentColor: Int,
    brightness: Float,
    speedMs: Long,
    isRunning: Boolean,
    isScreenPaused: Boolean,
    accentColor: Color,
    glowIntensity: Float,
    isAmoled: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RingAnimation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (speedMs * 30).coerceIn(600L, 8000L).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    val bgHeader = if (isAmoled) Color(0xFF030305) else Color(0xFF0D1017)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(bgHeader)
            .border(
                width = 1.5.dp,
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.35f * glowIntensity),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Simulated Camera Ring Canvas
            Box(
                modifier = Modifier.size(150.dp),
                contentAlignment = Alignment.Center
            ) {
                // Subtle ambient back-glow
                Canvas(modifier = Modifier.size(140.dp)) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val glowAlpha = if (isRunning && !isScreenPaused) (0.35f * glowIntensity * brightness).coerceIn(0f, 0.7f) else 0.05f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accentColor.copy(alpha = glowAlpha), Color.Transparent),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                }

                // Camera Cutout (center black circle) & Outer Ring
                Canvas(modifier = Modifier.size(130.dp)) {
                    val w = size.width
                    val h = size.height
                    val center = Offset(w / 2f, h / 2f)
                    val outerRadius = (w / 2f) - 6.dp.toPx()
                    val strokeWidth = 10.dp.toPx()

                    // Background camera module glass
                    drawCircle(
                        color = Color(0xFF0A0C10),
                        radius = outerRadius + 4.dp.toPx(),
                        center = center
                    )

                    if (!isRunning || isScreenPaused) {
                        // Inactive ring outline
                        drawCircle(
                            color = Color(0xFF232733),
                            radius = outerRadius,
                            center = center,
                            style = Stroke(strokeWidth)
                        )
                    } else {
                        // Active effect rendering
                        when (mode) {
                            DashDazzService.MODE_RAINBOW, DashDazzService.MODE_SPECTRUM_WAVE -> {
                                val rainbowColors = listOf(
                                    Color.Red, Color(0xFFFF7F00), Color.Yellow,
                                    Color.Green, Color.Cyan, Color.Blue,
                                    Color(0xFF8B00FF), Color.Red
                                )
                                drawCircle(
                                    brush = Brush.sweepGradient(rainbowColors, center),
                                    radius = outerRadius,
                                    center = center,
                                    style = Stroke(strokeWidth)
                                )
                            }
                            DashDazzService.MODE_BREATHE, DashDazzService.MODE_BREATHE_FADE -> {
                                val c = Color(currentColor).copy(alpha = (pulsePhase * brightness).coerceIn(0.1f, 1f))
                                drawCircle(
                                    color = c,
                                    radius = outerRadius,
                                    center = center,
                                    style = Stroke(strokeWidth)
                                )
                            }
                            DashDazzService.MODE_NEON_CYBER -> {
                                val cyberColors = listOf(
                                    Color(0xFF00E5FF), Color(0xFFFF007F),
                                    Color(0xFF00E5FF), Color(0xFFFF007F), Color(0xFF00E5FF)
                                )
                                drawCircle(
                                    brush = Brush.sweepGradient(cyberColors, center),
                                    radius = outerRadius,
                                    center = center,
                                    style = Stroke(strokeWidth)
                                )
                            }
                            DashDazzService.MODE_CANDLE -> {
                                val warmColor = Color(0xFFFF9E00).copy(alpha = (0.7f + 0.3f * pulsePhase) * brightness)
                                drawCircle(
                                    color = warmColor,
                                    radius = outerRadius,
                                    center = center,
                                    style = Stroke(strokeWidth)
                                )
                            }
                            DashDazzService.MODE_STROBE -> {
                                val strobeOn = (rotationAngle % 40f) < 20f
                                val c = if (strobeOn) Color(currentColor) else Color.Transparent
                                drawCircle(
                                    color = c,
                                    radius = outerRadius,
                                    center = center,
                                    style = Stroke(strokeWidth)
                                )
                            }
                            DashDazzService.MODE_COMET_CHASER -> {
                                drawArc(
                                    color = Color(currentColor),
                                    startAngle = rotationAngle,
                                    sweepAngle = 90f,
                                    useCenter = false,
                                    topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                                    size = Size(outerRadius * 2, outerRadius * 2),
                                    style = Stroke(strokeWidth)
                                )
                            }
                            DashDazzService.MODE_CHARGING -> {
                                val chargeColor = Color(0xFF00E676)
                                drawCircle(
                                    color = chargeColor.copy(alpha = (pulsePhase * brightness).coerceIn(0.2f, 1f)),
                                    radius = outerRadius,
                                    center = center,
                                    style = Stroke(strokeWidth)
                                )
                            }
                            else -> {
                                drawCircle(
                                    color = Color(currentColor).copy(alpha = brightness.coerceIn(0.1f, 1f)),
                                    radius = outerRadius,
                                    center = center,
                                    style = Stroke(strokeWidth)
                                )
                            }
                        }
                    }

                    // Inner camera punch hole
                    drawCircle(
                        color = Color.Black,
                        radius = outerRadius - (strokeWidth / 1.5f),
                        center = center
                    )
                    // Camera lens reflection dot
                    drawCircle(
                        color = Color(0xFF1E2330),
                        radius = 12.dp.toPx(),
                        center = center
                    )
                }
            }

            // Status Indicator Badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isRunning && !isScreenPaused) Color(0xFF00E676) else Color(0xFFFF5252))
                )
                top.yukonga.miuix.kmp.basic.Text(
                    text = if (isRunning) {
                        if (isScreenPaused) "STANDBY (SCREEN POLICY)" else "RING ACTIVE (HYPEROS 3)"
                    } else {
                        "OFFLINE"
                    },
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

/**
 * Task 1: Accent Color Picker (Neon Cyan, Cyber Purple, Electric Amber, Hyper Green, AMOLED White)
 */
@Composable
fun AccentColorPickerRow(
    selectedAccent: Int,
    onAccentSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val accents = listOf(
        Prefs.ACCENT_NEON_CYAN to "Neon Cyan",
        Prefs.ACCENT_CYBER_PURPLE to "Cyber Purple",
        Prefs.ACCENT_ELECTRIC_AMBER to "Electric Amber",
        Prefs.ACCENT_HYPER_GREEN to "Hyper Green",
        Prefs.ACCENT_AMOLED_WHITE to "AMOLED White"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        accents.forEach { (argb, name) ->
            val isSelected = selectedAccent == argb
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(argb))
                    .clickable { onAccentSelected(argb) }
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) Color.White else Color.Black.copy(alpha = 0.5f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                    )
                }
            }
        }
    }
}

@Composable
fun HyperGlowThemingCard(
    context: Context,
    modifier: Modifier = Modifier
) {
    var accentColor by remember { mutableIntStateOf(Prefs.getAccentColor(context)) }
    var amoledMode by remember { mutableStateOf(Prefs.isAmoledMode(context)) }
    var compactMode by remember { mutableStateOf(Prefs.isCompactDensity(context)) }
    var glowIntensity by remember { mutableFloatStateOf(Prefs.getGlowIntensity(context)) }

    HyperGlowCard(
        accentColor = Color(accentColor),
        glowIntensity = glowIntensity,
        isCompact = compactMode,
        isAmoled = amoledMode,
        modifier = modifier
    ) {
        top.yukonga.miuix.kmp.basic.Text(
            text = "Hyper-Glow Theming & Customization",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(accentColor)
        )
        Spacer(modifier = Modifier.height(12.dp))

        top.yukonga.miuix.kmp.basic.Text(
            text = "UI Accent Color",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        AccentColorPickerRow(
            selectedAccent = accentColor,
            onAccentSelected = {
                accentColor = it
                Prefs.setAccentColor(context, it)
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                top.yukonga.miuix.kmp.basic.Text(
                    text = "Pure AMOLED Dark Mode",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                top.yukonga.miuix.kmp.basic.Text(
                    text = "True-black background for OLED power savings",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            top.yukonga.miuix.kmp.basic.Switch(
                checked = amoledMode,
                onCheckedChange = {
                    amoledMode = it
                    Prefs.setAmoledMode(context, it)
                }
            )
        }
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                top.yukonga.miuix.kmp.basic.Text(
                    text = "Compact Card Density",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                top.yukonga.miuix.kmp.basic.Text(
                    text = "Tighter card spacing and padding",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            top.yukonga.miuix.kmp.basic.Switch(
                checked = compactMode,
                onCheckedChange = {
                    compactMode = it
                    Prefs.setCompactDensity(context, it)
                }
            )
        }
    }
}

@Composable
fun AdvancedChargingSettingsCard(
    context: Context,
    modifier: Modifier = Modifier
) {
    var chargingStyle by remember { mutableIntStateOf(Prefs.getChargingStyle(context)) }
    var lowBatteryFlash by remember { mutableStateOf(Prefs.isLowBatteryFlashEnabled(context)) }

    HyperGlowCard(
        accentColor = Color(Prefs.getAccentColor(context)),
        isAmoled = Prefs.isAmoledMode(context),
        modifier = modifier
    ) {
        top.yukonga.miuix.kmp.basic.Text(
            text = "Advanced Charging & Battery Engine",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(Prefs.getAccentColor(context))
        )
        Spacer(modifier = Modifier.height(12.dp))

        val styles = listOf(
            ChargingEngine.STYLE_BREATHE to "Adaptive Pulse",
            ChargingEngine.STYLE_GAUGE to "Fill Gauge",
            ChargingEngine.STYLE_GRADIENT to "Dynamic Gradient"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            styles.forEach { (styleId, name) ->
                val isSelected = chargingStyle == styleId
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(Prefs.getAccentColor(context)) else Color(0xFF1E2230))
                        .clickable {
                            chargingStyle = styleId
                            Prefs.setChargingStyle(context, styleId)
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    top.yukonga.miuix.kmp.basic.Text(
                        text = name,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.Black else Color.White
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                top.yukonga.miuix.kmp.basic.Text(
                    text = "Low Battery Warning Flash",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                top.yukonga.miuix.kmp.basic.Text(
                    text = "Subtle warning pulse when battery drops below 15%",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            top.yukonga.miuix.kmp.basic.Switch(
                checked = lowBatteryFlash,
                onCheckedChange = {
                    lowBatteryFlash = it
                    Prefs.setLowBatteryFlashEnabled(context, it)
                }
            )
        }
    }
}

@Composable
fun SystemAutomationsCard(
    context: Context,
    modifier: Modifier = Modifier
) {
    var quietHours by remember { mutableStateOf(Prefs.isQuietHoursEnabled(context)) }
    var dndSync by remember { mutableStateOf(Prefs.isDndSyncEnabled(context)) }
    var pocketGuard by remember { mutableStateOf(Prefs.isPocketGuardEnabled(context)) }
    var devLogger by remember { mutableStateOf(Prefs.isInternalLoggerEnabled(context)) }

    HyperGlowCard(
        accentColor = Color(Prefs.getAccentColor(context)),
        isAmoled = Prefs.isAmoledMode(context),
        modifier = modifier
    ) {
        top.yukonga.miuix.kmp.basic.Text(
            text = "System Automations & Guards",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(Prefs.getAccentColor(context))
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                top.yukonga.miuix.kmp.basic.Text(
                    text = "Quiet Hours (Night Schedule)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                top.yukonga.miuix.kmp.basic.Text(
                    text = "Mutes ring lighting automatically between 11 PM and 7 AM",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            top.yukonga.miuix.kmp.basic.Switch(
                checked = quietHours,
                onCheckedChange = {
                    quietHours = it
                    Prefs.setQuietHoursEnabled(context, it)
                }
            )
        }
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                top.yukonga.miuix.kmp.basic.Text(
                    text = "DND / Silent Mode Sync",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                top.yukonga.miuix.kmp.basic.Text(
                    text = "Suppresses ring when Do Not Disturb is active",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            top.yukonga.miuix.kmp.basic.Switch(
                checked = dndSync,
                onCheckedChange = {
                    dndSync = it
                    Prefs.setDndSyncEnabled(context, it)
                }
            )
        }
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                top.yukonga.miuix.kmp.basic.Text(
                    text = "Pocket & Proximity Guard",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                top.yukonga.miuix.kmp.basic.Text(
                    text = "Mutes ring when device is face-down or inside pocket",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            top.yukonga.miuix.kmp.basic.Switch(
                checked = pocketGuard,
                onCheckedChange = {
                    pocketGuard = it
                    Prefs.setPocketGuardEnabled(context, it)
                }
            )
        }
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                top.yukonga.miuix.kmp.basic.Text(
                    text = "LED State Watchdog Logger",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                top.yukonga.miuix.kmp.basic.Text(
                    text = "Logs hardware state transitions to internal buffer",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            top.yukonga.miuix.kmp.basic.Switch(
                checked = devLogger,
                onCheckedChange = {
                    devLogger = it
                    Prefs.setInternalLoggerEnabled(context, it)
                }
            )
        }
    }
}
