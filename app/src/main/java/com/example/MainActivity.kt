package com.example

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AimSessionRepository
import com.example.ui.screens.SandboxScreen
import com.example.ui.screens.TelemetryCodeScreen
import com.example.ui.screens.TuningOverlayScreen
import com.example.ui.theme.CrimsonLock
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TacticalGreen
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceVariant
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.AimViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        refreshAccessibilityConnectionState()
        setContent {
            MyApplicationTheme {
                DevAimStudioApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshAccessibilityConnectionState()
    }

    private fun refreshAccessibilityConnectionState() {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val isOurServiceEnabled = enabledServices.any { info ->
            info.id.contains(packageName) || info.id.contains("DevAimAccessibilityService")
        }
        if (isOurServiceEnabled) {
            AimSessionRepository.setServiceConnected(true)
        }
    }
}

private data class NavTabItem(
    val index: Int,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

@Composable
fun DevAimStudioApp(
    aimViewModel: AimViewModel = viewModel()
) {
    val config by aimViewModel.config.collectAsStateWithLifecycle()
    val sandboxState by aimViewModel.sandboxState.collectAsStateWithLifecycle()
    val isServiceConnected by aimViewModel.isServiceConnected.collectAsStateWithLifecycle()
    val isFloatingOverlayVisible by aimViewModel.isFloatingOverlayVisible.collectAsStateWithLifecycle()
    val gestureLogs by aimViewModel.gestureLogs.collectAsStateWithLifecycle()
    val lastGesture by aimViewModel.lastGestureStroke.collectAsStateWithLifecycle()
    val totalGestures by aimViewModel.totalGesturesDispatched.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    val navItems = listOf(
        NavTabItem(
            index = 0,
            labelResId = R.string.nav_sandbox,
            selectedIcon = Icons.Filled.GpsFixed,
            unselectedIcon = Icons.Outlined.GpsFixed,
            tag = "nav_tab_sandbox"
        ),
        NavTabItem(
            index = 1,
            labelResId = R.string.nav_tuning,
            selectedIcon = Icons.Filled.Tune,
            unselectedIcon = Icons.Outlined.Tune,
            tag = "nav_tab_tuning"
        ),
        NavTabItem(
            index = 2,
            labelResId = R.string.nav_telemetry,
            selectedIcon = Icons.Filled.Code,
            unselectedIcon = Icons.Outlined.Code,
            tag = "nav_tab_telemetry"
        )
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        val isExpandedScreen = maxWidth >= 640.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ObsidianBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TacticalTopHeader(
                    autoAimEnabled = config.autoAimEnabled,
                    stickyPercent = (config.stickyStrength * 100f).toInt(),
                    hitboxTag = config.hitboxBone.shortTag,
                    isServiceConnected = isServiceConnected
                )
            },
            bottomBar = {
                if (!isExpandedScreen) {
                    NavigationBar(
                        containerColor = TacticalSurface,
                        contentColor = Color.White
                    ) {
                        navItems.forEach { item ->
                            val selected = selectedTab == item.index
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectedTab = item.index },
                                modifier = Modifier.testTag(item.tag),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelResId)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(item.labelResId),
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ObsidianBg,
                                    selectedTextColor = NeonCyan,
                                    indicatorColor = NeonCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail(
                        containerColor = TacticalSurface,
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        navItems.forEach { item ->
                            val selected = selectedTab == item.index
                            NavigationRailItem(
                                selected = selected,
                                onClick = { selectedTab = item.index },
                                modifier = Modifier.testTag(item.tag),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelResId)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(item.labelResId),
                                        fontSize = 11.sp
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ObsidianBg,
                                    selectedTextColor = NeonCyan,
                                    indicatorColor = NeonCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (selectedTab) {
                        0 -> SandboxScreen(
                            config = config,
                            sandboxState = sandboxState,
                            isServiceConnected = isServiceConnected,
                            isFloatingOverlayVisible = isFloatingOverlayVisible,
                            lastGesture = lastGesture,
                            totalGestures = totalGestures,
                            onToggleAutoAim = aimViewModel::toggleAutoAim,
                            onFireGesture = aimViewModel::dispatchManualFireGesture,
                            onSpawnEnemy = aimViewModel::spawnExtraEnemy,
                            onResetArena = aimViewModel::resetArena,
                            onToggleOverlay = aimViewModel::toggleFloatingOverlay,
                            onCameraDrag = aimViewModel::onManualCameraDrag,
                            onArenaTap = aimViewModel::onArenaTap,
                            onSwitchTarget = aimViewModel::switchTargetLock,
                            onCycleHitboxBone = { aimViewModel.cycleHitboxBone() },
                            onActivateMaxSticky = aimViewModel::activateMaxStickyMode,
                            onUpdateDetectionAngle = aimViewModel::updateDetectionAngle,
                            onUpdateAimSpeed = aimViewModel::updateAimSpeed,
                            onUpdateStickyStrength = aimViewModel::updateStickyStrength,
                            onUpdatePredictionFactor = aimViewModel::updatePredictionFactor,
                            onUpdateGestureMode = aimViewModel::updateGestureMode
                        )

                        1 -> TuningOverlayScreen(
                            config = config,
                            isServiceConnected = isServiceConnected,
                            isFloatingOverlayVisible = isFloatingOverlayVisible,
                            onToggleOverlay = aimViewModel::toggleFloatingOverlay,
                            onUpdateDetectionAngle = aimViewModel::updateDetectionAngle,
                            onUpdateAimSpeed = aimViewModel::updateAimSpeed,
                            onUpdateStickyStrength = aimViewModel::updateStickyStrength,
                            onUpdatePredictionFactor = aimViewModel::updatePredictionFactor,
                            onUpdateStickyRetention = aimViewModel::updateStickyTargetRetention,
                            onUpdateHitboxBone = aimViewModel::updateHitboxBone,
                            onUpdateGestureDuration = aimViewModel::updateGestureDuration,
                            onUpdateGestureInterval = aimViewModel::updateGestureInterval,
                            onUpdateEnemySpeed = aimViewModel::updateEnemySpeedMultiplier,
                            onUpdateAutoFireOnLock = aimViewModel::updateAutoFireOnLock,
                            onUpdateShowVectors = aimViewModel::updateShowTrajectoryVectors,
                            onUpdateShowHud = aimViewModel::updateShowTelemetryHud,
                            onUpdateOverlayTargets = aimViewModel::updateOverlayTargetsEnabled,
                            onUpdateReticleColor = aimViewModel::updateReticleColor,
                            onUpdatePriorityMode = aimViewModel::updatePriorityMode,
                            onUpdateGestureMode = aimViewModel::updateGestureMode,
                            onApplyPreset = aimViewModel::applyTacticalPreset
                        )

                        2 -> TelemetryCodeScreen(
                            config = config,
                            gestureLogs = gestureLogs,
                            totalGestures = totalGestures,
                            onDispatchTestGesture = aimViewModel::dispatchManualFireGesture,
                            onClearLogs = aimViewModel::clearLogs
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TacticalTopHeader(
    autoAimEnabled: Boolean,
    stickyPercent: Int,
    hitboxTag: String,
    isServiceConnected: Boolean
) {
    Surface(
        color = TacticalSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.title_app_header),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontSize = 16.5.sp
                )
                Text(
                    text = stringResource(R.string.subtitle_app_header),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(TacticalSurfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isServiceConnected) {
                                        TacticalGreen
                                    } else if (autoAimEnabled) {
                                        CrimsonLock
                                    } else {
                                        TextSecondary
                                    }
                                )
                        )
                        Text(
                            text = "STICKY $stickyPercent% [$hitboxTag]",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                }
            }
        }
    }
}
