package com.example.data

import com.example.model.AimConfigState
import com.example.model.AimHitboxBone
import com.example.model.GestureDispatchMode
import com.example.model.GestureExecutionStatus
import com.example.model.GestureStrokeRecord
import com.example.model.ReticleThemeColor
import com.example.model.TargetPriorityMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Shared singleton repository bridging the Compose app UI and DevAimAccessibilityService.
 */
object AimSessionRepository {

    private val _config = MutableStateFlow(AimConfigState())
    val config: StateFlow<AimConfigState> = _config.asStateFlow()

    private val _isAccessibilityServiceConnected = MutableStateFlow(false)
    val isAccessibilityServiceConnected: StateFlow<Boolean> = _isAccessibilityServiceConnected.asStateFlow()

    private val _isFloatingOverlayVisible = MutableStateFlow(true)
    val isFloatingOverlayVisible: StateFlow<Boolean> = _isFloatingOverlayVisible.asStateFlow()

    private val _gestureLogs = MutableStateFlow<List<GestureStrokeRecord>>(emptyList())
    val gestureLogs: StateFlow<List<GestureStrokeRecord>> = _gestureLogs.asStateFlow()

    private val _lastGestureStroke = MutableStateFlow<GestureStrokeRecord?>(null)
    val lastGestureStroke: StateFlow<GestureStrokeRecord?> = _lastGestureStroke.asStateFlow()

    private val _totalGesturesDispatched = MutableStateFlow(0)
    val totalGesturesDispatched: StateFlow<Int> = _totalGesturesDispatched.asStateFlow()

    private var strokeIdCounter = 1L

    // Live callback registered by DevAimAccessibilityService when active
    var serviceGestureDispatcher: ((startX: Float, startY: Float, endX: Float, endY: Float, isTap: Boolean, targetName: String, angleDeg: Float) -> Boolean)? = null

    fun setServiceConnected(connected: Boolean) {
        _isAccessibilityServiceConnected.value = connected
        if (!connected) {
            serviceGestureDispatcher = null
        }
    }

    fun setFloatingOverlayVisible(visible: Boolean) {
        _isFloatingOverlayVisible.value = visible
    }

    fun toggleFloatingOverlay() {
        _isFloatingOverlayVisible.update { !it }
    }

    fun toggleAutoAim() {
        _config.update { it.copy(autoAimEnabled = !it.autoAimEnabled) }
    }

    fun setAutoAim(enabled: Boolean) {
        _config.update { it.copy(autoAimEnabled = enabled) }
    }

    fun updateDetectionAngle(angle: Float) {
        _config.update { it.copy(detectionAngle = angle.coerceIn(5f, 45f)) }
    }

    fun updateAimSpeed(speed: Float) {
        _config.update { it.copy(aimSpeed = speed.coerceIn(1f, 30f)) }
    }

    fun updateStickyStrength(strength: Float) {
        _config.update { it.copy(stickyStrength = strength.coerceIn(0.1f, 1.0f)) }
    }

    fun updatePredictionFactor(factor: Float) {
        _config.update { it.copy(predictionFactor = factor.coerceIn(0.0f, 1.5f)) }
    }

    fun updateStickyTargetRetention(enabled: Boolean) {
        _config.update { it.copy(stickyTargetRetention = enabled) }
    }

    fun updateHitboxBone(bone: AimHitboxBone) {
        _config.update { it.copy(hitboxBone = bone) }
    }

    fun cycleHitboxBone(): AimHitboxBone {
        val next = when (_config.value.hitboxBone) {
            AimHitboxBone.HEAD -> AimHitboxBone.CHEST
            AimHitboxBone.CHEST -> AimHitboxBone.SMART_AUTO
            AimHitboxBone.SMART_AUTO -> AimHitboxBone.HEAD
        }
        _config.update { it.copy(hitboxBone = next) }
        return next
    }

    fun activateMaxStickyMode() {
        _config.update {
            it.copy(
                autoAimEnabled = true,
                stickyStrength = 1.0f,
                predictionFactor = 1.05f,
                aimSpeed = 26f,
                stickyTargetRetention = true,
                hitboxBone = AimHitboxBone.HEAD
            )
        }
    }

    fun updateGestureDuration(durationMs: Long) {
        _config.update { it.copy(gestureDurationMs = durationMs.coerceIn(20L, 350L)) }
    }

    fun updateGestureInterval(intervalMs: Long) {
        _config.update { it.copy(gestureIntervalMs = intervalMs.coerceIn(50L, 600L)) }
    }

    fun updateEnemySpeedMultiplier(multiplier: Float) {
        _config.update { it.copy(enemySpeedMultiplier = multiplier.coerceIn(0.2f, 2.5f)) }
    }

    fun updateAutoFireOnLock(enabled: Boolean) {
        _config.update { it.copy(autoFireOnLock = enabled) }
    }

    fun updateShowTrajectoryVectors(show: Boolean) {
        _config.update { it.copy(showTrajectoryVectors = show) }
    }

    fun updateShowTelemetryHud(show: Boolean) {
        _config.update { it.copy(showTelemetryHud = show) }
    }

    fun updateOverlayTargetsEnabled(enabled: Boolean) {
        _config.update { it.copy(overlayTargetsEnabled = enabled) }
    }

    fun updateReticleColor(color: ReticleThemeColor) {
        _config.update { it.copy(reticleColor = color) }
    }

    fun updatePriorityMode(mode: TargetPriorityMode) {
        _config.update { it.copy(priorityMode = mode) }
    }

    fun updateGestureMode(mode: GestureDispatchMode) {
        _config.update { it.copy(gestureMode = mode) }
    }

    fun applyTacticalPreset(
        detectionAngle: Float,
        aimSpeed: Float,
        stickyStrength: Float,
        predictionFactor: Float,
        hitboxBone: AimHitboxBone,
        gestureDurationMs: Long,
        gestureMode: GestureDispatchMode
    ) {
        _config.update {
            it.copy(
                detectionAngle = detectionAngle,
                aimSpeed = aimSpeed,
                stickyStrength = stickyStrength,
                predictionFactor = predictionFactor,
                hitboxBone = hitboxBone,
                gestureDurationMs = gestureDurationMs,
                gestureMode = gestureMode
            )
        }
    }

    fun recordGestureStroke(
        typeLabel: String,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        durationMs: Long,
        targetCodeName: String,
        angleDeg: Float,
        status: GestureExecutionStatus
    ) {
        val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        val record = GestureStrokeRecord(
            id = strokeIdCounter++,
            timeFormatted = formatter.format(Date()),
            typeLabel = typeLabel,
            startX = startX,
            startY = startY,
            endX = endX,
            endY = endY,
            durationMs = durationMs,
            targetCodeName = targetCodeName,
            angleDeg = angleDeg,
            status = status
        )
        _lastGestureStroke.value = record
        _totalGesturesDispatched.update { it + 1 }
        _gestureLogs.update { current ->
            (listOf(record) + current).take(60)
        }
    }

    fun clearLogs() {
        _gestureLogs.value = emptyList()
        _lastGestureStroke.value = null
    }
}
