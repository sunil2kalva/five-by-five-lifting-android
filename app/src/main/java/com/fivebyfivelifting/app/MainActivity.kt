package com.fivebyfivelifting.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fivebyfivelifting.app.domain.AlertAudioRoute
import com.fivebyfivelifting.app.domain.RestAlertAudioPolicy
import com.fivebyfivelifting.app.domain.RestTimerChimeSynthesizer
import com.fivebyfivelifting.app.ui.StrengthViewModel
import com.fivebyfivelifting.app.ui.screens.GraphsScreen
import com.fivebyfivelifting.app.ui.screens.HistoryScreen
import com.fivebyfivelifting.app.ui.screens.HomeScreen
import com.fivebyfivelifting.app.ui.screens.SettingsScreen
import com.fivebyfivelifting.app.ui.screens.WorkoutScreen
import com.fivebyfivelifting.app.ui.theme.FiveByFiveLiftingTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private enum class MainTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME("home", "Home", Icons.Default.Home),
    WORKOUT("workout", "Workout", Icons.Default.FitnessCenter),
    HISTORY("history", "History", Icons.Default.DateRange),
    GRAPHS("graphs", "Graphs", Icons.Default.ShowChart),
    SETTINGS("settings", "Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val alertFocusLock = Any()
    private var activeAlertJob: Job? = null
    private var activeAudioFocusRequest: AudioFocusRequest? = null

    private val viewModel: StrengthViewModel by viewModels {
        val app = application as FiveByFiveLiftingApplication
        StrengthViewModel.Factory(app.workoutRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FiveByFiveLiftingTheme {
                val uiState by viewModel.uiState.collectAsState()
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }

                // Keep the display awake while an active workout session or rest timer is running.
                val keepScreenOn = RestAlertAudioPolicy.shouldKeepScreenOn(
                    hasActiveSession = uiState.activeSession != null,
                    isRestTimerRunning = uiState.restTimer.isRunning
                )
                DisposableEffect(keepScreenOn) {
                    if (keepScreenOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    onDispose {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                /**
                 * Deterministic top-level tab navigation:
                 * Always clears the backstack so tapping "Home" (or any other bottom bar item)
                 * 100% guarantees opening that exact screen and never gets stuck on Settings.
                 */
                fun navigateToTab(tab: MainTab) {
                    navController.navigate(tab.route) {
                        popUpTo(0) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }

                LaunchedEffect(uiState.statusMessage) {
                    val msg = uiState.statusMessage
                    if (!msg.isNullOrBlank()) {
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearStatusMessage()
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        NavigationBar {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.destination?.route
                            MainTab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentRoute == tab.route,
                                    onClick = { navigateToTab(tab) },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = MainTab.HOME.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(MainTab.HOME.route) {
                            HomeScreen(
                                uiState = uiState,
                                onSelectWorkoutType = viewModel::selectWorkoutType,
                                onSaveWorkoutTemplate = { type, title, exercises, enabled ->
                                    viewModel.saveWorkoutTemplate(type, title, exercises, enabled)
                                },
                                onSaveCustomExercise = viewModel::saveCustomExerciseConfig,
                                onStartOrResumeWorkout = {
                                    viewModel.startOrResumeWorkout {
                                        navigateToTab(MainTab.WORKOUT)
                                    }
                                },
                                onUpdateExerciseWeight = { exercise, displayWeight ->
                                    viewModel.updateWorkingWeight(
                                        exercise = exercise,
                                        displayWeight = displayWeight,
                                        showStatusMessage = false
                                    )
                                },
                                onNavigateToSettings = {
                                    navigateToTab(MainTab.SETTINGS)
                                }
                            )
                        }

                        composable(MainTab.WORKOUT.route) {
                            WorkoutScreen(
                                uiState = uiState,
                                onSelectWorkoutType = viewModel::selectWorkoutType,
                                onSaveWorkoutTemplate = { type, title, exercises, enabled ->
                                    viewModel.saveWorkoutTemplate(type, title, exercises, enabled)
                                },
                                onSaveCustomExercise = viewModel::saveCustomExerciseConfig,
                                onStartWorkout = {
                                    viewModel.startOrResumeWorkout {}
                                },
                                onTapSetBubble = { setId ->
                                    stopActiveAlert()
                                    viewModel.onTapSetBubble(setId) {
                                        playRestFinishedAlert()
                                    }
                                },
                                onUpdateExerciseWeight = { exercise, displayWeight ->
                                    viewModel.updateWorkingWeight(
                                        exercise = exercise,
                                        displayWeight = displayWeight,
                                        showStatusMessage = false
                                    )
                                },
                                onSkipRestTimer = {
                                    stopActiveAlert()
                                    viewModel.stopRestTimer()
                                },
                                onFinishWorkout = {
                                    stopActiveAlert()
                                    viewModel.finishActiveWorkout {}
                                },
                                onDiscardWorkout = {
                                    stopActiveAlert()
                                    viewModel.discardActiveWorkout {
                                        navigateToTab(MainTab.HOME)
                                    }
                                },
                                onDismissSummary = {
                                    viewModel.dismissCompletionSummary()
                                    navigateToTab(MainTab.HOME)
                                }
                            )
                        }

                        composable(MainTab.HISTORY.route) {
                            HistoryScreen(
                                uiState = uiState,
                                onDeleteSession = viewModel::deleteSession
                            )
                        }

                        composable(MainTab.GRAPHS.route) {
                            GraphsScreen(
                                uiState = uiState,
                                onLogBodyweight = viewModel::logStandaloneBodyweight
                            )
                        }

                        composable(MainTab.SETTINGS.route) {
                            SettingsScreen(
                                uiState = uiState,
                                onSelectWeightUnit = viewModel::setWeightUnit,
                                onSelectBarbellWeightDisplay = viewModel::setBarbellWeightDisplay,
                                onSelectRestTimerSeconds = viewModel::setRestTimerSeconds,
                                onToggleHealthConnectSync = viewModel::setHealthConnectSyncEnabled,
                                onUpdateExerciseWeight = { exercise, displayWeight ->
                                    viewModel.updateWorkingWeight(
                                        exercise = exercise,
                                        displayWeight = displayWeight,
                                        showStatusMessage = false
                                    )
                                },
                                onToggleAutoSync = viewModel::setAutoSyncEnabled,
                                onPrepareGoogleSignInIntent = viewModel::prepareGoogleSignInIntent,
                                onHandleGoogleSignInResult = viewModel::handleGoogleSignInResult,
                                onSignOutGoogleAccount = viewModel::signOutGoogleAccount,
                                buildOpenGoogleDriveFileIntent = viewModel::buildOpenGoogleDriveFileIntent,
                                onLaunchDirectSaveToGoogleDriveApp = viewModel::launchDirectSaveToGoogleDriveApp,
                                onRestoreFromGoogleDriveUri = viewModel::restoreOrMergeFromGoogleDriveUri,
                                onExportCsvToUri = viewModel::exportCsvToUri
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        stopActiveAlert()
        super.onDestroy()
    }

    private fun stopActiveAlert() {
        activeAlertJob?.cancel()
        activeAlertJob = null
        val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        releaseDuckingAudioFocus(audioManager)
    }

    private fun acquireDuckingAudioFocus(
        audioManager: AudioManager?,
        audioAttributes: AudioAttributes
    ): AudioFocusRequest? {
        if (audioManager == null) return null
        val focusChangeListener = AudioManager.OnAudioFocusChangeListener {
            // Transient alert focus is held briefly while the chime plays, then abandoned.
        }
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(audioAttributes)
            .setAcceptsDelayedFocusGain(false)
            .setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener(focusChangeListener, Handler(Looper.getMainLooper()))
            .build()
        val result = runCatching { audioManager.requestAudioFocus(request) }
            .getOrDefault(AudioManager.AUDIOFOCUS_REQUEST_FAILED)
        return if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            synchronized(alertFocusLock) {
                activeAudioFocusRequest = request
            }
            request
        } else {
            null
        }
    }

    private fun releaseDuckingAudioFocus(
        audioManager: AudioManager?,
        target: AudioFocusRequest? = null
    ) {
        val toAbandon = synchronized(alertFocusLock) {
            val current = activeAudioFocusRequest
            if (target == null) {
                activeAudioFocusRequest = null
                current
            } else if (current === target) {
                activeAudioFocusRequest = null
                target
            } else {
                null
            }
        }
        if (toAbandon != null && audioManager != null) {
            runCatching { audioManager.abandonAudioFocusRequest(toAbandon) }
        }
    }

    /**
     * Requests transient ducking audio focus (like turn-by-turn navigation alerts) so active
     * media such as Spotify drops softly into the background while still playing, waits for
     * Android's VolumeShaper duck ramp-down, plays a loud synthesized ascending multi-harmonic
     * chime at full scale + vibration waveform, and then restores background audio volume.
     */
    private fun playRestFinishedAlert() {
        val previousJob = activeAlertJob
        activeAlertJob = lifecycleScope.launch(Dispatchers.Default) {
            previousJob?.cancelAndJoin()

            val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val route = if (audioManager != null) {
                RestAlertAudioPolicy.chooseRoute(
                    alarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM),
                    alarmMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                    musicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC),
                    musicMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                    isMusicActive = audioManager.isMusicActive
                )
            } else {
                AlertAudioRoute.GUIDANCE_STREAM
            }

            val audioUsage = when (route) {
                AlertAudioRoute.GUIDANCE_STREAM -> AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE
                AlertAudioRoute.ALARM_STREAM -> AudioAttributes.USAGE_ALARM
            }
            val fallbackToneStream = when (route) {
                AlertAudioRoute.GUIDANCE_STREAM -> AudioManager.STREAM_MUSIC
                AlertAudioRoute.ALARM_STREAM -> AudioManager.STREAM_ALARM
            }
            val alertAttributes = AudioAttributes.Builder()
                .setUsage(audioUsage)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            // Pre-render the chime into a static AudioTrack before ducking so sound hits
            // full volume immediately after Spotify fades into the background.
            val pcmSamples = RestTimerChimeSynthesizer.buildChimePcm()
            val preparedTrack = runCatching {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(alertAttributes)
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(RestTimerChimeSynthesizer.SAMPLE_RATE_HZ)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcmSamples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                if (track.state == AudioTrack.STATE_INITIALIZED) {
                    val written = track.write(pcmSamples, 0, pcmSamples.size)
                    if (written > 0) {
                        track.setVolume(AudioTrack.getMaxVolume())
                        track
                    } else {
                        track.release()
                        null
                    }
                } else {
                    track.release()
                    null
                }
            }.getOrNull()

            // 1. Duck background music (e.g. Spotify) softly into the background (still playing).
            val focusRequest = acquireDuckingAudioFocus(audioManager, alertAttributes)
            var vibrator: Vibrator? = null
            var fallbackToneGen: ToneGenerator? = null
            var trackReleased = false
            var toneGenReleased = false

            try {
                if (focusRequest != null) {
                    delay(RestAlertAudioPolicy.DUCK_LEAD_IN_DELAY_MS)
                }

                // 2. Trigger multi-pulse vibration waveform matching the chime cadence.
                runCatching {
                    vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                        vm?.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    }
                    if (vibrator?.hasVibrator() == true) {
                        val timings = longArrayOf(0L, 450L, 250L, 450L, 250L, 450L, 250L, 650L)
                        vibrator?.vibrate(VibrationEffect.createWaveform(timings, -1))
                    }
                }

                // 3. Play prominent synthesized chime via AudioTrack (or ToneGenerator fallback).
                if (preparedTrack != null) {
                    runCatching { preparedTrack.play() }
                    delay(RestTimerChimeSynthesizer.DURATION_MS.toLong() + 150L)
                    runCatching {
                        if (preparedTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                            preparedTrack.stop()
                        }
                    }
                    runCatching { preparedTrack.release() }
                    trackReleased = true
                } else {
                    fallbackToneGen = runCatching {
                        ToneGenerator(fallbackToneStream, ToneGenerator.MAX_VOLUME)
                    }.getOrNull()
                    repeat(4) { pulseIndex ->
                        val toneType = if (pulseIndex == 3) {
                            ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
                        } else {
                            ToneGenerator.TONE_PROP_BEEP2
                        }
                        runCatching { fallbackToneGen?.startTone(toneType, 500) }
                        delay(700L)
                    }
                    runCatching { fallbackToneGen?.release() }
                    toneGenReleased = true
                }

                // Brief tail so background music swells back smoothly right after the chime ends.
                if (focusRequest != null) {
                    delay(RestAlertAudioPolicy.DUCK_RELEASE_TAIL_DELAY_MS)
                }
            } finally {
                if (preparedTrack != null && !trackReleased) {
                    runCatching {
                        if (preparedTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                            preparedTrack.stop()
                        }
                    }
                    runCatching { preparedTrack.release() }
                }
                if (fallbackToneGen != null && !toneGenReleased) {
                    runCatching { fallbackToneGen?.release() }
                }
                if (!isActive) {
                    runCatching { vibrator?.cancel() }
                }
                if (focusRequest != null) {
                    releaseDuckingAudioFocus(audioManager, focusRequest)
                }
            }
        }
    }
}
