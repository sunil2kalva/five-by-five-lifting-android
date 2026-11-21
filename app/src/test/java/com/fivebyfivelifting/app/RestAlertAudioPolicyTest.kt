package com.fivebyfivelifting.app

import com.fivebyfivelifting.app.domain.AlertAudioRoute
import com.fivebyfivelifting.app.domain.RestAlertAudioPolicy
import com.fivebyfivelifting.app.domain.RestTimerChimeSynthesizer
import com.fivebyfivelifting.app.ui.RestTimerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

class RestAlertAudioPolicyTest {

    @Test
    fun `screen stays awake during active workout session or running rest timer`() {
        assertFalse(RestAlertAudioPolicy.shouldKeepScreenOn(hasActiveSession = false, isRestTimerRunning = false))
        assertTrue(RestAlertAudioPolicy.shouldKeepScreenOn(hasActiveSession = true, isRestTimerRunning = false))
        assertTrue(RestAlertAudioPolicy.shouldKeepScreenOn(hasActiveSession = false, isRestTimerRunning = true))
        assertTrue(RestAlertAudioPolicy.shouldKeepScreenOn(hasActiveSession = true, isRestTimerRunning = true))
    }

    @Test
    fun `routes to guidance stream when music is actively playing so audio ducks cleanly`() {
        val routeEvenWithHighAlarm = RestAlertAudioPolicy.chooseRoute(
            alarmVolume = 15,
            alarmMaxVolume = 15,
            musicVolume = 8,
            musicMaxVolume = 15,
            isMusicActive = true
        )
        assertEquals(AlertAudioRoute.GUIDANCE_STREAM, routeEvenWithHighAlarm)

        val routeWithMutedAlarm = RestAlertAudioPolicy.chooseRoute(
            alarmVolume = 0,
            alarmMaxVolume = 15,
            musicVolume = 10,
            musicMaxVolume = 15,
            isMusicActive = true
        )
        assertEquals(AlertAudioRoute.GUIDANCE_STREAM, routeWithMutedAlarm)
    }

    @Test
    fun `uses alarm stream when no music is active and alarm slider is at least as loud as media`() {
        val route = RestAlertAudioPolicy.chooseRoute(
            alarmVolume = 12,
            alarmMaxVolume = 15,
            musicVolume = 8,
            musicMaxVolume = 15,
            isMusicActive = false
        )
        assertEquals(AlertAudioRoute.ALARM_STREAM, route)

        val mutedMediaRoute = RestAlertAudioPolicy.chooseRoute(
            alarmVolume = 10,
            alarmMaxVolume = 15,
            musicVolume = 0,
            musicMaxVolume = 15,
            isMusicActive = false
        )
        assertEquals(AlertAudioRoute.ALARM_STREAM, mutedMediaRoute)
    }

    @Test
    fun `uses guidance stream when no music is active but media slider is louder than alarm`() {
        val route = RestAlertAudioPolicy.chooseRoute(
            alarmVolume = 4,
            alarmMaxVolume = 15,
            musicVolume = 12,
            musicMaxVolume = 15,
            isMusicActive = false
        )
        assertEquals(AlertAudioRoute.GUIDANCE_STREAM, route)
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

    @Test
    fun `count-up rest timer transitions to elapsed red state and continues into overtime`() {
        val start = RestTimerState(isRunning = true, elapsedSeconds = 0, totalSeconds = 180)
        assertFalse(start.isElapsed)
        assertEquals("0:00", start.formattedElapsed())
        assertEquals(0f, start.progressFraction, 0.001f)
        assertEquals(180, start.remainingSeconds)

        val mid = start.copy(elapsedSeconds = 90)
        assertFalse(mid.isElapsed)
        assertEquals("1:30", mid.formattedElapsed())
        assertEquals(0.5f, mid.progressFraction, 0.001f)

        val justBefore = start.copy(elapsedSeconds = 179)
        assertFalse(justBefore.isElapsed)
        assertEquals("2:59", justBefore.formattedElapsed())

        val atTarget = start.copy(elapsedSeconds = 180, hasAlerted = true)
        assertTrue(atTarget.isElapsed)
        assertEquals("3:00", atTarget.formattedElapsed())
        assertEquals(1f, atTarget.progressFraction, 0.001f)
        assertEquals(0, atTarget.remainingSeconds)

        val overtime = start.copy(elapsedSeconds = 255, hasAlerted = true)
        assertTrue(overtime.isElapsed)
        assertEquals("4:15", overtime.formattedElapsed())
        assertEquals(1f, overtime.progressFraction, 0.001f)
        assertEquals(0, overtime.remainingSeconds)
    }

    @Test
    fun `synthesized chime produces loud clean PCM audio with silent breathing gap`() {
        val pcm = RestTimerChimeSynthesizer.buildChimePcm()
        val expectedSamples =
            (RestTimerChimeSynthesizer.SAMPLE_RATE_HZ.toLong() * RestTimerChimeSynthesizer.DURATION_MS / 1000L).toInt()
        assertEquals(expectedSamples, pcm.size)

        val peak = pcm.maxOf { abs(it.toInt()) }
        assertTrue("Expected prominent peak amplitude, got $peak", peak in 24_000..Short.MAX_VALUE.toInt())

        // Verify silent gap between the two chime cadences (1300..1500 ms)
        val gapStart = 1300 * RestTimerChimeSynthesizer.SAMPLE_RATE_HZ / 1000
        val gapEnd = 1500 * RestTimerChimeSynthesizer.SAMPLE_RATE_HZ / 1000
        val gapMax = (gapStart until gapEnd).maxOf { abs(pcm[it].toInt()) }
        assertEquals(0, gapMax)

        // Verify active RMS loudness is high enough to cut above ducked Spotify music
        var sumSq = 0.0
        var activeCount = 0
        for (s in pcm) {
            val v = s.toInt()
            if (abs(v) > 100) {
                sumSq += v.toDouble() * v.toDouble()
                activeCount++
            }
        }
        val activeRmsDb = 20.0 * log10(sqrt(sumSq / activeCount) / Short.MAX_VALUE.toDouble())
        assertTrue("Expected chime active RMS >= -10 dBFS, got $activeRmsDb", activeRmsDb >= -10.0)
    }
}
