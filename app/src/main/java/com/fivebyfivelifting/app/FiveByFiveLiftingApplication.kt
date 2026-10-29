package com.fivebyfivelifting.app

import android.app.Application
import com.fivebyfivelifting.app.data.local.AppDatabase
import com.fivebyfivelifting.app.data.preferences.UserPreferencesRepository
import com.fivebyfivelifting.app.data.repository.WorkoutRepository
import com.fivebyfivelifting.app.domain.CloudSyncManager

class FiveByFiveLiftingApplication : Application() {

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
