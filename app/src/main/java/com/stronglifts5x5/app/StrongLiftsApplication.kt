package com.stronglifts5x5.app

import android.app.Application
import com.stronglifts5x5.app.data.local.AppDatabase
import com.stronglifts5x5.app.data.preferences.UserPreferencesRepository
import com.stronglifts5x5.app.data.repository.WorkoutRepository
import com.stronglifts5x5.app.domain.CloudSyncManager

class StrongLiftsApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val preferencesRepository: UserPreferencesRepository by lazy { UserPreferencesRepository(this) }
    val cloudSyncManager: CloudSyncManager by lazy { CloudSyncManager(this) }
    val workoutRepository: WorkoutRepository by lazy {
        WorkoutRepository(
            dao = database.workoutDao(),
            preferencesRepository = preferencesRepository,
            cloudSyncManager = cloudSyncManager
        )
    }
}
