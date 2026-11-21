package com.example.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View
import com.example.data.AimSessionRepository
import com.example.model.AimHitboxBone
import java.util.Locale
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class OverlayVirtualTarget(
    val id: Int,
    val label: String,
    var normX: Float,
    var normY: Float,
    var velX: Float,
    var velY: Float
)

private data class OverlayCandidate(
    val label: String,
    val hitboxX: Float,
    val hitboxY: Float,
    val predictedX: Float,
    val predictedY: Float,
    val bodyX: Float,
    val bodyY: Float,
    val headX: Float,
    val headY: Float
)

/**
 * Full-screen pass-through overlay canvas drawn by DevAimAccessibilityService
 * using TYPE_ACCESSIBILITY_OVERLAY (FLAG_NOT_TOUCHABLE | FLAG_NOT_FOCUSABLE).
 * Implements Smart Sticky Aim with Velocity Lead Prediction and Hysteresis Target Retention.
 */
class FloatingAimReticleView(
    context: Context,
    private val onTargetLockedTick: (
        centerX: Float,
        centerY: Float,
        targetX: Float,
        targetY: Float,
        targetLabel: String,
        angleDeg: Float
    ) -> Unit
) : View(context) {

    private val fovPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#00F5D4")
    }

    private val magneticZonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.argb(110, 255, 42, 133)
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val fovFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(18, 0, 245, 212)
    }

    private val dashedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
        color = Color.parseColor("#FF2A85")
        pathEffect = DashPathEffect(floatArrayOf(14f, 8f), 0f)
    }

    private val lockCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.parseColor("#FF2A85")
    }

    private val targetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
        color = Color.parseColor("#F59E0B")
    }

    private val leadGhostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        color = Color.parseColor("#00F5D4")
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 25f
        typeface = Typeface.MONOSPACE
    }

    private val hudBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(205, 9, 13, 22)
    }

    private val virtualTargets = mutableListOf(
        OverlayVirtualTarget(1, "UNITY-BOT-1", 0.45f, 0.47f, 0.085f, 0.055f),
        OverlayVirtualTarget(2, "UNITY-BOT-2", 0.56f, 0.52f, -0.095f, 0.065f),
        OverlayVirtualTarget(3, "UNITY-BOT-3", 0.38f, 0.58f, 0.075f, -0.08f)
    )

    private var reticleX = -1f
    private var reticleY = -1f
    private var currentLockedLabel: String? = null
    private var radarPhase = 0f
    private var lastFrameTimeMs = System.currentTimeMillis()
    private var lastGestureDispatchMs = 0L

    // External Accessibility Node target coordinates detected from active window
    var externalNodeTargets: List<Pair<Float, Float>> = emptyList()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val now = System.currentTimeMillis()
        val dt = ((now - lastFrameTimeMs).coerceIn(1L, 100L)) / 1000f
        lastFrameTimeMs = now

        val config = AimSessionRepository.config.value
        val centerX = w / 2f
        val centerY = h / 2f

        if (reticleX < 0f || reticleY < 0f) {
            reticleX = centerX
            reticleY = centerY
        }

        val minDim = kotlin.math.min(w, h)
        val fovRadiusPx = (config.detectionAngle / (config.cameraFov * 0.5f))
            .coerceIn(0.12f, 0.85f) * (minDim * 0.46f)
        val magneticInnerRadiusPx = fovRadiusPx * 0.58f

        val primaryColorInt = Color.parseColor(config.reticleColor.hexString)
        fovPaint.color = primaryColorInt
        fovFillPaint.color = Color.argb(
            20,
            Color.red(primaryColorInt),
            Color.green(primaryColorInt),
            Color.blue(primaryColorInt)
        )

        val headOffsetY = when (config.hitboxBone) {
            AimHitboxBone.HEAD -> -22f
            AimHitboxBone.CHEST -> 0f
            AimHitboxBone.SMART_AUTO -> -15f
        }

        // 1. Update virtual targets & build predictive candidates
        val candidates = mutableListOf<OverlayCandidate>()
        if (config.overlayTargetsEnabled) {
            val speedMult = config.enemySpeedMultiplier
            for (t in virtualTargets) {
                t.normX += t.velX * dt * speedMult
                t.normY += t.velY * dt * speedMult
                if (t.normX < 0.20f || t.normX > 0.80f) t.velX = -t.velX
                if (t.normY < 0.26f || t.normY > 0.74f) t.velY = -t.velY
                t.normX = t.normX.coerceIn(0.20f, 0.80f)
                t.normY = t.normY.coerceIn(0.26f, 0.74f)

                val bodyX = t.normX * w
                val bodyY = t.normY * h
                val headX = bodyX
                val headY = bodyY - 22f
                val hitX = bodyX
                val hitY = bodyY + headOffsetY

                // Velocity lead prediction in pixels
                val leadTimeSec = 0.045f * config.predictionFactor
                val predX = hitX + (t.velX * w * speedMult) * leadTimeSec
                val predY = hitY + (t.velY * h * speedMult) * leadTimeSec

                candidates.add(
                    OverlayCandidate(
                        label = t.label,
                        hitboxX = hitX,
                        hitboxY = hitY,
                        predictedX = predX,
                        predictedY = predY,
                        bodyX = bodyX,
                        bodyY = bodyY,
                        headX = headX,
                        headY = headY
                    )
                )
            }
        }

        externalNodeTargets.forEachIndexed { idx, pair ->
            val bx = pair.first
            val by = pair.second
            val hx = bx
            val hy = by + headOffsetY
            candidates.add(
                OverlayCandidate(
                    label = "NODE-${idx + 1}",
                    hitboxX = hx,
                    hitboxY = hy,
                    predictedX = hx,
                    predictedY = hy,
                    bodyX = bx,
                    bodyY = by,
                    headX = bx,
                    headY = by - 20f
                )
            )
        }

        // 2. Smart Sticky Target Selection with Hysteresis Retention
        var lockedCandidate: OverlayCandidate? = null
        var bestAngleDeg = config.detectionAngle

        if (config.stickyTargetRetention && currentLockedLabel != null) {
            val retained = candidates.firstOrNull { it.label == currentLockedLabel }
            if (retained != null) {
                val distPx = hypot(
                    (retained.hitboxX - centerX).toDouble(),
                    (retained.hitboxY - centerY).toDouble()
                ).toFloat()
                if (distPx <= fovRadiusPx * 1.25f) {
                    lockedCandidate = retained
                    bestAngleDeg = ((distPx / fovRadiusPx) * config.detectionAngle).coerceAtMost(config.detectionAngle)
                }
            }
        }

        if (lockedCandidate == null) {
            for (cand in candidates) {
                val distPx = hypot(
                    (cand.hitboxX - centerX).toDouble(),
                    (cand.hitboxY - centerY).toDouble()
                ).toFloat()
                val equivAngle = (distPx / fovRadiusPx) * config.detectionAngle
                if (distPx <= fovRadiusPx && equivAngle < bestAngleDeg) {
                    bestAngleDeg = equivAngle
                    lockedCandidate = cand
                }
            }
        }

        currentLockedLabel = if (config.autoAimEnabled) lockedCandidate?.label else null

        // 3. Draw Outer FOV Circle & Inner Magnetic Sticky Zone
        canvas.drawCircle(centerX, centerY, fovRadiusPx, fovFillPaint)
        canvas.drawCircle(centerX, centerY, fovRadiusPx, fovPaint)
        canvas.drawCircle(centerX, centerY, magneticInnerRadiusPx, magneticZonePaint)

        radarPhase = (radarPhase + dt * 55f) % 360f
        for (i in 0 until 4) {
            val angRad = Math.toRadians((radarPhase + i * 90f).toDouble())
            val x1 = centerX + cos(angRad).toFloat() * (fovRadiusPx - 14f)
            val y1 = centerY + sin(angRad).toFloat() * (fovRadiusPx - 14f)
            val x2 = centerX + cos(angRad).toFloat() * (fovRadiusPx + 14f)
            val y2 = centerY + sin(angRad).toFloat() * (fovRadiusPx + 14f)
            canvas.drawLine(x1, y1, x2, y2, fovPaint)
        }

        val crossSize = 18f
        canvas.drawLine(centerX - crossSize, centerY, centerX + crossSize, centerY, fovPaint)
        canvas.drawLine(centerX, centerY - crossSize, centerX, centerY + crossSize, fovPaint)

        // 4. Draw Candidate Targets with Head + Body Skeleton Hitboxes
        for (cand in candidates) {
            val isLocked = lockedCandidate != null && lockedCandidate.label == cand.label && config.autoAimEnabled
            targetPaint.color = if (isLocked) Color.parseColor("#FF2A85") else Color.parseColor("#F59E0B")
            // Body ring
            canvas.drawCircle(cand.bodyX, cand.bodyY, 24f, targetPaint)
            // Head hitbox ring
            canvas.drawCircle(cand.headX, cand.headY, 12f, targetPaint)
            // Predicted lead ghost dot
            if (isLocked && config.predictionFactor > 0.05f) {
                canvas.drawCircle(cand.predictedX, cand.predictedY, 7f, leadGhostPaint)
            }
            canvas.drawText(
                "${cand.label} [${config.hitboxBone.shortTag}]",
                cand.bodyX - 65f,
                cand.headY - 22f,
                textPaint
            )
        }

        // 5. Zero-Lag Sticky Lock Reticle Movement
        if (config.autoAimEnabled && lockedCandidate != null) {
            // Combine speed + stickyStrength so at high stickyStrength the reticle glues 100% onto the predicted hitbox!
            val baseLerp = (dt * config.aimSpeed * 1.4f).coerceIn(0.15f, 1f)
            val stickySnap = (baseLerp + config.stickyStrength * 0.85f).coerceIn(0.35f, 1.0f)

            reticleX += (lockedCandidate.predictedX - reticleX) * stickySnap
            reticleY += (lockedCandidate.predictedY - reticleY) * stickySnap

            if (config.showTrajectoryVectors) {
                canvas.drawLine(centerX, centerY, reticleX, reticleY, dashedPaint)
            }

            lockCirclePaint.color = Color.parseColor("#FF2A85")
            canvas.drawCircle(reticleX, reticleY, 30f, lockCirclePaint)
            // Outer magnetic lock brackets
            val br = 22f
            canvas.drawRect(reticleX - br, reticleY - br, reticleX + br, reticleY + br, lockCirclePaint)
            lockCirclePaint.style = Paint.Style.FILL
            canvas.drawCircle(reticleX, reticleY, 8f, lockCirclePaint)
            lockCirclePaint.style = Paint.Style.STROKE

            // Dispatch predictive gesture periodically
            if (now - lastGestureDispatchMs >= config.gestureIntervalMs) {
                lastGestureDispatchMs = now
                onTargetLockedTick(
                    centerX,
                    centerY,
                    reticleX,
                    reticleY,
                    lockedCandidate.label,
                    bestAngleDeg
                )
            }
        } else {
            val returnLerp = (dt * config.aimSpeed).coerceIn(0.1f, 1f)
            reticleX += (centerX - reticleX) * returnLerp
            reticleY += (centerY - reticleY) * returnLerp
            lockCirclePaint.color = primaryColorInt
            canvas.drawCircle(reticleX, reticleY, 24f, lockCirclePaint)
        }

        // 6. Precision Diagnostics Badge under FOV Circle
        if (config.showTelemetryHud) {
            val statusText = if (config.autoAimEnabled && lockedCandidate != null) {
                val stickyPct = (config.stickyStrength * 100f).toInt()
                String.format(
                    Locale.US,
                    "STICKY %d%%: %s [%s] | Δθ: 0.0°",
                    stickyPct,
                    lockedCandidate.label,
                    config.hitboxBone.shortTag
                )
            } else if (config.autoAimEnabled) {
                String.format(
                    Locale.US,
                    "FOV: %.1f° | STICKY %d%% // SCANNING",
                    config.detectionAngle,
                    (config.stickyStrength * 100f).toInt()
                )
            } else {
                "STICKY AIM: STANDBY (OFF)"
            }
            val textWidth = textPaint.measureText(statusText)
            val badgeRect = RectF(
                centerX - textWidth / 2f - 22f,
                centerY + fovRadiusPx + 20f,
                centerX + textWidth / 2f + 22f,
                centerY + fovRadiusPx + 68f
            )
            canvas.drawRoundRect(badgeRect, 14f, 14f, hudBgPaint)
            canvas.drawText(statusText, centerX - textWidth / 2f, centerY + fovRadiusPx + 52f, textPaint)
        }

        if (AimSessionRepository.isFloatingOverlayVisible.value) {
            postInvalidateOnAnimation()
        }
    }
}
