package com.stronglifts5x5.app

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.stronglifts5x5.app.ui.StrengthViewModel
import com.stronglifts5x5.app.ui.screens.GraphsScreen
import com.stronglifts5x5.app.ui.screens.HistoryScreen
import com.stronglifts5x5.app.ui.screens.HomeScreen
import com.stronglifts5x5.app.ui.screens.SettingsScreen
import com.stronglifts5x5.app.ui.screens.WorkoutScreen
import com.stronglifts5x5.app.ui.theme.StrongLifts5x5Theme

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

    private val viewModel: StrengthViewModel by viewModels {
        val app = application as StrongLiftsApplication
        StrengthViewModel.Factory(app.workoutRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StrongLifts5x5Theme {
                val uiState by viewModel.uiState.collectAsState()
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }

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
                                    onClick = {
                                        navController.navigate(tab.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
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
                                        navController.navigate(MainTab.WORKOUT.route)
                                    }
                                },
                                onUpdateExerciseWeight = viewModel::updateWorkingWeight,
                                onNavigateToSettings = {
                                    navController.navigate(MainTab.SETTINGS.route)
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
                                onAddThirtySecondsRest = {
                                    viewModel.adjustRestTimerSeconds(30)
                                },
                                onSwitchRestToThreeMinutes = viewModel::switchRestTimerToThreeMinutes,
                                onSkipRestTimer = viewModel::stopRestTimer,
                                onFinishWorkout = { bw, notes ->
                                    viewModel.finishActiveWorkout(bw, notes) {}
                                },
                                onDiscardWorkout = {
                                    viewModel.discardActiveWorkout {
                                        navController.navigate(MainTab.HOME.route)
                                    }
                                },
                                onDismissSummary = {
                                    viewModel.dismissCompletionSummary()
                                    navController.navigate(MainTab.HOME.route)
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
                                onUpdateExerciseWeight = viewModel::updateWorkingWeight,
                                onToggleAutoSync = viewModel::setAutoSyncEnabled,
                                onLinkGoogleDriveSyncFile = viewModel::linkGoogleDriveSyncFile,
                                onTriggerManualCloudSync = viewModel::triggerManualCloudSync,
                                onRestoreFromGoogleDriveUri = viewModel::restoreOrMergeFromGoogleDriveUri,
                                onExportCsvToUri = viewModel::exportCsvToUri
                            )
                        }
                    }
                }
            }
        }
    }

    private fun playRestFinishedAlert() {
        runCatching {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
        }
    }
}
