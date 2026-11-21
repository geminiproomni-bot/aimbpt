package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CrimsonLock
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TacticalGreen
import com.example.ui.theme.TelemetryAmber
import java.util.Locale
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * High-precision 3D Vector matching Unity's Vector3 with velocity lead prediction helpers.
 */
data class Vec3(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) {
    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vec3(x * scalar, y * scalar, z * scalar)

    fun magnitude(): Float = sqrt(x * x + y * y + z * z)

    fun normalized(): Vec3 {
        val mag = magnitude()
        return if (mag > 1e-6f) Vec3(x / mag, y / mag, z / mag) else Vec3(0f, 0f, 1f)
    }

    fun dot(other: Vec3): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vec3): Vec3 = Vec3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    companion object {
        fun lerp(a: Vec3, b: Vec3, t: Float): Vec3 {
            val clamped = t.coerceIn(0f, 1f)
            return Vec3(
                a.x + (b.x - a.x) * clamped,
                a.y + (b.y - a.y) * clamped,
                a.z + (b.z - a.z) * clamped
            )
        }

        /**
         * Matches Unity's Vector3.Angle(from, to) returning angle in degrees [0, 180].
         */
        fun angle(from: Vec3, to: Vec3): Float {
            val denom = sqrt(
                (from.x * from.x + from.y * from.y + from.z * from.z) *
                    (to.x * to.x + to.y * to.y + to.z * to.z)
            )
            if (denom < 1e-6f) return 0f
            val dotVal = (from.dot(to) / denom).coerceIn(-1f, 1f)
            return Math.toDegrees(acos(dotVal.toDouble())).toFloat()
        }
    }
}

/**
 * Quaternion matching Unity's Quaternion.LookRotation and Quaternion.Slerp.
 */
data class Quaternion(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val w: Float = 1f
) {
    fun normalized(): Quaternion {
        val mag = sqrt(x * x + y * y + z * z + w * w)
        return if (mag > 1e-6f) Quaternion(x / mag, y / mag, z / mag, w / mag) else Quaternion()
    }

    /**
     * Rotates the forward unit vector (0, 0, 1) by this quaternion.
     */
    fun forward(): Vec3 {
        val vx = 2f * (x * z + w * y)
        val vy = 2f * (y * z - w * x)
        val vz = 1f - 2f * (x * x + y * y)
        return Vec3(vx, vy, vz).normalized()
    }

    companion object {
        fun fromYawPitch(yawRad: Float, pitchRad: Float): Quaternion {
            val cy = cos(yawRad * 0.5f)
            val sy = sin(yawRad * 0.5f)
            val cp = cos(pitchRad * 0.5f)
            val sp = sin(pitchRad * 0.5f)
            return Quaternion(
                x = cy * sp,
                y = sy * cp,
                z = -sy * sp,
                w = cy * cp
            ).normalized()
        }

        fun lookRotation(forwardDir: Vec3): Quaternion {
            val dir = forwardDir.normalized()
            val yaw = atan2(dir.x.toDouble(), dir.z.toDouble()).toFloat()
            val pitch = asin((-dir.y).coerceIn(-1f, 1f).toDouble()).toFloat()
            return fromYawPitch(yaw, pitch)
        }

        /**
         * Spherical Linear Interpolation matching Unity's Quaternion.Slerp(a, b, t).
         */
        fun slerp(a: Quaternion, b: Quaternion, tRaw: Float): Quaternion {
            val t = tRaw.coerceIn(0f, 1f)
            var bx = b.x
            var by = b.y
            var bz = b.z
            var bw = b.w

            var cosHalfTheta = a.x * bx + a.y * by + a.z * bz + a.w * bw
            if (cosHalfTheta < 0f) {
                bx = -bx
                by = -by
                bz = -bz
                bw = -bw
                cosHalfTheta = -cosHalfTheta
            }

            if (cosHalfTheta >= 0.9995f) {
                return Quaternion(
                    x = a.x + t * (bx - a.x),
                    y = a.y + t * (by - a.y),
                    z = a.z + t * (bz - a.z),
                    w = a.w + t * (bw - a.w)
                ).normalized()
            }

            val halfTheta = acos(cosHalfTheta.coerceIn(-1f, 1f).toDouble()).toFloat()
            val sinHalfTheta = sqrt(1f - cosHalfTheta * cosHalfTheta)
            if (kotlin.math.abs(sinHalfTheta) < 0.001f) {
                return Quaternion(
                    x = a.x * 0.5f + bx * 0.5f,
                    y = a.y * 0.5f + by * 0.5f,
                    z = a.z * 0.5f + bz * 0.5f,
                    w = a.w * 0.5f + bw * 0.5f
                ).normalized()
            }

            val ratioA = sin((1f - t) * halfTheta) / sinHalfTheta
            val ratioB = sin(t * halfTheta) / sinHalfTheta
            return Quaternion(
                x = a.x * ratioA + bx * ratioB,
                y = a.y * ratioA + by * ratioB,
                z = a.z * ratioA + bz * ratioB,
                w = a.w * ratioA + bw * ratioB
            ).normalized()
        }
    }
}

enum class TargetPattern(val titleAr: String, val badge: String) {
    STRAFE("حركة جانبية تكتيكية (Strafe)", "STRAFE"),
    ORBIT("دوران مداري سريع (Orbit)", "ORBIT"),
    ZIGZAG("مناورة متعرجة (ZigZag)", "ZIGZAG"),
    RUSH("تقدم هجومي (Rush)", "RUSH")
}

/**
 * Selects which anatomical hitbox on the enemy the Sticky Aim glues onto.
 */
enum class AimHitboxBone(val labelAr: String, val shortTag: String, val verticalOffsetMeters: Float) {
    HEAD("الرأس (Headshot Lock 100%)", "HEAD", 0.42f),
    CHEST("الصدر والمركز (Center Mass)", "CHEST", 0.0f),
    SMART_AUTO("ذكي تلقائي (Dynamic Head/Chest)", "SMART", 0.28f)
}

data class EnemyTarget(
    val id: Int,
    val codeName: String,
    val position: Vec3,
    val hitboxPosition: Vec3 = position,
    val predictedPosition: Vec3 = position,
    val velocity: Vec3 = Vec3(),
    val speedMps: Float = 0f,
    val phaseOffset: Float,
    val pattern: TargetPattern,
    val health: Float = 100f,
    val maxHealth: Float = 100f,
    val currentAngleDeg: Float = 0f,
    val distanceMeters: Float = 20f,
    val normalizedScreenX: Float = 0.5f,
    val normalizedScreenY: Float = 0.5f,
    val headScreenX: Float = 0.5f,
    val headScreenY: Float = 0.48f,
    val chestScreenX: Float = 0.5f,
    val chestScreenY: Float = 0.5f,
    val predictedScreenX: Float = 0.5f,
    val predictedScreenY: Float = 0.5f,
    val activeHitboxTag: String = "HEAD",
    val isInFront: Boolean = true,
    val isInsideCone: Boolean = false,
    val isClosestLocked: Boolean = false
)

enum class ReticleThemeColor(val labelAr: String, val primaryColor: Color, val hexString: String) {
    NEON_CYAN("سيان تكتيكي (Neon Cyan)", NeonCyan, "#00F5D4"),
    CRIMSON_LOCK("قرمزي القفل (Crimson)", CrimsonLock, "#FF2A85"),
    TACTICAL_GREEN("أخضر راداري (Radar Green)", TacticalGreen, "#10B981"),
    AMBER_GOLD("كهرماني (Telemetry Amber)", TelemetryAmber, "#F59E0B")
}

enum class TargetPriorityMode(val labelAr: String, val descAr: String) {
    CLOSEST_ANGLE_IN_CONE(
        "أقرب زاوية + حفظ القفل اللصيق (Smart Sticky Cone)",
        "يختار أقرب هدف لمركز الدائرة ويحافظ على الالتصاق به (Sticky Retention) دون تشتت"
    ),
    NEAREST_DISTANCE_3D(
        "الأقرب مسافة ثلاثية الأبعاد (3D Proximity Lock)",
        "يعطي الأولوية للهدف الأقرب مسافة إلى اللاعب داخل دائرة مجال الرؤية"
    ),
    LOWEST_HP(
        "الأقل صحة (Finisher Lock)",
        "يلتصق تلقائياً بالهدف الأضعف صحة لحسم الإقصاء السريع"
    )
}

enum class GestureDispatchMode(val labelAr: String, val descAr: String) {
    SLERP_CAMERA_AND_SWIPE(
        "التصويب اللصيق الكامل (Magnetic Camera Lock + Swipe)",
        "قفل مغناطيسي للكاميرا والدائرة معاً على الهدف بدقة 0.0° مع إرسال إيماءات التتبع"
    ),
    RETICLE_OFFSET_SWIPE(
        "التصاق الدائرة العائمة بالهدف (Floating Reticle Glue)",
        "تلتصق الدائرة المتحركة مباشرة بنقطة الهدف (الرأس/الصدر) داخل دائرة FOV دون تحريك الكاميرا"
    ),
    AUTO_LOCK_TAP(
        "قفل مغناطيسي + إطلاق تلقائي فوري (Sticky Lock & Auto-Fire)",
        "يلتصق بالهدف بتنبؤ حركي ويرسل إيماءة نقر/إطلاق تلقائية فور استقرار القفل"
    )
}

data class AimConfigState(
    val autoAimEnabled: Boolean = true,
    val detectionAngle: Float = 20f,
    val aimSpeed: Float = 18f,
    // 0.0f .. 1.0f: Magnetic sticky adhesion strength (0.95f = ultra-sticky zero-error lock)
    val stickyStrength: Float = 0.95f,
    // 0.0f .. 1.5f: Kinematic Kalman velocity lead prediction factor
    val predictionFactor: Float = 1.0f,
    // Retains lock on current target unless it leaves 1.3x cone
    val stickyTargetRetention: Boolean = true,
    val hitboxBone: AimHitboxBone = AimHitboxBone.HEAD,
    val cameraFov: Float = 60f,
    val gestureDurationMs: Long = 45L,
    val gestureIntervalMs: Long = 110L,
    val enemySpeedMultiplier: Float = 1.0f,
    val autoFireOnLock: Boolean = true,
    val showTrajectoryVectors: Boolean = true,
    val showTelemetryHud: Boolean = true,
    val overlayTargetsEnabled: Boolean = true,
    val reticleColor: ReticleThemeColor = ReticleThemeColor.NEON_CYAN,
    val priorityMode: TargetPriorityMode = TargetPriorityMode.CLOSEST_ANGLE_IN_CONE,
    val gestureMode: GestureDispatchMode = GestureDispatchMode.SLERP_CAMERA_AND_SWIPE
)

enum class GestureExecutionStatus(val badge: String) {
    DISPATCHED_ACCESSIBILITY("SERVICE_DISPATCHED"),
    SIMULATED_SANDBOX("STICKY_LOCKED"),
    CANCELLED("CANCELLED")
}

data class GestureStrokeRecord(
    val id: Long,
    val timeFormatted: String,
    val typeLabel: String,
    val startX: Int,
    val startY: Int,
    val endX: Int,
    val endY: Int,
    val durationMs: Long,
    val targetCodeName: String,
    val angleDeg: Float,
    val status: GestureExecutionStatus
)

/**
 * Generates updated Unity C# + Android Accessibility Bridge source code
 * incorporating Smart Sticky Aim, Velocity Lead Prediction, and Hysteresis Lock.
 */
object AimCodeGenerator {
    fun generateUnityCSharpCode(config: AimConfigState): String {
        val hitboxOffsetStr = String.format(Locale.US, "%.2ff", config.hitboxBone.verticalOffsetMeters)
        val stickyStr = String.format(Locale.US, "%.2ff", config.stickyStrength)
        val predStr = String.format(Locale.US, "%.2ff", config.predictionFactor)
        return """
using UnityEngine;
using UnityEngine.UI;
using System.Collections.Generic;

/// <summary>
/// Smart Sticky Aim System (نظام التصويب اللصيق الذكي بدون أخطاء)
/// يتضمن:
/// 1. التنبؤ الحركي بمسار الهدف (Kinematic Velocity Lead Prediction) لمنع أي تأخر أثناء حركة الهدف.
/// 2. الالتصاق المغناطيسي ثنائي النطاق (Dual-Zone Magnetic Sticky Lock) بدقة 0.0 درجة.
/// 3. الحفاظ على الهدف المقفل (Sticky Target Hysteresis) لمنع التشتت عند تقاطع الأهداف.
/// </summary>
public class DevAimTester : MonoBehaviour
{
    public Camera playerCamera;
    public Transform[] enemies;
    public Image fovCircleImage;
    public Image stickyLockDot;

    [Header("Smart Sticky Aim Parameters")]
    public float detectionAngle = ${String.format(Locale.US, "%.1ff", config.detectionAngle)};
    public float aimSpeed = ${String.format(Locale.US, "%.1ff", config.aimSpeed)};
    [Range(0f, 1f)] public float stickyStrength = $stickyStr;
    [Range(0f, 1.5f)] public float predictionFactor = $predStr;
    public Vector3 hitboxOffset = new Vector3(0f, $hitboxOffsetStr, 0f); // ${config.hitboxBone.shortTag}
    public bool autoAimEnabled = ${config.autoAimEnabled};

    private Transform currentLockedTarget = null;
    private Dictionary<Transform, Vector3> prevPositions = new Dictionary<Transform, Vector3>();
    private Dictionary<Transform, Vector3> velocities = new Dictionary<Transform, Vector3>();

    void Update()
    {
        Vector3 screenCenter = new Vector3(Screen.width / 2f, Screen.height / 2f, 0f);
        if (fovCircleImage != null)
            fovCircleImage.rectTransform.position = screenCenter;

        UpdateEnemyVelocities();

        if (!autoAimEnabled || playerCamera == null)
        {
            currentLockedTarget = null;
            return;
        }

        Transform target = FindSmartStickyTarget();
        currentLockedTarget = target;

        if (target == null)
        {
            if (stickyLockDot != null)
                stickyLockDot.rectTransform.position = Vector3.Lerp(
                    stickyLockDot.rectTransform.position, screenCenter, Time.deltaTime * aimSpeed);
            return;
        }

        // 1. حساب موقع نقطة الإصابة (Hitbox) مع التنبؤ الحركي بالسرعة (Zero-Lag Prediction)
        Vector3 targetVel = velocities.ContainsKey(target) ? velocities[target] : Vector3.zero;
        float leadTime = (1.05f - stickyStrength * 0.5f) * 0.06f * predictionFactor;
        Vector3 predictedHitboxPos = target.position + hitboxOffset + (targetVel * leadTime);

        Vector3 dir = (predictedHitboxPos - playerCamera.transform.position).normalized;
        float angleError = Vector3.Angle(playerCamera.transform.forward, dir);
        Quaternion targetRot = Quaternion.LookRotation(dir);

        // 2. الالتصاق المغناطيسي الذكي (Magnetic Sticky Lock)
        // كلما اقترب الهدف من المركز، تزداد قوة الالتصاق لتصل إلى قفل تام 100% بدون أي تأخر
        float magneticBoost = Mathf.Clamp01(1f - (angleError / Mathf.Max(1f, detectionAngle)));
        float effectiveT = Mathf.Lerp(
            Time.deltaTime * aimSpeed,
            1f,
            stickyStrength * (0.65f + 0.35f * magneticBoost)
        );

        playerCamera.transform.rotation = Quaternion.Slerp(
            playerCamera.transform.rotation, targetRot, effectiveT);

        // 3. تثبيت دائرة القفل اللصيقة مباشرة على موقع الهدف على الشاشة
        if (stickyLockDot != null)
        {
            Vector3 screenPos = playerCamera.WorldToScreenPoint(predictedHitboxPos);
            if (screenPos.z > 0f)
            {
                stickyLockDot.rectTransform.position = Vector3.Lerp(
                    stickyLockDot.rectTransform.position, screenPos, Mathf.Max(effectiveT, stickyStrength));
            }
        }
    }

    void UpdateEnemyVelocities()
    {
        float dt = Mathf.Max(Time.deltaTime, 0.0001f);
        foreach (var e in enemies)
        {
            if (e == null) continue;
            if (prevPositions.ContainsKey(e))
            {
                Vector3 instantVel = (e.position - prevPositions[e]) / dt;
                velocities[e] = Vector3.Lerp(
                    velocities.ContainsKey(e) ? velocities[e] : instantVel, instantVel, 0.35f);
            }
            prevPositions[e] = e.position;
        }
    }

    Transform FindSmartStickyTarget()
    {
        // الحفاظ على الهدف المقفل حالياً طالما بقي ضمن النطاق المغناطيسي لمنع التشتت
        if (currentLockedTarget != null && currentLockedTarget.gameObject.activeInHierarchy)
        {
            Vector3 lockedDir = (currentLockedTarget.position + hitboxOffset) - playerCamera.transform.position;
            float lockedAngle = Vector3.Angle(playerCamera.transform.forward, lockedDir);
            if (lockedAngle <= detectionAngle * 1.25f)
                return currentLockedTarget;
        }

        Transform best = null;
        float bestAngle = detectionAngle;
        foreach (var e in enemies)
        {
            if (e == null || !e.gameObject.activeInHierarchy) continue;
            Vector3 dir = (e.position + hitboxOffset) - playerCamera.transform.position;
            float angle = Vector3.Angle(playerCamera.transform.forward, dir);
            if (angle < bestAngle)
            {
                bestAngle = angle;
                best = e;
            }
        }
        return best;
    }

    public void ToggleAutoAim() => autoAimEnabled = !autoAimEnabled;
}
        """.trimIndent()
    }

    fun generateAndroidAccessibilitySnippet(config: AimConfigState): String {
        return """
// Smart Predictive Sticky Gesture Controller (DevAimAccessibilityService.kt)
fun dispatchStickyAimGesture(
    service: AccessibilityService,
    centerX: Float,
    centerY: Float,
    targetX: Float,
    targetY: Float,
    targetVelX: Float,
    targetVelY: Float
) {
    // تعويض سرعة الهدف (Velocity Lead Prediction) + معامل الالتصاق المغناطيسي
    val stickyGain = ${String.format(Locale.US, "%.2ff", config.stickyStrength)}
    val leadFactor = ${String.format(Locale.US, "%.2ff", config.predictionFactor)} * 0.05f
    val predictedX = targetX + targetVelX * leadFactor
    val predictedY = targetY + targetVelY * leadFactor

    val lockX = centerX + (predictedX - centerX) * stickyGain
    val lockY = centerY + (predictedY - centerY) * stickyGain

    val path = Path().apply {
        moveTo(centerX, centerY)
        lineTo(lockX, lockY)
    }
    val stroke = GestureDescription.StrokeDescription(path, 0L, ${config.gestureDurationMs}L)
    val gesture = GestureDescription.Builder().addStroke(stroke).build()
    service.dispatchGesture(gesture, null, null)
}
        """.trimIndent()
    }
}
