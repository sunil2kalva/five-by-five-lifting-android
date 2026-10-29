package com.fivebyfivelifting.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fivebyfivelifting.app.data.model.WeightUnit
import com.fivebyfivelifting.app.data.model.WorkoutType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

data class UserPreferences(
    val weightUnit: WeightUnit = WeightUnit.LB,
    val barbellWeightLb: Double = 45.0,
    val nextWorkoutOverride: WorkoutType? = null,
    val googleAccountEmail: String? = null,
    val cloudSyncUriString: String? = null,
    val cloudBackupFileName: String? = null,
    val cloudBackupConfigured: Boolean = false,
    val autoSyncEnabled: Boolean = true,
    val lastCloudSyncEpochMillis: Long? = null,
    val soundAndVibrationEnabled: Boolean = true
) {
    val isBackupFileConfigured: Boolean
        get() = cloudBackupConfigured ||
            !cloudSyncUriString.isNullOrBlank() ||
            lastCloudSyncEpochMillis != null

    val isBackupSetupComplete: Boolean
        get() = !googleAccountEmail.isNullOrBlank() && isBackupFileConfigured

    val isSyncConfiguredAndWorking: Boolean
        get() = isBackupSetupComplete && autoSyncEnabled

    val effectiveBackupFileName: String
        get() = cloudBackupFileName?.takeIf { it.isNotBlank() }
            ?: "fivebyfivelifting_cloud_sync.json"
}

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val BARBELL_WEIGHT_LB = doublePreferencesKey("barbell_weight_lb")
        val NEXT_WORKOUT_OVERRIDE = stringPreferencesKey("next_workout_override")
        val GOOGLE_ACCOUNT_EMAIL = stringPreferencesKey("google_account_email")
        val CLOUD_SYNC_URI = stringPreferencesKey("cloud_sync_uri")
        val CLOUD_BACKUP_FILE_NAME = stringPreferencesKey("cloud_backup_file_name")
        val CLOUD_BACKUP_CONFIGURED = booleanPreferencesKey("cloud_backup_configured")
        val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        val LAST_CLOUD_SYNC_MS = longPreferencesKey("last_cloud_sync_ms")
        val SOUND_VIBRATION = booleanPreferencesKey("sound_vibration_enabled")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val unit = prefs[Keys.WEIGHT_UNIT]?.let {
            runCatching { WeightUnit.valueOf(it) }.getOrNull()
        } ?: WeightUnit.LB
        val barLb = prefs[Keys.BARBELL_WEIGHT_LB] ?: unit.defaultBarWeightLb
        val overrideWorkout = prefs[Keys.NEXT_WORKOUT_OVERRIDE]?.let {
            runCatching { WorkoutType.valueOf(it) }.getOrNull()
        }
        UserPreferences(
            weightUnit = unit,
            barbellWeightLb = barLb,
            nextWorkoutOverride = overrideWorkout,
            googleAccountEmail = prefs[Keys.GOOGLE_ACCOUNT_EMAIL],
            cloudSyncUriString = prefs[Keys.CLOUD_SYNC_URI],
            cloudBackupFileName = prefs[Keys.CLOUD_BACKUP_FILE_NAME],
            cloudBackupConfigured = prefs[Keys.CLOUD_BACKUP_CONFIGURED] ?: false,
            autoSyncEnabled = prefs[Keys.AUTO_SYNC_ENABLED] ?: true,
            lastCloudSyncEpochMillis = prefs[Keys.LAST_CLOUD_SYNC_MS],
            soundAndVibrationEnabled = prefs[Keys.SOUND_VIBRATION] ?: true
        )
    }

    suspend fun setWeightUnit(unit: WeightUnit) {
        context.dataStore.edit { prefs ->
            prefs[Keys.WEIGHT_UNIT] = unit.name
            prefs[Keys.BARBELL_WEIGHT_LB] = unit.defaultBarWeightLb
        }
    }

    suspend fun setBarbellWeightLb(weightLb: Double) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BARBELL_WEIGHT_LB] = weightLb
        }
    }

    suspend fun setNextWorkoutOverride(workoutType: WorkoutType?) {
        context.dataStore.edit { prefs ->
            if (workoutType == null) {
                prefs.remove(Keys.NEXT_WORKOUT_OVERRIDE)
            } else {
                prefs[Keys.NEXT_WORKOUT_OVERRIDE] = workoutType.name
            }
        }
    }

    suspend fun setGoogleAccountEmail(email: String?) {
        context.dataStore.edit { prefs ->
            if (email.isNullOrBlank()) {
                prefs.remove(Keys.GOOGLE_ACCOUNT_EMAIL)
            } else {
                prefs[Keys.GOOGLE_ACCOUNT_EMAIL] = email.trim()
            }
        }
    }

    suspend fun setCloudSyncUri(uriString: String?) {
        context.dataStore.edit { prefs ->
            if (uriString.isNullOrBlank()) {
                prefs.remove(Keys.CLOUD_SYNC_URI)
            } else {
                prefs[Keys.CLOUD_SYNC_URI] = uriString
                prefs[Keys.CLOUD_BACKUP_CONFIGURED] = true
                prefs[Keys.AUTO_SYNC_ENABLED] = true
            }
        }
    }

    suspend fun markCloudBackupFileConfigured(
        fileName: String = "fivebyfivelifting_cloud_sync.json",
        timestampMillis: Long = System.currentTimeMillis()
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CLOUD_BACKUP_FILE_NAME] = fileName
            prefs[Keys.CLOUD_BACKUP_CONFIGURED] = true
            prefs[Keys.AUTO_SYNC_ENABLED] = true
            prefs[Keys.LAST_CLOUD_SYNC_MS] = timestampMillis
        }
    }

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_SYNC_ENABLED] = enabled
        }
    }

    suspend fun recordCloudSyncSuccess(timestampMillis: Long = System.currentTimeMillis()) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CLOUD_BACKUP_CONFIGURED] = true
            prefs[Keys.LAST_CLOUD_SYNC_MS] = timestampMillis
        }
    }

    suspend fun setSoundAndVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SOUND_VIBRATION] = enabled
        }
    }
}
