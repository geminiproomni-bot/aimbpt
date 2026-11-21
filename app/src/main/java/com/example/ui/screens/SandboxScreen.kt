package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsAccessibility
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AimConfigState
import com.example.model.GestureDispatchMode
import com.example.model.GestureStrokeRecord
import com.example.ui.theme.CrimsonLock
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TacticalCardBorder
import com.example.ui.theme.TacticalGreen
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceVariant
import com.example.ui.theme.TelemetryAmber
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.SandboxUiState
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun SandboxScreen(
    config: AimConfigState,
    sandboxState: SandboxUiState,
    isServiceConnected: Boolean,
    isFloatingOverlayVisible: Boolean,
    lastGesture: GestureStrokeRecord?,
    totalGestures: Int,
    onToggleAutoAim: () -> Unit,
    onFireGesture: () -> Unit,
    onSpawnEnemy: () -> Unit,
    onResetArena: () -> Unit,
    onToggleOverlay: () -> Unit,
    onCameraDrag: (Float, Float) -> Unit,
    onArenaTap: (Float, Float) -> Unit,
    onSwitchTarget: () -> Unit,
    onCycleHitboxBone: () -> Unit,
    onActivateMaxSticky: () -> Unit,
    onUpdateDetectionAngle: (Float) -> Unit,
    onUpdateAimSpeed: (Float) -> Unit,
    onUpdateStickyStrength: (Float) -> Unit,
    onUpdatePredictionFactor: (Float) -> Unit,
    onUpdateGestureMode: (GestureDispatchMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Accessibility Service & Overlay Status Banner
        AccessibilityStatusBanner(
            isServiceConnected = isServiceConnected,
            isFloatingOverlayVisible = isFloatingOverlayVisible,
            onOpenAccessibilitySettings = { openAccessibilitySettings(context) },
            onToggleOverlay = onToggleOverlay
        )

        // 2. Interactive 3D Unity DevAimTester Viewport Canvas with Magnetic Sticky Aim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 700.dp)
                .height(360.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(
                    width = if (sandboxState.isMagneticGlued) 2.dp else 1.5.dp,
                    color = if (sandboxState.isMagneticGlued) CrimsonLock else config.reticleColor.primaryColor.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp)
                )
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF060A12),
                            Color(0xFF0D1726),
                            Color(0xFF09101D)
                        )
                    )
                )
                .testTag("aim_sandbox_canvas")
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (size.width > 0 && size.height > 0) {
                            onArenaTap(offset.x / size.width.toFloat(), offset.y / size.height.toFloat())
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onCameraDrag(dragAmount.x, dragAmount.y)
                    }
                }
        ) {
            ArenaViewportCanvas(
                config = config,
                sandboxState = sandboxState,
                lastGesture = lastGesture
            )

            // Top HUD Telemetry Strip inside Viewport
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color(0xBF090D16))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    sandboxState.isMagneticGlued -> CrimsonLock
                                    config.autoAimEnabled -> TacticalGreen
                                    else -> TextSecondary
                                }
                            )
                    )
                    Text(
                        text = when {
                            sandboxState.isMagneticGlued -> "MAGNETIC GLUE 100%"
                            config.autoAimEnabled -> "STICKY AIM: ACTIVE"
                            else -> "STICKY AIM: OFF"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            sandboxState.isMagneticGlued -> CrimsonLock
                            config.autoAimEnabled -> TacticalGreen
                            else -> TextSecondary
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = String.format(
                        Locale.US,
                        "STICKY: %d%% | LEAD: %.1fx | [%s]",
                        (config.stickyStrength * 100f).toInt(),
                        config.predictionFactor,
                        config.hitboxBone.shortTag
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan
                )

                Text(
                    text = "KILLS: ${sandboxState.eliminatedCount} (💀${sandboxState.headshotCount})",
                    style = MaterialTheme.typography.labelSmall,
                    color = TelemetryAmber,
                    fontWeight = FontWeight.Bold
                )
            }

            // Bottom HUD Readout inside Viewport
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color(0xD8090D16))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val locked = sandboxState.lockedTarget
                Text(
                    text = if (locked != null) {
                        String.format(
                            Locale.US,
                            "🎯 %s [%s] | الخطأ: %.1f° | الدقة: %.1f%%",
                            locked.codeName,
                            locked.activeHitboxTag,
                            sandboxState.angularErrorDeg,
                            sandboxState.lockPrecisionPercent
                        )
                    } else {
                        "انقر على أي هدف للقفل الفوري • أو اسحب الكاميرا نحو الأهداف"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (locked != null) CrimsonLock else TextSecondary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "GESTURES: $totalGestures",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan
                )
            }
        }

        // 3. Smart Sticky Diagnostics & Instant Boost Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 700.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Row: Hitbox Selector + Switch Target + Max Sticky 100% Booster
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = onCycleHitboxBone,
                        modifier = Modifier
                            .weight(1.1f)
                            .height(48.dp)
                            .testTag("cycle_hitbox_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "نقطة القفل: ${config.hitboxBone.shortTag}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }

                    OutlinedButton(
                        onClick = onSwitchTarget,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("switch_target_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "تبديل الهدف",
                            modifier = Modifier.size(18.dp),
                            tint = TelemetryAmber
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تبديل الهدف",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = onActivateMaxSticky,
                        modifier = Modifier
                            .weight(1.15f)
                            .height(48.dp)
                            .testTag("max_sticky_boost_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TelemetryAmber,
                            contentColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "قفل لصيق 100%",
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "لصيق 100%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Second Row: Toggle Sticky Aim & Manual Fire Gesture
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val autoBtnColor by animateColorAsState(
                        targetValue = if (config.autoAimEnabled) NeonCyan else TacticalSurfaceVariant,
                        label = "autoAimBtnColor"
                    )
                    Button(
                        onClick = onToggleAutoAim,
                        modifier = Modifier
                            .weight(1.15f)
                            .height(50.dp)
                            .testTag("toggle_auto_aim_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = autoBtnColor,
                            contentColor = if (config.autoAimEnabled) ObsidianBg else Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = stringResource(R.string.btn_auto_aim_on),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (config.autoAimEnabled) {
                                stringResource(R.string.btn_auto_aim_on)
                            } else {
                                stringResource(R.string.btn_auto_aim_off)
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = onFireGesture,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("fire_gesture_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrimsonLock,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = stringResource(R.string.btn_fire_gesture),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إيماءة نقر (Tap)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Third Row: Mode Switcher + Spawn Enemy + Reset Arena
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            val nextMode = when (config.gestureMode) {
                                GestureDispatchMode.SLERP_CAMERA_AND_SWIPE -> GestureDispatchMode.RETICLE_OFFSET_SWIPE
                                GestureDispatchMode.RETICLE_OFFSET_SWIPE -> GestureDispatchMode.AUTO_LOCK_TAP
                                GestureDispatchMode.AUTO_LOCK_TAP -> GestureDispatchMode.SLERP_CAMERA_AND_SWIPE
                            }
                            onUpdateGestureMode(nextMode)
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp)
                            .testTag("cycle_gesture_mode_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (config.gestureMode) {
                                GestureDispatchMode.SLERP_CAMERA_AND_SWIPE -> "الوضع: قفل الكاميرا + الدائرة"
                                GestureDispatchMode.RETICLE_OFFSET_SWIPE -> "الوضع: التصاق الدائرة فقط"
                                GestureDispatchMode.AUTO_LOCK_TAP -> "الوضع: قفل لصيق + نقر تلقائي"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onSpawnEnemy,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("spawn_enemy_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.btn_spawn_enemy),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(R.string.btn_spawn_enemy), fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onResetArena,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("reset_arena_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.btn_reset_arena),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 4. Live Smart Sticky Calibration Sliders (Sticky Adhesion %, Lead Prediction, FOV Angle, Aim Speed)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 700.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sticky Strength Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.label_sticky_strength),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "${(config.stickyStrength * 100f).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = CrimsonLock,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                }
                Slider(
                    value = config.stickyStrength,
                    onValueChange = onUpdateStickyStrength,
                    valueRange = 0.1f..1.0f,
                    modifier = Modifier.testTag("slider_sticky_strength"),
                    colors = SliderDefaults.colors(
                        thumbColor = CrimsonLock,
                        activeTrackColor = CrimsonLock
                    )
                )

                // Velocity Lead Prediction Factor
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.label_prediction_factor),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = String.format(Locale.US, "%.2fx", config.predictionFactor),
                        style = MaterialTheme.typography.labelLarge,
                        color = TelemetryAmber,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                }
                Slider(
                    value = config.predictionFactor,
                    onValueChange = onUpdatePredictionFactor,
                    valueRange = 0.0f..1.5f,
                    modifier = Modifier.testTag("slider_prediction_factor"),
                    colors = SliderDefaults.colors(
                        thumbColor = TelemetryAmber,
                        activeTrackColor = TelemetryAmber
                    )
                )

                // detectionAngle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.label_detection_angle),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f°", config.detectionAngle),
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonCyan,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                }
                Slider(
                    value = config.detectionAngle,
                    onValueChange = onUpdateDetectionAngle,
                    valueRange = 5f..45f,
                    modifier = Modifier.testTag("slider_detection_angle"),
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan
                    )
                )

                // aimSpeed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.label_aim_speed),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f", config.aimSpeed),
                        style = MaterialTheme.typography.labelLarge,
                        color = TacticalGreen,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                }
                Slider(
                    value = config.aimSpeed,
                    onValueChange = onUpdateAimSpeed,
                    valueRange = 1f..30f,
                    modifier = Modifier.testTag("slider_aim_speed"),
                    colors = SliderDefaults.colors(
                        thumbColor = TacticalGreen,
                        activeTrackColor = TacticalGreen
                    )
                )
            }
        }
    }
}

@Composable
private fun AccessibilityStatusBanner(
    isServiceConnected: Boolean,
    isFloatingOverlayVisible: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onToggleOverlay: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 700.dp),
        color = if (isServiceConnected) Color(0xFF062E26) else TacticalSurfaceVariant,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isServiceConnected) TacticalGreen else TacticalCardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SettingsAccessibility,
                    contentDescription = null,
                    tint = if (isServiceConnected) TacticalGreen else TelemetryAmber,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = if (isServiceConnected) {
                            stringResource(R.string.status_service_active)
                        } else {
                            stringResource(R.string.status_service_inactive)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                    Text(
                        text = if (isServiceConnected) {
                            "قفل مغناطيسي عائم + تنبؤ حركي + dispatchGesture نشط فوق التطبيقات"
                        } else {
                            "التصويب اللصيق الذكي يعمل بالأسفل • فعّل Accessibility للعمل فوق لعبتك"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isServiceConnected) {
                FilledTonalButton(
                    onClick = onToggleOverlay,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("toggle_overlay_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isFloatingOverlayVisible) "إخفاء الدائرة" else "إظهار الدائرة",
                        fontSize = 12.sp
                    )
                }
            } else {
                Button(
                    onClick = onOpenAccessibilitySettings,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("open_accessibility_settings_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TelemetryAmber,
                        contentColor = ObsidianBg
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.btn_enable_accessibility),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ArenaViewportCanvas(
    config: AimConfigState,
    sandboxState: SandboxUiState,
    lastGesture: GestureStrokeRecord?
) {
    val textMeasurer = rememberTextMeasurer()
    val reticlePrimaryColor = config.reticleColor.primaryColor

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val centerX = w / 2f
        val centerY = h / 2f
        val minDim = min(w, h)

        // 1. Draw 3D Perspective Horizon & Cyber Grid Floor
        val horizonY = h * 0.52f
        drawLine(
            color = reticlePrimaryColor.copy(alpha = 0.25f),
            start = Offset(0f, horizonY),
            end = Offset(w, horizonY),
            strokeWidth = 2f
        )

        val vanishX = centerX - (sandboxState.cameraYawDeg / 65f) * (w * 0.35f)
        for (i in -6..6) {
            val bottomX = vanishX + i * (w * 0.22f)
            drawLine(
                color = reticlePrimaryColor.copy(alpha = 0.12f),
                start = Offset(vanishX, horizonY),
                end = Offset(bottomX, h),
                strokeWidth = 1.5f
            )
        }
        val depthSteps = listOf(0.57f, 0.64f, 0.73f, 0.85f, 0.96f)
        for (ratio in depthSteps) {
            val y = h * ratio
            drawLine(
                color = reticlePrimaryColor.copy(alpha = 0.14f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.2f
            )
        }

        // 2. Calculate Outer FOV Cone Radius and Inner Magnetic Sticky Zone Radius
        val fovRadiusPx = ((config.detectionAngle / (config.cameraFov * 0.5f))
            .coerceIn(0.12f, 0.90f)) * (minDim * 0.46f)
        val magneticZoneRadiusPx = fovRadiusPx * 0.58f

        // Outer FOV Circle
        drawCircle(
            color = reticlePrimaryColor.copy(alpha = 0.07f),
            radius = fovRadiusPx,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = reticlePrimaryColor.copy(alpha = 0.85f),
            radius = fovRadiusPx,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2.5.dp.toPx())
        )

        // Inner Magnetic Sticky Snap Zone Circle (dashed)
        drawCircle(
            color = CrimsonLock.copy(alpha = 0.38f),
            radius = magneticZoneRadiusPx,
            center = Offset(centerX, centerY),
            style = Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
            )
        )

        // Rotating radar ticks around the FOV Circle
        val radarAngleBase = (sandboxState.elapsedTimeSec * 50f) % 360f
        for (i in 0 until 8) {
            val aRad = Math.toRadians((radarAngleBase + i * 45f).toDouble())
            val innerR = fovRadiusPx - 6.dp.toPx()
            val outerR = fovRadiusPx + 6.dp.toPx()
            drawLine(
                color = reticlePrimaryColor,
                start = Offset(
                    centerX + cos(aRad).toFloat() * innerR,
                    centerY + sin(aRad).toFloat() * innerR
                ),
                end = Offset(
                    centerX + cos(aRad).toFloat() * outerR,
                    centerY + sin(aRad).toFloat() * outerR
                ),
                strokeWidth = 2.dp.toPx()
            )
        }

        // Center Fixed Crosshair Reference Ticks
        val crossLen = 10.dp.toPx()
        drawLine(
            color = reticlePrimaryColor.copy(alpha = 0.65f),
            start = Offset(centerX - crossLen, centerY),
            end = Offset(centerX + crossLen, centerY),
            strokeWidth = 1.5.dp.toPx()
        )
        drawLine(
            color = reticlePrimaryColor.copy(alpha = 0.65f),
            start = Offset(centerX, centerY - crossLen),
            end = Offset(centerX, centerY + crossLen),
            strokeWidth = 1.5.dp.toPx()
        )

        // 3. Draw 3D Enemies with Anatomical Skeletons (Head Hitbox + Chest Torso + Predicted Lead Point)
        for (enemy in sandboxState.enemies) {
            if (!enemy.isInFront) continue
            val chestX = enemy.chestScreenX * w
            val chestY = enemy.chestScreenY * h
            val headX = enemy.headScreenX * w
            val headY = enemy.headScreenY * h
            val activeX = enemy.normalizedScreenX * w
            val activeY = enemy.normalizedScreenY * h
            val predX = enemy.predictedScreenX * w
            val predY = enemy.predictedScreenY * h

            if (chestX < -70f || chestX > w + 70f || chestY < -70f || chestY > h + 70f) continue

            val scale = (22f / enemy.distanceMeters.coerceAtLeast(8f)).coerceIn(0.65f, 1.7f)
            val bodyRadius = 18.dp.toPx() * scale
            val headRadius = 9.5.dp.toPx() * scale

            val enemyColor = when {
                enemy.isClosestLocked -> CrimsonLock
                enemy.isInsideCone -> TelemetryAmber
                else -> NeonCyan.copy(alpha = 0.65f)
            }

            // Spine connecting Head and Chest
            drawLine(
                color = enemyColor.copy(alpha = 0.6f),
                start = Offset(headX, headY),
                end = Offset(chestX, chestY + bodyRadius * 0.7f),
                strokeWidth = 2.dp.toPx()
            )

            // Torso / Chest Hitbox Ring
            drawCircle(
                color = enemyColor.copy(alpha = 0.14f),
                radius = bodyRadius,
                center = Offset(chestX, chestY)
            )
            drawCircle(
                color = enemyColor,
                radius = bodyRadius,
                center = Offset(chestX, chestY),
                style = Stroke(width = if (enemy.isClosestLocked) 2.5.dp.toPx() else 1.6.dp.toPx())
            )

            // Head Hitbox Ring
            drawCircle(
                color = if (enemy.activeHitboxTag == "HEAD" && enemy.isClosestLocked) {
                    CrimsonLock.copy(alpha = 0.35f)
                } else {
                    enemyColor.copy(alpha = 0.2f)
                },
                radius = headRadius,
                center = Offset(headX, headY)
            )
            drawCircle(
                color = if (enemy.activeHitboxTag == "HEAD" && enemy.isClosestLocked) CrimsonLock else enemyColor,
                radius = headRadius,
                center = Offset(headX, headY),
                style = Stroke(width = if (enemy.isClosestLocked) 2.8.dp.toPx() else 1.8.dp.toPx())
            )

            // Active Hitbox Core Point
            drawCircle(
                color = Color.White,
                radius = 3.2.dp.toPx(),
                center = Offset(activeX, activeY)
            )

            // Predicted Velocity Lead Ghost Indicator
            if (enemy.isClosestLocked && config.predictionFactor > 0.05f) {
                drawLine(
                    color = ElectricBlue.copy(alpha = 0.8f),
                    start = Offset(activeX, activeY),
                    end = Offset(predX, predY),
                    strokeWidth = 1.8.dp.toPx()
                )
                drawCircle(
                    color = ElectricBlue,
                    radius = 5.dp.toPx(),
                    center = Offset(predX, predY),
                    style = Stroke(width = 1.8.dp.toPx())
                )
            }

            // Health bar above enemy head
            val barWidth = bodyRadius * 2.0f
            val barHeight = 4.5.dp.toPx()
            val barLeft = headX - barWidth / 2f
            val barTop = headY - headRadius - 12.dp.toPx()
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.7f),
                topLeft = Offset(barLeft, barTop),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(2f, 2f)
            )
            drawRoundRect(
                color = if (enemy.isClosestLocked) CrimsonLock else TacticalGreen,
                topLeft = Offset(barLeft, barTop),
                size = Size(barWidth * (enemy.health / enemy.maxHealth).coerceIn(0f, 1f), barHeight),
                cornerRadius = CornerRadius(2f, 2f)
            )

            // Enemy Label, Active Bone & Angle Readout
            val labelText = String.format(
                Locale.US,
                "%s [%s %.1f°]",
                enemy.codeName,
                enemy.activeHitboxTag,
                enemy.currentAngleDeg
            )
            val textLayout = textMeasurer.measure(
                text = labelText,
                style = TextStyle(
                    color = enemyColor,
                    fontSize = 10.sp,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                )
            )
            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(headX - textLayout.size.width / 2f, barTop - textLayout.size.height - 3f)
            )
        }

        // 4. Draw Zero-Lag Sticky Lock Reticle Glued onto the Target Hitbox!
        val dynX = sandboxState.dynamicReticleNormX * w
        val dynY = sandboxState.dynamicReticleNormY * h
        val isLocked = sandboxState.lockedTarget != null && config.autoAimEnabled

        if (config.showTrajectoryVectors && isLocked) {
            drawLine(
                color = CrimsonLock,
                start = Offset(centerX, centerY),
                end = Offset(dynX, dynY),
                strokeWidth = 2.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )
        }

        val dynamicCircleColor = if (isLocked) CrimsonLock else reticlePrimaryColor
        val dynamicRadius = if (isLocked) 20.dp.toPx() else 15.dp.toPx()

        drawCircle(
            color = dynamicCircleColor.copy(alpha = 0.24f),
            radius = dynamicRadius,
            center = Offset(dynX, dynY)
        )
        drawCircle(
            color = dynamicCircleColor,
            radius = dynamicRadius,
            center = Offset(dynX, dynY),
            style = Stroke(width = 3.dp.toPx())
        )

        // Tactical magnetic lock corner brackets when glued onto enemy hitbox
        if (isLocked) {
            val bSize = dynamicRadius * 0.78f
            drawRoundRect(
                color = TelemetryAmber.copy(alpha = 0.9f),
                topLeft = Offset(dynX - bSize, dynY - bSize),
                size = Size(bSize * 2f, bSize * 2f),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                style = Stroke(width = 1.8.dp.toPx())
            )
        }

        drawCircle(
            color = Color.White,
            radius = 4.dp.toPx(),
            center = Offset(dynX, dynY)
        )

        // 5. Draw Hit Bursts
        for (burst in sandboxState.hitBursts) {
            val bx = burst.normX * w
            val by = burst.normY * h
            val burstColor = if (burst.isHeadshot) CrimsonLock else TelemetryAmber
            drawCircle(
                color = burstColor.copy(alpha = burst.alpha),
                radius = (1.3f - burst.alpha) * 46.dp.toPx(),
                center = Offset(bx, by),
                style = Stroke(width = 2.8.dp.toPx())
            )
            val burstText = textMeasurer.measure(
                text = burst.label,
                style = TextStyle(
                    color = burstColor.copy(alpha = burst.alpha),
                    fontSize = 11.sp,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                )
            )
            drawText(
                textLayoutResult = burstText,
                topLeft = Offset(bx - burstText.size.width / 2f, by - 38.dp.toPx())
            )
        }

        // 6. Live Gesture Stroke Vector Overlay
        if (config.showTelemetryHud && lastGesture != null) {
            val gText = "STROKE: ${lastGesture.typeLabel} (${lastGesture.startX},${lastGesture.startY}) → (${lastGesture.endX},${lastGesture.endY}) | Δθ=${String.format(Locale.US, "%.1f°", lastGesture.angleDeg)}"
            val gLayout = textMeasurer.measure(
                text = gText,
                style = TextStyle(
                    color = NeonCyan.copy(alpha = 0.88f),
                    fontSize = 9.sp,
                    fontFamily = JetBrainsMonoFontFamily
                )
            )
            drawText(
                textLayoutResult = gLayout,
                topLeft = Offset(12.dp.toPx(), 38.dp.toPx())
            )
        }
    }
}

fun openAccessibilitySettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}
