package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AimSessionRepository
import com.example.model.AimConfigState
import com.example.model.AimHitboxBone
import com.example.model.EnemyTarget
import com.example.model.GestureDispatchMode
import com.example.model.GestureExecutionStatus
import com.example.model.GestureStrokeRecord
import com.example.model.Quaternion
import com.example.model.ReticleThemeColor
import com.example.model.TargetPattern
import com.example.model.TargetPriorityMode
import com.example.model.Vec3
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.tan
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class HitBurstEffect(
    val id: Long,
    val normX: Float,
    val normY: Float,
    val label: String,
    val isHeadshot: Boolean = true,
    val alpha: Float = 1f
)

data class SandboxUiState(
    val cameraPosition: Vec3 = Vec3(0f, 1.8f, 0f),
    val cameraRotation: Quaternion = Quaternion(),
    val cameraYawDeg: Float = 0f,
    val cameraPitchDeg: Float = 0f,
    val enemies: List<EnemyTarget> = emptyList(),
    val lockedTarget: EnemyTarget? = null,
    val retainedLockedEnemyId: Int? = null,
    // Dynamic inner circle normalized coordinates (0.5, 0.5 is exact screen center)
    val dynamicReticleNormX: Float = 0.5f,
    val dynamicReticleNormY: Float = 0.5f,
    val lockPrecisionPercent: Float = 100f,
    val angularErrorDeg: Float = 0f,
    val isMagneticGlued: Boolean = false,
    val hitBursts: List<HitBurstEffect> = emptyList(),
    val eliminatedCount: Int = 0,
    val headshotCount: Int = 0,
    val simulatedFps: Int = 60,
    val elapsedTimeSec: Float = 0f
)

class AimViewModel : ViewModel() {

    val config: StateFlow<AimConfigState> = AimSessionRepository.config
    val isServiceConnected: StateFlow<Boolean> = AimSessionRepository.isAccessibilityServiceConnected
    val isFloatingOverlayVisible: StateFlow<Boolean> = AimSessionRepository.isFloatingOverlayVisible
    val gestureLogs: StateFlow<List<GestureStrokeRecord>> = AimSessionRepository.gestureLogs
    val lastGestureStroke: StateFlow<GestureStrokeRecord?> = AimSessionRepository.lastGestureStroke
    val totalGesturesDispatched: StateFlow<Int> = AimSessionRepository.totalGesturesDispatched

    private val _sandboxState = MutableStateFlow(
        SandboxUiState(enemies = createDefaultEnemies())
    )
    val sandboxState: StateFlow<SandboxUiState> = _sandboxState.asStateFlow()

    private var nextEnemyId = 6
    private var nextBurstId = 1L
    private var lastAutoGestureTimestampMs = 0L

    init {
        startArenaSimulationLoop()
    }

    private fun createDefaultEnemies(): List<EnemyTarget> {
        return listOf(
            EnemyTarget(
                id = 1,
                codeName = "BOT-ALPHA",
                position = Vec3(-2.4f, 1.8f, 21f),
                phaseOffset = 0.0f,
                pattern = TargetPattern.STRAFE
            ),
            EnemyTarget(
                id = 2,
                codeName = "BOT-NEON",
                position = Vec3(1.8f, 2.1f, 18.5f),
                phaseOffset = 1.4f,
                pattern = TargetPattern.ORBIT
            ),
            EnemyTarget(
                id = 3,
                codeName = "BOT-VIPER",
                position = Vec3(0.3f, 1.6f, 24f),
                phaseOffset = 2.8f,
                pattern = TargetPattern.ZIGZAG
            ),
            EnemyTarget(
                id = 4,
                codeName = "BOT-TITAN",
                position = Vec3(-4.2f, 1.8f, 26f),
                phaseOffset = 4.1f,
                pattern = TargetPattern.RUSH
            )
        )
    }

    private fun startArenaSimulationLoop() {
        viewModelScope.launch {
            val dt = 0.016f // ~60 FPS
            while (isActive) {
                stepSimulation(dt)
                delay(16L)
            }
        }
    }

    private fun stepSimulation(dt: Float) {
        val cfg = config.value
        val state = _sandboxState.value
        val newElapsed = state.elapsedTimeSec + dt
        val camPos = state.cameraPosition

        // 1. Update Enemy 3D Positions, Instantaneous Velocities, Hitboxes, and Kalman Lead Predictions
        val speedMult = cfg.enemySpeedMultiplier
        val updatedRawEnemies = state.enemies.map { enemy ->
            val t = newElapsed * speedMult + enemy.phaseOffset
            val newPos = when (enemy.pattern) {
                TargetPattern.STRAFE -> {
                    val x = sin(t * 1.1f) * 6.8f
                    val y = 1.75f + sin(t * 2.0f) * 0.35f
                    val z = 20f + cos(t * 0.55f) * 2.2f
                    Vec3(x, y, z)
                }
                TargetPattern.ORBIT -> {
                    val x = cos(t * 0.9f) * 5.8f
                    val y = 2.0f + sin(t * 1.6f) * 0.85f
                    val z = 18.5f + sin(t * 0.9f) * 4.0f
                    Vec3(x, y, z)
                }
                TargetPattern.ZIGZAG -> {
                    val x = sin(t * 1.75f) * 5.0f
                    val y = 1.55f + abs(sin(t * 2.6f)) * 1.1f
                    val z = 22.5f + cos(t * 1.0f) * 3.0f
                    Vec3(x, y, z)
                }
                TargetPattern.RUSH -> {
                    val zCycle = 27f - ((t * 2.9f) % 12f)
                    val x = sin(t * 0.8f) * 4.4f
                    Vec3(x, 1.8f, zCycle)
                }
            }

            val instantVel = (newPos - enemy.position) * (1f / dt.coerceAtLeast(0.001f))
            // Smooth Kalman velocity estimate
            val smoothedVel = if (enemy.velocity.magnitude() < 0.01f) {
                instantVel
            } else {
                Vec3.lerp(enemy.velocity, instantVel, 0.45f)
            }
            val speedMps = smoothedVel.magnitude()

            // Determine active hitbox bone (HEAD, CHEST, or SMART_AUTO)
            val resolvedBone = when (cfg.hitboxBone) {
                AimHitboxBone.HEAD -> AimHitboxBone.HEAD
                AimHitboxBone.CHEST -> AimHitboxBone.CHEST
                AimHitboxBone.SMART_AUTO -> {
                    if (speedMps <= 4.8f || cfg.stickyStrength >= 0.80f) {
                        AimHitboxBone.HEAD
                    } else {
                        AimHitboxBone.CHEST
                    }
                }
            }

            val hitboxPos = newPos + Vec3(0f, resolvedBone.verticalOffsetMeters, 0f)
            // Kinematic velocity lead prediction compensates for target displacement during frame/gesture
            val leadTimeSec = (1.05f - cfg.stickyStrength * 0.55f) * 0.045f * cfg.predictionFactor
            val predictedPos = hitboxPos + (smoothedVel * leadTimeSec)

            enemy.copy(
                position = newPos,
                hitboxPosition = hitboxPos,
                predictedPosition = predictedPos,
                velocity = smoothedVel,
                speedMps = speedMps,
                activeHitboxTag = resolvedBone.shortTag
            )
        }

        // 2. Smart Sticky Target Selection with Hysteresis Retention (prevents target thrashing)
        val preCamForward = state.cameraRotation.forward()
        var chosenEnemyId: Int? = null

        if (cfg.autoAimEnabled && cfg.stickyTargetRetention && state.retainedLockedEnemyId != null) {
            val retainedEnemy = updatedRawEnemies.firstOrNull { it.id == state.retainedLockedEnemyId }
            if (retainedEnemy != null) {
                val retainedDir = retainedEnemy.predictedPosition - camPos
                val retainedAngle = Vec3.angle(preCamForward, retainedDir)
                // Hysteresis margin: keep lock up to 1.30x detectionAngle so it stays glued
                if (retainedAngle <= cfg.detectionAngle * 1.30f) {
                    chosenEnemyId = retainedEnemy.id
                }
            }
        }

        if (chosenEnemyId == null) {
            var bestScore = Float.MAX_VALUE
            for (enemy in updatedRawEnemies) {
                val dir = enemy.predictedPosition - camPos
                val dist = dir.magnitude().coerceAtLeast(0.1f)
                val angle = Vec3.angle(preCamForward, dir)
                if (angle <= cfg.detectionAngle) {
                    val score = when (cfg.priorityMode) {
                        TargetPriorityMode.CLOSEST_ANGLE_IN_CONE -> angle
                        TargetPriorityMode.NEAREST_DISTANCE_3D -> dist
                        TargetPriorityMode.LOWEST_HP -> enemy.health * 10f + angle
                    }
                    if (score < bestScore) {
                        bestScore = score
                        chosenEnemyId = enemy.id
                    }
                }
            }
        }

        // 3. Apply Dual-Zone Magnetic Sticky Quaternion.Slerp to playerCamera
        var newCamQuat = state.cameraRotation
        val targetToLock = updatedRawEnemies.firstOrNull { it.id == chosenEnemyId }

        if (cfg.autoAimEnabled && targetToLock != null) {
            val targetDir = (targetToLock.predictedPosition - camPos).normalized()
            val preAngleError = Vec3.angle(preCamForward, targetDir)
            val targetRot = Quaternion.lookRotation(targetDir)

            if (cfg.gestureMode != GestureDispatchMode.RETICLE_OFFSET_SWIPE) {
                // Dual-Zone Magnetic Adhesion:
                // As target enters the magnetic cone, stickyStrength boosts interpolation all the way to 1.0 (zero-lag glue!)
                val magneticProximity = (1f - (preAngleError / cfg.detectionAngle.coerceAtLeast(1f))).coerceIn(0f, 1f)
                val baseSlerp = (dt * cfg.aimSpeed).coerceIn(0.05f, 1f)
                val stickyBoost = cfg.stickyStrength * (0.68f + 0.32f * magneticProximity)
                val effectiveT = (baseSlerp + (1f - baseSlerp) * stickyBoost).coerceIn(0.08f, 1.0f)

                newCamQuat = Quaternion.slerp(state.cameraRotation, targetRot, effectiveT)
            }
        }

        // Extract updated camera basis vectors after magnetic Slerp
        val fwd = newCamQuat.forward()
        val yawRad = atan2(fwd.x.toDouble(), fwd.z.toDouble()).toFloat()
        val pitchRad = asin((-fwd.y).coerceIn(-1f, 1f).toDouble()).toFloat()

        // 4. Project all 3D enemies (Body, Head, Chest, and Predicted Lead Point) onto 2D screen [0..1]
        val halfFovRad = Math.toRadians((cfg.cameraFov * 0.5f).toDouble()).toFloat()
        val focalLen = 1f / tan(halfFovRad)
        val worldUp = Vec3(0f, 1f, 0f)
        val camRight = worldUp.cross(fwd).normalized()
        val camUp = fwd.cross(camRight).normalized()

        fun projectPoint(worldPt: Vec3): Triple<Float, Float, Boolean> {
            val rel = worldPt - camPos
            val lx = rel.dot(camRight)
            val ly = rel.dot(camUp)
            val lz = rel.dot(fwd)
            val inFront = lz > 0.5f
            val sx = if (inFront) 0.5f + (lx / lz) * focalLen * 0.5f else -1f
            val sy = if (inFront) 0.5f - (ly / lz) * focalLen * 0.5f else -1f
            return Triple(sx, sy, inFront)
        }

        val projectedEnemies = updatedRawEnemies.map { enemy ->
            val chestProj = projectPoint(enemy.position)
            val headProj = projectPoint(enemy.position + Vec3(0f, AimHitboxBone.HEAD.verticalOffsetMeters, 0f))
            val activeHitboxProj = projectPoint(enemy.hitboxPosition)
            val predictedProj = projectPoint(enemy.predictedPosition)

            val relHitbox = enemy.predictedPosition - camPos
            val dist = relHitbox.magnitude().coerceAtLeast(0.1f)
            val postAngle = Vec3.angle(fwd, relHitbox)
            val insideConeNow = postAngle <= cfg.detectionAngle * 1.05f
            val isLocked = (enemy.id == chosenEnemyId) && cfg.autoAimEnabled

            enemy.copy(
                currentAngleDeg = postAngle,
                distanceMeters = dist,
                normalizedScreenX = activeHitboxProj.first,
                normalizedScreenY = activeHitboxProj.second,
                headScreenX = headProj.first,
                headScreenY = headProj.second,
                chestScreenX = chestProj.first,
                chestScreenY = chestProj.second,
                predictedScreenX = predictedProj.first,
                predictedScreenY = predictedProj.second,
                isInFront = activeHitboxProj.third,
                isInsideCone = insideConeNow || isLocked,
                isClosestLocked = isLocked
            )
        }

        val finalLockedTarget = projectedEnemies.firstOrNull { it.isClosestLocked }

        // 5. Glue the Dynamic Inner Aim Circle directly onto the Locked Target's Hitbox!
        val newReticleX: Float
        val newReticleY: Float
        val angularError: Float
        val lockPrecision: Float
        val isGlued: Boolean

        if (cfg.autoAimEnabled && finalLockedTarget != null && finalLockedTarget.isInFront) {
            val targetScreenX = finalLockedTarget.normalizedScreenX.coerceIn(0.08f, 0.92f)
            val targetScreenY = finalLockedTarget.normalizedScreenY.coerceIn(0.08f, 0.92f)
            // With high stickyStrength, snap directly onto the projected hitbox with zero trailing lag
            val reticleSnap = ((dt * cfg.aimSpeed * 1.6f) + cfg.stickyStrength * 0.94f).coerceIn(0.35f, 1.0f)
            newReticleX = state.dynamicReticleNormX + (targetScreenX - state.dynamicReticleNormX) * reticleSnap
            newReticleY = state.dynamicReticleNormY + (targetScreenY - state.dynamicReticleNormY) * reticleSnap

            val screenPixelErr = hypot(
                (targetScreenX - newReticleX).toDouble(),
                (targetScreenY - newReticleY).toDouble()
            ).toFloat()
            angularError = if (cfg.gestureMode == GestureDispatchMode.RETICLE_OFFSET_SWIPE) {
                (screenPixelErr * cfg.cameraFov).coerceAtLeast(0f)
            } else {
                finalLockedTarget.currentAngleDeg
            }
            lockPrecision = (100f - angularError * 4.5f).coerceIn(85f, 100f)
            isGlued = angularError <= 1.2f
        } else {
            val returnLerp = (dt * cfg.aimSpeed).coerceIn(0.1f, 1f)
            newReticleX = state.dynamicReticleNormX + (0.5f - state.dynamicReticleNormX) * returnLerp
            newReticleY = state.dynamicReticleNormY + (0.5f - state.dynamicReticleNormY) * returnLerp
            angularError = 0f
            lockPrecision = 100f
            isGlued = false
        }

        // 6. Periodic Gesture Telemetry & Smart Sticky Auto-Fire
        val nowMs = System.currentTimeMillis()
        var updatedEnemiesAfterFire = projectedEnemies
        var updatedBursts = state.hitBursts.mapNotNull { b ->
            val nextAlpha = b.alpha - dt * 1.85f
            if (nextAlpha > 0.05f) b.copy(alpha = nextAlpha) else null
        }
        var newEliminatedCount = state.eliminatedCount
        var newHeadshotCount = state.headshotCount
        var nextRetainedId = if (cfg.autoAimEnabled) finalLockedTarget?.id else null

        if (cfg.autoAimEnabled && finalLockedTarget != null && nowMs - lastAutoGestureTimestampMs >= cfg.gestureIntervalMs) {
            lastAutoGestureTimestampMs = nowMs
            val startPxX = 540
            val startPxY = 1200
            val endPxX = (finalLockedTarget.normalizedScreenX * 1080f).toInt().coerceIn(40, 1040)
            val endPxY = (finalLockedTarget.normalizedScreenY * 2400f).toInt().coerceIn(80, 2320)

            val isStickyLocked = angularError <= 3.5f
            val isAutoTap = (cfg.gestureMode == GestureDispatchMode.AUTO_LOCK_TAP || cfg.autoFireOnLock) && isStickyLocked

            val hitboxLabel = "${finalLockedTarget.codeName}[${finalLockedTarget.activeHitboxTag}]"
            val dispatcher = AimSessionRepository.serviceGestureDispatcher
            if (dispatcher != null) {
                dispatcher(
                    startPxX.toFloat(),
                    startPxY.toFloat(),
                    endPxX.toFloat(),
                    endPxY.toFloat(),
                    isAutoTap,
                    hitboxLabel,
                    angularError
                )
            } else {
                AimSessionRepository.recordGestureStroke(
                    typeLabel = if (isAutoTap) "STICKY_TAP" else "STICKY_LOCK",
                    startX = startPxX,
                    startY = startPxY,
                    endX = endPxX,
                    endY = endPxY,
                    durationMs = cfg.gestureDurationMs,
                    targetCodeName = hitboxLabel,
                    angleDeg = angularError,
                    status = GestureExecutionStatus.SIMULATED_SANDBOX
                )
            }

            if (isAutoTap) {
                val isHead = finalLockedTarget.activeHitboxTag == "HEAD"
                val dmg = if (isHead) 35f else 25f
                updatedEnemiesAfterFire = projectedEnemies.map { e ->
                    if (e.id == finalLockedTarget.id) {
                        val nextHp = e.health - dmg
                        if (nextHp <= 0f) {
                            newEliminatedCount++
                            if (isHead) newHeadshotCount++
                            nextRetainedId = null // Release lock so next closest target is acquired immediately
                            updatedBursts = updatedBursts + HitBurstEffect(
                                id = nextBurstId++,
                                normX = e.normalizedScreenX,
                                normY = e.normalizedScreenY,
                                label = if (isHead) "💀 HEADSHOT ${e.codeName}!" else "ELIMINATED ${e.codeName}!",
                                isHeadshot = isHead
                            )
                            e.copy(health = e.maxHealth, phaseOffset = e.phaseOffset + 2.3f)
                        } else {
                            updatedBursts = updatedBursts + HitBurstEffect(
                                id = nextBurstId++,
                                normX = e.normalizedScreenX,
                                normY = e.normalizedScreenY,
                                label = if (isHead) "⚡ HEAD -35" else "HIT -25",
                                isHeadshot = isHead
                            )
                            e.copy(health = nextHp)
                        }
                    } else {
                        e
                    }
                }
            }
        }

        _sandboxState.value = state.copy(
            cameraRotation = newCamQuat,
            cameraYawDeg = Math.toDegrees(yawRad.toDouble()).toFloat(),
            cameraPitchDeg = Math.toDegrees(pitchRad.toDouble()).toFloat(),
            enemies = updatedEnemiesAfterFire,
            lockedTarget = finalLockedTarget,
            retainedLockedEnemyId = nextRetainedId,
            dynamicReticleNormX = newReticleX,
            dynamicReticleNormY = newReticleY,
            lockPrecisionPercent = lockPrecision,
            angularErrorDeg = angularError,
            isMagneticGlued = isGlued,
            hitBursts = updatedBursts,
            eliminatedCount = newEliminatedCount,
            headshotCount = newHeadshotCount,
            elapsedTimeSec = newElapsed
        )
    }

    /**
     * Manual camera drag with magnetic sticky resistance when passing over a locked target.
     */
    fun onManualCameraDrag(deltaX: Float, deltaY: Float) {
        val state = _sandboxState.value
        val cfg = config.value
        // When sticky aim is locked onto a target, apply magnetic resistance so manual drag doesn't accidentally slip off
        val stickyDamping = if (cfg.autoAimEnabled && state.lockedTarget != null) {
            (1f - cfg.stickyStrength * 0.65f).coerceIn(0.2f, 1f)
        } else {
            1.0f
        }
        val sensitivity = 0.14f * stickyDamping
        val newYawDeg = (state.cameraYawDeg + deltaX * sensitivity).coerceIn(-65f, 65f)
        val newPitchDeg = (state.cameraPitchDeg - deltaY * sensitivity).coerceIn(-35f, 35f)
        val newQuat = Quaternion.fromYawPitch(
            Math.toRadians(newYawDeg.toDouble()).toFloat(),
            Math.toRadians(newPitchDeg.toDouble()).toFloat()
        )
        _sandboxState.update {
            it.copy(
                cameraRotation = newQuat,
                cameraYawDeg = newYawDeg,
                cameraPitchDeg = newPitchDeg
            )
        }
    }

    /**
     * Tapping on or near any target in the 3D Arena immediately locks onto that target!
     */
    fun onArenaTap(normX: Float, normY: Float) {
        val state = _sandboxState.value
        val tappedEnemy = state.enemies
            .filter { it.isInFront }
            .minByOrNull { e ->
                hypot(
                    (e.normalizedScreenX - normX).toDouble(),
                    (e.normalizedScreenY - normY).toDouble()
                )
            }

        if (tappedEnemy != null) {
            val dist = hypot(
                (tappedEnemy.normalizedScreenX - normX).toDouble(),
                (tappedEnemy.normalizedScreenY - normY).toDouble()
            )
            if (dist <= 0.18) {
                val camPos = state.cameraPosition
                val dir = (tappedEnemy.predictedPosition - camPos).normalized()
                val snapRot = Quaternion.lookRotation(dir)
                _sandboxState.update {
                    it.copy(
                        retainedLockedEnemyId = tappedEnemy.id,
                        cameraRotation = if (config.value.gestureMode != GestureDispatchMode.RETICLE_OFFSET_SWIPE) {
                            snapRot
                        } else {
                            it.cameraRotation
                        }
                    )
                }
                AimSessionRepository.setAutoAim(true)
            }
        }
    }

    /**
     * Switches sticky lock to the next enemy in the arena.
     */
    fun switchTargetLock() {
        val state = _sandboxState.value
        if (state.enemies.isEmpty()) return
        val currentId = state.retainedLockedEnemyId ?: state.lockedTarget?.id
        val sorted = state.enemies.sortedBy { it.id }
        val nextIndex = if (currentId == null) {
            0
        } else {
            val idx = sorted.indexOfFirst { it.id == currentId }
            (idx + 1) % sorted.size
        }
        val nextEnemy = sorted[nextIndex]
        val dir = (nextEnemy.predictedPosition - state.cameraPosition).normalized()
        val snapRot = Quaternion.lookRotation(dir)

        AimSessionRepository.setAutoAim(true)
        _sandboxState.update {
            it.copy(
                retainedLockedEnemyId = nextEnemy.id,
                cameraRotation = if (config.value.gestureMode != GestureDispatchMode.RETICLE_OFFSET_SWIPE) {
                    Quaternion.slerp(it.cameraRotation, snapRot, config.value.stickyStrength.coerceAtLeast(0.7f))
                } else {
                    it.cameraRotation
                }
            )
        }
    }

    /**
     * Manual trigger button to dispatch a precision tap/fire gesture at the locked target's hitbox.
     */
    fun dispatchManualFireGesture() {
        val state = _sandboxState.value
        val cfg = config.value
        val target = state.lockedTarget ?: state.enemies.minByOrNull { it.currentAngleDeg }
        val normX = target?.normalizedScreenX?.coerceIn(0.08f, 0.92f) ?: state.dynamicReticleNormX
        val normY = target?.normalizedScreenY?.coerceIn(0.08f, 0.92f) ?: state.dynamicReticleNormY

        val pxX = (normX * 1080f).toInt()
        val pxY = (normY * 2400f).toInt()
        val hitboxTag = target?.activeHitboxTag ?: cfg.hitboxBone.shortTag
        val targetName = if (target != null) "${target.codeName}[$hitboxTag]" else "CENTER-RETICLE"
        val angle = state.angularErrorDeg

        val dispatcher = AimSessionRepository.serviceGestureDispatcher
        if (dispatcher != null) {
            dispatcher(540f, 1200f, pxX.toFloat(), pxY.toFloat(), true, targetName, angle)
        } else {
            AimSessionRepository.recordGestureStroke(
                typeLabel = "STICKY_TAP",
                startX = 540,
                startY = 1200,
                endX = pxX,
                endY = pxY,
                durationMs = cfg.gestureDurationMs,
                targetCodeName = targetName,
                angleDeg = angle,
                status = GestureExecutionStatus.SIMULATED_SANDBOX
            )
        }

        if (target != null && (target.isInsideCone || target.isClosestLocked)) {
            val isHead = hitboxTag == "HEAD"
            val damage = if (isHead) 50f else 35f
            _sandboxState.update { current ->
                var eliminatedInc = 0
                val updatedList = current.enemies.map { e ->
                    if (e.id == target.id) {
                        val nextHp = e.health - damage
                        if (nextHp <= 0f) {
                            eliminatedInc = 1
                            e.copy(health = e.maxHealth, phaseOffset = e.phaseOffset + 1.85f)
                        } else {
                            e.copy(health = nextHp)
                        }
                    } else {
                        e
                    }
                }
                val burst = HitBurstEffect(
                    id = nextBurstId++,
                    normX = normX,
                    normY = normY,
                    label = if (eliminatedInc > 0) {
                        if (isHead) "💀 HEADSHOT KILL ${target.codeName}!" else "DESTROYED ${target.codeName}!"
                    } else {
                        if (isHead) "⚡ HEADSHOT -50" else "HIT -35"
                    },
                    isHeadshot = isHead
                )
                current.copy(
                    enemies = updatedList,
                    retainedLockedEnemyId = if (eliminatedInc > 0) null else current.retainedLockedEnemyId,
                    hitBursts = current.hitBursts + burst,
                    eliminatedCount = current.eliminatedCount + eliminatedInc,
                    headshotCount = current.headshotCount + (if (eliminatedInc > 0 && isHead) 1 else 0)
                )
            }
        }
    }

    fun spawnExtraEnemy() {
        _sandboxState.update { current ->
            if (current.enemies.size >= 8) return@update current
            val id = nextEnemyId++
            val patterns = TargetPattern.entries
            val pattern = patterns[id % patterns.size]
            val newEnemy = EnemyTarget(
                id = id,
                codeName = "BOT-0$id",
                position = Vec3(((id % 5) - 2) * 2.3f, 1.8f, 19.5f + (id % 3) * 2f),
                phaseOffset = id * 1.15f,
                pattern = pattern
            )
            current.copy(enemies = current.enemies + newEnemy)
        }
    }

    fun resetArena() {
        _sandboxState.update {
            SandboxUiState(enemies = createDefaultEnemies())
        }
    }

    fun toggleAutoAim() = AimSessionRepository.toggleAutoAim()
    fun toggleFloatingOverlay() = AimSessionRepository.toggleFloatingOverlay()
    fun activateMaxStickyMode() = AimSessionRepository.activateMaxStickyMode()
    fun cycleHitboxBone() = AimSessionRepository.cycleHitboxBone()
    fun updateDetectionAngle(angle: Float) = AimSessionRepository.updateDetectionAngle(angle)
    fun updateAimSpeed(speed: Float) = AimSessionRepository.updateAimSpeed(speed)
    fun updateStickyStrength(strength: Float) = AimSessionRepository.updateStickyStrength(strength)
    fun updatePredictionFactor(factor: Float) = AimSessionRepository.updatePredictionFactor(factor)
    fun updateStickyTargetRetention(enabled: Boolean) = AimSessionRepository.updateStickyTargetRetention(enabled)
    fun updateHitboxBone(bone: AimHitboxBone) = AimSessionRepository.updateHitboxBone(bone)
    fun updateGestureDuration(ms: Long) = AimSessionRepository.updateGestureDuration(ms)
    fun updateGestureInterval(ms: Long) = AimSessionRepository.updateGestureInterval(ms)
    fun updateEnemySpeedMultiplier(mult: Float) = AimSessionRepository.updateEnemySpeedMultiplier(mult)
    fun updateAutoFireOnLock(enabled: Boolean) = AimSessionRepository.updateAutoFireOnLock(enabled)
    fun updateShowTrajectoryVectors(show: Boolean) = AimSessionRepository.updateShowTrajectoryVectors(show)
    fun updateShowTelemetryHud(show: Boolean) = AimSessionRepository.updateShowTelemetryHud(show)
    fun updateOverlayTargetsEnabled(enabled: Boolean) = AimSessionRepository.updateOverlayTargetsEnabled(enabled)
    fun updateReticleColor(color: ReticleThemeColor) = AimSessionRepository.updateReticleColor(color)
    fun updatePriorityMode(mode: TargetPriorityMode) = AimSessionRepository.updatePriorityMode(mode)
    fun updateGestureMode(mode: GestureDispatchMode) = AimSessionRepository.updateGestureMode(mode)
    fun applyTacticalPreset(
        angle: Float,
        speed: Float,
        stickyStrength: Float,
        predictionFactor: Float,
        hitboxBone: AimHitboxBone,
        durationMs: Long,
        mode: GestureDispatchMode
    ) = AimSessionRepository.applyTacticalPreset(
        angle,
        speed,
        stickyStrength,
        predictionFactor,
        hitboxBone,
        durationMs,
        mode
    )
    fun clearLogs() = AimSessionRepository.clearLogs()
}
