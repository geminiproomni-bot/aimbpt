package com.example

import com.example.model.Quaternion
import com.example.model.Vec3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun lookRotation_and_forward_preserveTargetDirectionExactly() {
        val testTargets = listOf(
            Vec3(0f, 0f, 20f),
            Vec3(5.5f, 2.2f, 18f),
            Vec3(-6.1f, -1.8f, 24f),
            Vec3(3.2f, 4.0f, 15f)
        )
        for (target in testTargets) {
            val expectedDir = target.normalized()
            val rot = Quaternion.lookRotation(expectedDir)
            val actualForward = rot.forward()
            val angularError = Vec3.angle(expectedDir, actualForward)
            assertTrue("Angular error should be < 0.05 deg but was $angularError", angularError < 0.05f)
        }
    }

    @Test
    fun stickySlerp_convergesToZeroAngleError() {
        var camRot = Quaternion()
        val targetDir = Vec3(4.5f, 1.5f, 20f).normalized()
        val targetRot = Quaternion.lookRotation(targetDir)
        camRot = Quaternion.slerp(camRot, targetRot, 1.0f)
        val postError = Vec3.angle(camRot.forward(), targetDir)
        assertEquals(0.0f, postError, 0.05f)
    }
}
