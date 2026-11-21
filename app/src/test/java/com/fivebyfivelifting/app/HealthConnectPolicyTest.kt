package com.fivebyfivelifting.app

import com.fivebyfivelifting.app.domain.HealthConnectManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthConnectPolicyTest {

    @Test
    fun `health connect declares write-only exercise permission for Play Store compliance`() {
        val perms = HealthConnectManager.requiredPermissions
        assertEquals(1, perms.size)
        assertTrue(perms.contains("android.permission.health.WRITE_EXERCISE"))
        assertFalse(perms.contains("android.permission.health.READ_EXERCISE"))
    }

    @Test
    fun `recognizes Android 13 and Android 14+ health connect privacy rationale intents`() {
        assertTrue(
            HealthConnectManager.isPrivacyRationaleIntent(
                "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE"
            )
        )
        assertTrue(
            HealthConnectManager.isPrivacyRationaleIntent(
                "android.intent.action.VIEW_PERMISSION_USAGE"
            )
        )
        assertFalse(HealthConnectManager.isPrivacyRationaleIntent("android.intent.action.MAIN"))
        assertFalse(HealthConnectManager.isPrivacyRationaleIntent(null))
    }

    @Test
    fun `privacy policy url uses HTTPS`() {
        assertTrue(HealthConnectManager.PRIVACY_POLICY_URL.startsWith("https://"))
        assertTrue(HealthConnectManager.PRIVACY_POLICY_URL.endsWith("privacy-policy.html"))
    }
}
