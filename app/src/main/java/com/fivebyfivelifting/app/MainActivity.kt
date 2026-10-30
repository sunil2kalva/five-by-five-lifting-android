package com.fivebyfivelifting.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import com.fivebyfivelifting.app.ui.StrengthViewModel
import com.fivebyfivelifting.app.ui.screens.GraphsScreen
import com.fivebyfivelifting.app.ui.screens.HistoryScreen
import com.fivebyfivelifting.app.ui.screens.HomeScreen
import com.fivebyfivelifting.app.ui.screens.SettingsScreen
import com.fivebyfivelifting.app.ui.screens.WorkoutScreen
import com.fivebyfivelifting.app.ui.theme.FiveByFiveLiftingTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private var activeAlertJob: Job? = null

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
                                onUpdateExerciseWeight = viewModel::updateWorkingWeight,
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
                                    viewModel.onTapSetBubble(setId) {
                                        playRestFinishedAlert()
                                    }
                                },
                                onUpdateExerciseWeight = viewModel::updateWorkingWeight,
                                onSkipRestTimer = viewModel::stopRestTimer,
                                onFinishWorkout = {
                                    viewModel.finishActiveWorkout {}
                                },
                                onDiscardWorkout = {
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
                                onUpdateExerciseWeight = viewModel::updateWorkingWeight,
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

    /**
     * Plays an unmistakable ~3.5-second multi-pulse gym timer chime & haptic alert
     * when the rest timer between sets finishes counting down.
     */
    private fun playRestFinishedAlert() {
        activeAlertJob?.cancel()
        activeAlertJob = lifecycleScope.launch(Dispatchers.Default) {
            // 1. Trigger a ~3.2-second multi-pulse vibration waveform
            runCatching {
                val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                if (vibrator?.hasVibrator() == true) {
                    val timings = longArrayOf(0L, 450L, 250L, 450L, 250L, 450L, 250L, 650L)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
                }
            }

            // 2. Play default notification/alarm ringtone alongside a 4-burst gym timer chime for ~3.5 seconds
            val ringtone = runCatching {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                RingtoneManager.getRingtone(applicationContext, uri)?.apply {
                    audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    play()
                }
            }.getOrNull()

            val toneGen = runCatching {
                ToneGenerator(AudioManager.STREAM_ALARM, 100)
            }.getOrElse {
                runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) }.getOrNull()
            }

            try {
                repeat(4) { pulseIndex ->
                    val toneType = if (pulseIndex == 3) {
                        ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
                    } else {
                        ToneGenerator.TONE_PROP_BEEP2
                    }
                    runCatching { toneGen?.startTone(toneType, 500) }
                    delay(850L)
                }
            } finally {
                runCatching { ringtone?.stop() }
                runCatching { toneGen?.release() }
            }
        }
    }
}
