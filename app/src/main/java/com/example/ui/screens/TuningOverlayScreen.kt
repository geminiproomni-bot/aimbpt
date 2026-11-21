package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SettingsAccessibility
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AimConfigState
import com.example.model.AimHitboxBone
import com.example.model.GestureDispatchMode
import com.example.model.ReticleThemeColor
import com.example.model.TargetPriorityMode
import com.example.ui.theme.CrimsonLock
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TacticalCardBorder
import com.example.ui.theme.TacticalGreen
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceVariant
import com.example.ui.theme.TelemetryAmber
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun TuningOverlayScreen(
    config: AimConfigState,
    isServiceConnected: Boolean,
    isFloatingOverlayVisible: Boolean,
    onToggleOverlay: () -> Unit,
    onUpdateDetectionAngle: (Float) -> Unit,
    onUpdateAimSpeed: (Float) -> Unit,
    onUpdateStickyStrength: (Float) -> Unit,
    onUpdatePredictionFactor: (Float) -> Unit,
    onUpdateStickyRetention: (Boolean) -> Unit,
    onUpdateHitboxBone: (AimHitboxBone) -> Unit,
    onUpdateGestureDuration: (Long) -> Unit,
    onUpdateGestureInterval: (Long) -> Unit,
    onUpdateEnemySpeed: (Float) -> Unit,
    onUpdateAutoFireOnLock: (Boolean) -> Unit,
    onUpdateShowVectors: (Boolean) -> Unit,
    onUpdateShowHud: (Boolean) -> Unit,
    onUpdateOverlayTargets: (Boolean) -> Unit,
    onUpdateReticleColor: (ReticleThemeColor) -> Unit,
    onUpdatePriorityMode: (TargetPriorityMode) -> Unit,
    onUpdateGestureMode: (GestureDispatchMode) -> Unit,
    onApplyPreset: (Float, Float, Float, Float, AimHitboxBone, Long, GestureDispatchMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Hero Banner Card with Accessibility Overlay Launch Controls
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 700.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_aim_hud_1795204428175),
                    contentDescription = stringResource(R.string.title_app_header),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x55090D16),
                                    Color(0xDD090D16),
                                    Color(0xF8090D16)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Smart Sticky Aim & Accessibility Overlay",
                        style = MaterialTheme.typography.titleLarge,
                        color = NeonCyan
                    )
                    Text(
                        text = "قفل مغناطيسي لصيق 100% مع تنبؤ حركي بمسار الأهداف وإيماءات dispatchGesture فوق لعبتك.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { openAccessibilitySettings(context) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("tuning_open_accessibility_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isServiceConnected) TacticalGreen else TelemetryAmber,
                            contentColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SettingsAccessibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isServiceConnected) "الخدمة متصلة (إعدادات)" else "1. تفعيل خدمة Accessibility",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    FilledTonalButton(
                        onClick = onToggleOverlay,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("tuning_toggle_overlay_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFloatingOverlayVisible) "الدائرة العائمة: ظاهرة" else "الدائرة العائمة: مخفية",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(TacticalSurfaceVariant)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "أهداف تدريبية متحركة على الـ Overlay العائم",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "يرسم أهدافاً افتراضية مع رأس وصدر فوق الشاشة لاختبار الالتصاق المغناطيسي فوق لعبتك",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = config.overlayTargetsEnabled,
                        onCheckedChange = onUpdateOverlayTargets,
                        modifier = Modifier.testTag("switch_overlay_targets"),
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                    )
                }
            }
        }

        // 2. Quick Smart Sticky Presets
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 700.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = TelemetryAmber
                    )
                    Text(
                        text = "أنظمة التصويب اللصيق الجاهزة (Smart Presets)",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetChipButton(
                        title = "لصيق خارق 100%",
                        subtitle = "HEAD • Sticky 100%",
                        onClick = {
                            onApplyPreset(
                                22f,
                                26f,
                                1.0f,
                                1.05f,
                                AimHitboxBone.HEAD,
                                38L,
                                GestureDispatchMode.SLERP_CAMERA_AND_SWIPE
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("preset_ultra_sticky")
                    )
                    PresetChipButton(
                        title = "قناص ذكي",
                        subtitle = "14° • قفل + نقر",
                        onClick = {
                            onApplyPreset(
                                14f,
                                22f,
                                0.96f,
                                1.15f,
                                AimHitboxBone.HEAD,
                                35L,
                                GestureDispatchMode.AUTO_LOCK_TAP
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("preset_sniper_lock")
                    )
                    PresetChipButton(
                        title = "رادار واسع",
                        subtitle = "32° • SMART Bone",
                        onClick = {
                            onApplyPreset(
                                32f,
                                20f,
                                0.92f,
                                1.0f,
                                AimHitboxBone.SMART_AUTO,
                                50L,
                                GestureDispatchMode.RETICLE_OFFSET_SWIPE
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("preset_wide_cone")
                    )
                }
            }
        }

        // 3. Hitbox Bone Selection (HEAD / CHEST / SMART_AUTO)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 700.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = CrimsonLock
                    )
                    Text(
                        text = "نقطة الالتصاق على الهدف (Target Hitbox Bone)",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }

                AimHitboxBone.entries.forEach { bone ->
                    val selected = config.hitboxBone == bone
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) Color(0xFF331026) else TacticalSurfaceVariant)
                            .border(
                                width = 1.dp,
                                color = if (selected) CrimsonLock else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onUpdateHitboxBone(bone) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (selected) CrimsonLock else TextSecondary
                        )
                        Column {
                            Text(
                                text = bone.labelAr,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "الإزاحة الرأسية: +${bone.verticalOffsetMeters}m • رمز التتبع: [${bone.shortTag}]",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan
                            )
                        }
                    }
                }
            }
        }

        // 4. Detailed Sliders for Magnetic Sticky Adhesion, Velocity Prediction & DevAimTester
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 700.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = NeonCyan
                    )
                    Text(
                        text = "معايرة الالتصاق المغناطيسي والتنبؤ الحركي",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }

                // stickyStrength
                TuningSliderItem(
                    label = stringResource(R.string.label_sticky_strength),
                    valueText = "${(config.stickyStrength * 100f).toInt()}%",
                    value = config.stickyStrength,
                    range = 0.1f..1.0f,
                    accentColor = CrimsonLock,
                    onValueChange = onUpdateStickyStrength
                )

                // predictionFactor
                TuningSliderItem(
                    label = stringResource(R.string.label_prediction_factor),
                    valueText = String.format(Locale.US, "%.2fx", config.predictionFactor),
                    value = config.predictionFactor,
                    range = 0.0f..1.5f,
                    accentColor = TelemetryAmber,
                    onValueChange = onUpdatePredictionFactor
                )

                // detectionAngle
                TuningSliderItem(
                    label = stringResource(R.string.label_detection_angle),
                    valueText = String.format(Locale.US, "%.1f°", config.detectionAngle),
                    value = config.detectionAngle,
                    range = 5f..45f,
                    accentColor = NeonCyan,
                    onValueChange = onUpdateDetectionAngle
                )

                // aimSpeed
                TuningSliderItem(
                    label = stringResource(R.string.label_aim_speed),
                    valueText = String.format(Locale.US, "%.1f", config.aimSpeed),
                    value = config.aimSpeed,
                    range = 1f..30f,
                    accentColor = TacticalGreen,
                    onValueChange = onUpdateAimSpeed
                )

                // gestureDurationMs
                TuningSliderItem(
                    label = stringResource(R.string.label_gesture_duration),
                    valueText = "${config.gestureDurationMs} ms",
                    value = config.gestureDurationMs.toFloat(),
                    range = 20f..350f,
                    accentColor = TelemetryAmber,
                    onValueChange = { onUpdateGestureDuration(it.toLong()) }
                )

                // gestureIntervalMs
                TuningSliderItem(
                    label = "معدل تكرار الإيماءات (Dispatch Interval)",
                    valueText = "${config.gestureIntervalMs} ms",
                    value = config.gestureIntervalMs.toFloat(),
                    range = 50f..600f,
                    accentColor = TacticalGreen,
                    onValueChange = { onUpdateGestureInterval(it.toLong()) }
                )

                // enemySpeedMultiplier
                TuningSliderItem(
                    label = stringResource(R.string.label_enemy_speed),
                    valueText = String.format(Locale.US, "%.1fx", config.enemySpeedMultiplier),
                    value = config.enemySpeedMultiplier,
                    range = 0.2f..2.5f,
                    accentColor = NeonCyan,
                    onValueChange = onUpdateEnemySpeed
                )
            }
        }

        // 5. Gesture Mode & Target Priority Selection
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 700.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = CrimsonLock
                    )
                    Text(
                        text = "سلوك القفل وإيماءات التحكم (Gesture Mode)",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }

                GestureDispatchMode.entries.forEach { mode ->
                    val selected = config.gestureMode == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) Color(0xFF0A2E36) else TacticalSurfaceVariant)
                            .border(
                                width = 1.dp,
                                color = if (selected) NeonCyan else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onUpdateGestureMode(mode) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (selected) NeonCyan else TextSecondary
                        )
                        Column {
                            Text(
                                text = mode.labelAr,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = mode.descAr,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "خوارزمية اختيار الهدف (FindSmartStickyTarget)",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonCyan
                )

                TargetPriorityMode.entries.forEach { pMode ->
                    val selected = config.priorityMode == pMode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) Color(0xFF281028) else TacticalSurfaceVariant)
                            .border(
                                width = 1.dp,
                                color = if (selected) CrimsonLock else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onUpdatePriorityMode(pMode) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (selected) CrimsonLock else TextSecondary
                        )
                        Column {
                            Text(
                                text = pMode.labelAr,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = pMode.descAr,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // 6. Reticle Color Theme & Smart Toggles
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 700.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = TacticalSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "لون الدائرة وخيارات الثبات الذكي",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReticleThemeColor.entries.forEach { item ->
                        val isSelected = config.reticleColor == item
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(item.primaryColor.copy(alpha = 0.18f))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) item.primaryColor else TacticalCardBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onUpdateReticleColor(item) },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(item.primaryColor)
                                )
                                Text(
                                    text = item.hexString,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                ToggleRowItem(
                    title = "حفظ القفل اللصيق ومنع التشتت عند تقاطع الأهداف (Sticky Hysteresis)",
                    checked = config.stickyTargetRetention,
                    onCheckedChange = onUpdateStickyRetention
                )
                ToggleRowItem(
                    title = "إطلاق إيماءة نقر تلقائية فور الالتصاق بالهدف (θ ≤ 3.5°)",
                    checked = config.autoFireOnLock,
                    onCheckedChange = onUpdateAutoFireOnLock
                )
                ToggleRowItem(
                    title = "رسم متجهات مسار الإيماءة ونقطة التنبؤ الحركي",
                    checked = config.showTrajectoryVectors,
                    onCheckedChange = onUpdateShowVectors
                )
                ToggleRowItem(
                    title = "إظهار شريط قراءات الدقة والخطأ الزاوي HUD",
                    checked = config.showTelemetryHud,
                    onCheckedChange = onUpdateShowHud
                )
            }
        }
    }
}

@Composable
private fun PresetChipButton(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(TacticalSurfaceVariant)
            .border(1.dp, TacticalCardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = NeonCyan
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun TuningSliderItem(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge,
                color = accentColor,
                fontFamily = JetBrainsMonoFontFamily
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor
            )
        )
    }
}

@Composable
private fun ToggleRowItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TacticalSurfaceVariant)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
        )
    }
}
