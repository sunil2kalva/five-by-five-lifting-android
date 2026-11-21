package com.fivebyfivelifting.app.domain

/**
 * Audio stream routing decision for the rest-timer completion alert.
 */
enum class AlertAudioRoute {
    /**
     * Navigation/guidance usage (USAGE_ASSISTANCE_NAVIGATION_GUIDANCE).
     * Mirrors Google Maps turn-by-turn audio: ducks active music (e.g. Spotify) softly into
     * the background while playing our alert prominently at the user's listening volume on
     * headphones, Bluetooth earbuds, or speaker.
     */
    GUIDANCE_STREAM,

    /**
     * System alarm stream (USAGE_ALARM) used when no music is active and alarm volume
     * is audible and at least as loud as media volume (e.g. phone media muted).
     */
    ALARM_STREAM
}

object RestAlertAudioPolicy {
    /**
     * Gives Android's VolumeShaper ducking curve time to ramp background audio down before
     * the first chime note plays.
     */
    const val DUCK_LEAD_IN_DELAY_MS: Long = 350L

    /**
     * Holds focus briefly after the final chime note ends so music swells back smoothly.
     */
    const val DUCK_RELEASE_TAIL_DELAY_MS: Long = 300L

    /**
     * Keeps the device screen awake whenever an active workout session or rest timer is active.
     */
    fun shouldKeepScreenOn(
        hasActiveSession: Boolean,
        isRestTimerRunning: Boolean
    ): Boolean = hasActiveSession || isRestTimerRunning

    /**
     * Picks the route that yields the clearest, most prominent rest-timer alert over any
     * playing music without muting or stopping playback.
     */
    fun chooseRoute(
        alarmVolume: Int,
        alarmMaxVolume: Int,
        musicVolume: Int,
        musicMaxVolume: Int,
        isMusicActive: Boolean
    ): AlertAudioRoute {
        val hasMusicVolume = musicVolume > 0 && musicMaxVolume > 0
        val hasAlarmVolume = alarmVolume > 0 && alarmMaxVolume > 0

        if (isMusicActive && hasMusicVolume) {
            return AlertAudioRoute.GUIDANCE_STREAM
        }
        if (!hasAlarmVolume && hasMusicVolume) {
            return AlertAudioRoute.GUIDANCE_STREAM
        }
        if (hasAlarmVolume && !hasMusicVolume) {
            return AlertAudioRoute.ALARM_STREAM
        }
        if (hasAlarmVolume && hasMusicVolume) {
            val musicRatio = musicVolume.toFloat() / musicMaxVolume.toFloat()
            val alarmRatio = alarmVolume.toFloat() / alarmMaxVolume.toFloat()
            return if (musicRatio > alarmRatio) {
                AlertAudioRoute.GUIDANCE_STREAM
            } else {
                AlertAudioRoute.ALARM_STREAM
            }
        }
        return AlertAudioRoute.ALARM_STREAM
    }
}
