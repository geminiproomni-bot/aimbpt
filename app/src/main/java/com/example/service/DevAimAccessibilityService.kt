package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import android.widget.TextView
import com.example.data.AimSessionRepository
import com.example.model.GestureDispatchMode
import com.example.model.GestureExecutionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Real Android AccessibilityService that draws the floating FOV cone & sticky magnetic reticle
 * via TYPE_ACCESSIBILITY_OVERLAY and performs predictive screen gestures (dispatchGesture).
 */
class DevAimAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var windowManager: WindowManager? = null
    private var reticleOverlayView: FloatingAimReticleView? = null
    private var controlPillLayout: LinearLayout? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        AimSessionRepository.setServiceConnected(true)

        AimSessionRepository.serviceGestureDispatcher = { startX, startY, endX, endY, isTap, targetName, angleDeg ->
            dispatchProgrammaticGesture(startX, startY, endX, endY, isTap, targetName, angleDeg)
        }

        setupAccessibilityOverlayWindows()

        serviceScope.launch {
            AimSessionRepository.isFloatingOverlayVisible.collectLatest { visible ->
                reticleOverlayView?.visibility = if (visible) View.VISIBLE else View.GONE
                controlPillLayout?.visibility = if (visible) View.VISIBLE else View.GONE
                if (visible) {
                    reticleOverlayView?.postInvalidateOnAnimation()
                }
            }
        }
    }

    private fun setupAccessibilityOverlayWindows() {
        val wm = windowManager ?: return
        if (reticleOverlayView != null) return

        // 1. Full-screen pass-through reticle canvas (FLAG_NOT_TOUCHABLE so game touches are never blocked)
        val canvasParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        val reticleView = FloatingAimReticleView(this) { cx, cy, tx, ty, label, angleDeg ->
            val cfg = AimSessionRepository.config.value
            val isTap = (cfg.gestureMode == GestureDispatchMode.AUTO_LOCK_TAP || cfg.autoFireOnLock) && angleDeg <= 6.0f
            // Apply sticky gain to swipe target coordinates
            val stickyEndX = cx + (tx - cx) * cfg.stickyStrength
            val stickyEndY = cy + (ty - cy) * cfg.stickyStrength
            dispatchProgrammaticGesture(cx, cy, stickyEndX, stickyEndY, isTap, label, angleDeg)
        }

        try {
            wm.addView(reticleView, canvasParams)
            reticleOverlayView = reticleView
        } catch (_: Exception) {
        }

        // 2. Compact draggable tactical control pill at top of screen
        val pillParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 96
        }

        val pill = createFloatingControlPill(wm, pillParams)
        try {
            wm.addView(pill, pillParams)
            controlPillLayout = pill
        } catch (_: Exception) {
        }
    }

    private fun createFloatingControlPill(
        wm: WindowManager,
        params: WindowManager.LayoutParams
    ): LinearLayout {
        val bgDrawable = GradientDrawable().apply {
            setColor(Color.argb(235, 17, 24, 39))
            cornerRadius = 42f
            setStroke(3, Color.parseColor("#00F5D4"))
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = bgDrawable
            setPadding(22, 12, 22, 12)
        }

        val dragHandle = TextView(this).apply {
            text = "❖ DevAim "
            setTextColor(Color.parseColor("#00F5D4"))
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(6, 6, 12, 6)
        }

        val btnAutoAim = TextView(this).apply {
            text = "STICKY: ON"
            setTextColor(Color.BLACK)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(18, 9, 18, 9)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#00F5D4"))
                cornerRadius = 24f
            }
            setOnClickListener {
                AimSessionRepository.toggleAutoAim()
                val enabled = AimSessionRepository.config.value.autoAimEnabled
                text = if (enabled) "STICKY: ON" else "STICKY: OFF"
                background = GradientDrawable().apply {
                    setColor(if (enabled) Color.parseColor("#00F5D4") else Color.parseColor("#334155"))
                    cornerRadius = 24f
                }
                setTextColor(if (enabled) Color.BLACK else Color.WHITE)
                reticleOverlayView?.postInvalidateOnAnimation()
            }
        }

        val btnHitbox = TextView(this).apply {
            text = AimSessionRepository.config.value.hitboxBone.shortTag
            setTextColor(Color.parseColor("#FF2A85"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(16, 9, 16, 9)
            setOnClickListener {
                val nextBone = AimSessionRepository.cycleHitboxBone()
                text = nextBone.shortTag
                reticleOverlayView?.postInvalidateOnAnimation()
            }
        }

        val btnTargets = TextView(this).apply {
            text = "BOTS"
            setTextColor(Color.parseColor("#F59E0B"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(14, 9, 14, 9)
            setOnClickListener {
                val current = AimSessionRepository.config.value.overlayTargetsEnabled
                AimSessionRepository.updateOverlayTargetsEnabled(!current)
                setTextColor(if (!current) Color.parseColor("#F59E0B") else Color.GRAY)
                reticleOverlayView?.postInvalidateOnAnimation()
            }
        }

        val btnHide = TextView(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#FF2A85"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(16, 8, 8, 8)
            setOnClickListener {
                AimSessionRepository.setFloatingOverlayVisible(false)
            }
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        dragHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    try {
                        wm.updateViewLayout(container, params)
                    } catch (_: Exception) {
                    }
                    true
                }
                else -> false
            }
        }

        container.addView(dragHandle)
        container.addView(btnAutoAim)
        container.addView(btnHitbox)
        container.addView(btnTargets)
        container.addView(btnHide)
        return container
    }

    /**
     * Dispatches a precision touch gesture (Swipe or Tap) using AccessibilityService.dispatchGesture().
     */
    private fun dispatchProgrammaticGesture(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        isTap: Boolean,
        targetName: String,
        angleDeg: Float
    ): Boolean {
        val config = AimSessionRepository.config.value
        val safeStartX = startX.coerceAtLeast(10f)
        val safeStartY = startY.coerceAtLeast(10f)
        val safeEndX = endX.coerceAtLeast(10f)
        val safeEndY = endY.coerceAtLeast(10f)

        val path = Path().apply {
            if (isTap) {
                moveTo(safeEndX, safeEndY)
            } else {
                moveTo(safeStartX, safeStartY)
                lineTo(safeEndX, safeEndY)
            }
        }

        val duration = if (isTap) 35L else config.gestureDurationMs.coerceIn(20L, 350L)
        val stroke = GestureDescription.StrokeDescription(path, 0L, duration)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val dispatched = try {
            dispatchGesture(
                gesture,
                object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        super.onCompleted(gestureDescription)
                        AimSessionRepository.recordGestureStroke(
                            typeLabel = if (isTap) "STICKY_TAP" else "STICKY_SWIPE",
                            startX = safeStartX.toInt(),
                            startY = safeStartY.toInt(),
                            endX = safeEndX.toInt(),
                            endY = safeEndY.toInt(),
                            durationMs = duration,
                            targetCodeName = "$targetName[${config.hitboxBone.shortTag}]",
                            angleDeg = angleDeg,
                            status = GestureExecutionStatus.DISPATCHED_ACCESSIBILITY
                        )
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        super.onCancelled(gestureDescription)
                        AimSessionRepository.recordGestureStroke(
                            typeLabel = if (isTap) "STICKY_TAP" else "STICKY_SWIPE",
                            startX = safeStartX.toInt(),
                            startY = safeStartY.toInt(),
                            endX = safeEndX.toInt(),
                            endY = safeEndY.toInt(),
                            durationMs = duration,
                            targetCodeName = "$targetName[${config.hitboxBone.shortTag}]",
                            angleDeg = angleDeg,
                            status = GestureExecutionStatus.CANCELLED
                        )
                    }
                },
                null
            )
        } catch (_: Exception) {
            false
        }
        return dispatched
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val root = rootInActiveWindow ?: return
        val detectedPoints = mutableListOf<Pair<Float, Float>>()
        collectTargetNodes(root, detectedPoints, 0)
        reticleOverlayView?.externalNodeTargets = detectedPoints
    }

    private fun collectTargetNodes(
        node: AccessibilityNodeInfo?,
        outPoints: MutableList<Pair<Float, Float>>,
        depth: Int
    ) {
        if (node == null || outPoints.size >= 8 || depth > 12) return
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""

        if (desc.contains("target") || desc.contains("enemy") || desc.contains("bot") ||
            text.contains("target") || text.contains("هدف") || text.contains("bot") ||
            viewId.contains("target") || viewId.contains("enemy")
        ) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (rect.width() > 4 && rect.height() > 4) {
                outPoints.add(rect.exactCenterX() to rect.exactCenterY())
            }
        }

        for (i in 0 until node.childCount) {
            collectTargetNodes(node.getChild(i), outPoints, depth + 1)
        }
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        AimSessionRepository.setServiceConnected(false)
        val wm = windowManager
        reticleOverlayView?.let {
            try {
                wm?.removeView(it)
            } catch (_: Exception) {
            }
        }
        controlPillLayout?.let {
            try {
                wm?.removeView(it)
            } catch (_: Exception) {
            }
        }
        reticleOverlayView = null
        controlPillLayout = null
    }
}
