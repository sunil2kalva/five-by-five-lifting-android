package com.fivebyfivelifting.app.domain

/**
 * Audio stream routing decision for the rest-timer completion alert.
 */
enum class AlertAudioRoute {
    /** Route through the system alarm stream (default loud timer behavior). */
    ALARM_STREAM,

    /**
     * Route through the media stream when alarm volume is muted or far quieter than
     * active background music (e.g. Spotify on headphones), so the alert stays audible
     * at the user's listening level while background audio is ducked.
     */
    MEDIA_STREAM
}

object RestAlertAudioPolicy {
    const val DUCK_LEAD_IN_DELAY_MS: Long = 180L
    const val DUCK_RELEASE_TAIL_DELAY_MS: Long = 200L
    private const val MIN_ALARM_TO_MUSIC_RATIO: Float = 0.5f

    /**
     * Keeps the device screen awake whenever an active workout session or rest timer is active.
     */
    fun shouldKeepScreenOn(
        hasActiveSession: Boolean,
        isRestTimerRunning: Boolean
    ): Boolean = hasActiveSession || isRestTimerRunning

    /**
     * Picks the most audible stream for the rest-timer alert without raising device volume.
     */
    fun chooseRoute(
        alarmVolume: Int,
        alarmMaxVolume: Int,
        musicVolume: Int,
        musicMaxVolume: Int,
        isMusicActive: Boolean
    ): AlertAudioRoute {
        if (alarmVolume <= 0 || alarmMaxVolume <= 0) {
            return if (musicVolume > 0 && musicMaxVolume > 0) {
                AlertAudioRoute.MEDIA_STREAM
            } else {
                AlertAudioRoute.ALARM_STREAM
            }
        }

        if (isMusicActive && musicVolume > 0 && musicMaxVolume > 0) {
            val alarmRatio = alarmVolume.toFloat() / alarmMaxVolume.toFloat()
            val musicRatio = musicVolume.toFloat() / musicMaxVolume.toFloat()
            if (alarmRatio < musicRatio * MIN_ALARM_TO_MUSIC_RATIO) {
                return AlertAudioRoute.MEDIA_STREAM
            }
        }

        return AlertAudioRoute.ALARM_STREAM
    }
}
