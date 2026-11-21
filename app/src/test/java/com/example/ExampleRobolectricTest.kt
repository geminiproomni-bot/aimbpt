package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AimSessionRepository
import com.example.model.AimHitboxBone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context and verify max sticky mode`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DevAim Studio", appName)

        AimSessionRepository.activateMaxStickyMode()
        val cfg = AimSessionRepository.config.value
        assertTrue(cfg.autoAimEnabled)
        assertEquals(1.0f, cfg.stickyStrength, 0.001f)
        assertEquals(AimHitboxBone.HEAD, cfg.hitboxBone)
    }
}
