package com.fivebyfivelifting.app

import com.fivebyfivelifting.app.domain.AlertAudioRoute
import com.fivebyfivelifting.app.domain.RestAlertAudioPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestAlertAudioPolicyTest {

    @Test
    fun `screen stays awake during active workout session or running rest timer`() {
        assertFalse(RestAlertAudioPolicy.shouldKeepScreenOn(hasActiveSession = false, isRestTimerRunning = false))
        assertTrue(RestAlertAudioPolicy.shouldKeepScreenOn(hasActiveSession = true, isRestTimerRunning = false))
        assertTrue(RestAlertAudioPolicy.shouldKeepScreenOn(hasActiveSession = false, isRestTimerRunning = true))
        assertTrue(RestAlertAudioPolicy.shouldKeepScreenOn(hasActiveSession = true, isRestTimerRunning = true))
    }

    @Test
    fun `uses alarm stream by default when alarm volume is audible and no music is playing`() {
        val route = RestAlertAudioPolicy.chooseRoute(
            alarmVolume = 8,
            alarmMaxVolume = 15,
            musicVolume = 10,
            musicMaxVolume = 15,
            isMusicActive = false
        )
        assertEquals(AlertAudioRoute.ALARM_STREAM, route)
    }

    @Test
    fun `falls back to media stream when alarm volume is muted and media volume is audible`() {
        val route = RestAlertAudioPolicy.chooseRoute(
            alarmVolume = 0,
            alarmMaxVolume = 15,
            musicVolume = 10,
            musicMaxVolume = 15,
            isMusicActive = true
        )
        assertEquals(AlertAudioRoute.MEDIA_STREAM, route)
    }

    @Test
    fun `switches to media stream when music is loud and alarm volume is far quieter`() {
        val route = RestAlertAudioPolicy.chooseRoute(
            alarmVolume = 2,
            alarmMaxVolume = 15,
            musicVolume = 12,
            musicMaxVolume = 15,
            isMusicActive = true
        )
        assertEquals(AlertAudioRoute.MEDIA_STREAM, route)
    }

    @Test
    fun `keeps alarm stream when music is active and alarm volume is comparable`() {
        val route = RestAlertAudioPolicy.chooseRoute(
            alarmVolume = 9,
            alarmMaxVolume = 15,
            musicVolume = 12,
            musicMaxVolume = 15,
            isMusicActive = true
        )
        assertEquals(AlertAudioRoute.ALARM_STREAM, route)
    }

    @Test
    fun `handles muted phone or zero max volumes safely`() {
        assertEquals(
            AlertAudioRoute.ALARM_STREAM,
            RestAlertAudioPolicy.chooseRoute(
                alarmVolume = 0,
                alarmMaxVolume = 0,
                musicVolume = 0,
                musicMaxVolume = 0,
                isMusicActive = false
            )
        )
        assertEquals(
            AlertAudioRoute.ALARM_STREAM,
            RestAlertAudioPolicy.chooseRoute(
                alarmVolume = 0,
                alarmMaxVolume = 15,
                musicVolume = 0,
                musicMaxVolume = 15,
                isMusicActive = false
            )
        )
    }
}
