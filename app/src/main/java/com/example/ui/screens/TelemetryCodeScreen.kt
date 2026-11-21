package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AimCodeGenerator
import com.example.model.AimConfigState
import com.example.model.GestureExecutionStatus
import com.example.model.GestureStrokeRecord
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
fun TelemetryCodeScreen(
    config: AimConfigState,
    gestureLogs: List<GestureStrokeRecord>,
    totalGestures: Int,
    onDispatchTestGesture: () -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showUnityTab by remember { mutableStateOf(true) }

    val generatedUnityCode = remember(config) {
        AimCodeGenerator.generateUnityCSharpCode(config)
    }
    val generatedAndroidCode = remember(config) {
        AimCodeGenerator.generateAndroidAccessibilitySnippet(config)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Live Gesture Telemetry Card (AccessibilityService GestureDescription Log)
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = NeonCyan
                        )
                        Column {
                            Text(
                                text = "سجل إيماءات AccessibilityService",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = "إجمالي الإيماءات المنفذة: $totalGestures stroke(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = onDispatchTestGesture,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("telemetry_test_gesture_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "اختبار إيماءة", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onClearLogs,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("clear_logs_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = stringResource(R.string.label_clear_logs),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (gestureLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(TacticalSurfaceVariant)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد إيماءات مسجلة حالياً. فعّل التصويب التلقائي أو اضغط (اختبار إيماءة).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        gestureLogs.take(8).forEach { item ->
                            GestureLogRow(record = item)
                        }
                    }
                }
            }
        }

        // 2. Synced Unity C# (DevAimTester.cs) & Android AccessibilityService Code Viewer
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = TelemetryAmber
                        )
                        Text(
                            text = "الكود البرمجي المزامن مع إعداداتك",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showUnityTab = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("tab_unity_code_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showUnityTab) NeonCyan else TacticalSurfaceVariant,
                            contentColor = if (showUnityTab) ObsidianBg else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Unity C# (DevAimTester)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = { showUnityTab = false },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("tab_android_code_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!showUnityTab) CrimsonLock else TacticalSurfaceVariant,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Kotlin (dispatchGesture)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                val activeCode = if (showUnityTab) generatedUnityCode else generatedAndroidCode

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF06090F))
                        .border(1.dp, TacticalCardBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    Text(
                        text = activeCode,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = Color(0xFFE2E8F0)
                    )
                }

                Button(
                    onClick = {
                        copyToClipboard(context, activeCode)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("copy_code_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = ObsidianBg
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.label_copy_unity_code),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.label_copy_unity_code),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun GestureLogRow(record: GestureStrokeRecord) {
    val badgeColor = when (record.status) {
        GestureExecutionStatus.DISPATCHED_ACCESSIBILITY -> TacticalGreen
        GestureExecutionStatus.SIMULATED_SANDBOX -> NeonCyan
        GestureExecutionStatus.CANCELLED -> CrimsonLock
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(TacticalSurfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "#${record.id} ${record.typeLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "🎯 ${record.targetCodeName} (${String.format(Locale.US, "%.1f°", record.angleDeg)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
            Text(
                text = "Path: (${record.startX}, ${record.startY}) → (${record.endX}, ${record.endY}) • ${record.durationMs}ms",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = record.status.badge,
                style = MaterialTheme.typography.labelSmall,
                color = badgeColor,
                fontSize = 9.sp
            )
            Text(
                text = record.timeFormatted,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 9.sp
            )
        }
    }
}

private fun copyToClipboard(context: Context, code: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("DevAimTester Code", code))
    Toast.makeText(context, "تم نسخ الكود البرمجي إلى الحافظة", Toast.LENGTH_SHORT).show()
}
