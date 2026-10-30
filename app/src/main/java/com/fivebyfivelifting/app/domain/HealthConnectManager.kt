package com.fivebyfivelifting.app.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import java.time.Instant
import java.time.ZoneId

object HealthConnectManager {

    const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"

    val requiredPermissions: Set<String> = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    )

    fun createPermissionRequestContract() =
        PermissionController.createRequestPermissionResultContract()

    fun isAvailable(context: Context): Boolean {
        return runCatching {
            HealthConnectClient.getSdkStatus(context, HEALTH_CONNECT_PACKAGE) ==
                HealthConnectClient.SDK_AVAILABLE
        }.getOrDefault(false)
    }

    fun getSdkStatus(context: Context): Int {
        return runCatching {
            HealthConnectClient.getSdkStatus(context, HEALTH_CONNECT_PACKAGE)
        }.getOrDefault(HealthConnectClient.SDK_UNAVAILABLE)
    }

    suspend fun hasAllPermissions(context: Context): Boolean {
        if (!isAvailable(context)) return false
        return runCatching {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            granted.containsAll(requiredPermissions)
        }.getOrDefault(false)
    }

    fun buildHealthConnectSettingsIntent(): Intent {
        return runCatching {
            Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
        }.getOrElse {
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=$HEALTH_CONNECT_PACKAGE")
            )
        }
    }

    suspend fun writeCompletedWorkoutSession(
        context: Context,
        workoutTitle: String,
        startEpochMillis: Long,
        endEpochMillis: Long,
        summaryNotes: String
    ): Result<Unit> = runCatching {
        val status = getSdkStatus(context)
        if (status != HealthConnectClient.SDK_AVAILABLE) {
            error("Health Connect is not available on this device (status=$status).")
        }
        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        if (!granted.contains(HealthPermission.getWritePermission(ExerciseSessionRecord::class))) {
            error("Health Connect Exercise write permission has not been granted.")
        }

        // Ensure startTime is strictly before endTime for Health Connect record validation
        val safeEndMillis = if (endEpochMillis <= startEpochMillis) {
            startEpochMillis + 60_000L
        } else {
            endEpochMillis
        }
        val safeStartMillis = if (safeEndMillis - startEpochMillis < 10_000L) {
            safeEndMillis - 60_000L
        } else {
            startEpochMillis
        }

        val startInstant = Instant.ofEpochMilli(safeStartMillis)
        val endInstant = Instant.ofEpochMilli(safeEndMillis)
        val zoneId = ZoneId.systemDefault()
        val startOffset = zoneId.rules.getOffset(startInstant)
        val endOffset = zoneId.rules.getOffset(endInstant)

        val record = ExerciseSessionRecord(
            startTime = startInstant,
            startZoneOffset = startOffset,
            endTime = endInstant,
            endZoneOffset = endOffset,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING,
            title = "Five By Five Lifting • $workoutTitle",
            notes = summaryNotes
        )

        client.insertRecords(listOf(record))
    }
}
