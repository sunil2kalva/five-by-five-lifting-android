package com.fivebyfivelifting.app.domain

import com.fivebyfivelifting.app.data.local.BodyweightEntryEntity
import com.fivebyfivelifting.app.data.local.ExerciseStateEntity
import com.fivebyfivelifting.app.data.local.WorkoutSessionWithSets
import com.fivebyfivelifting.app.data.model.WeightUnit
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Serializable
data class AppBackupSnapshot(
    val schemaVersion: Int = 1,
    val exportedAtEpochMillis: Long,
    val weightUnit: WeightUnit = WeightUnit.LB,
    val barbellWeightLb: Double = 45.0,
    val exerciseStates: List<ExerciseStateEntity>,
    val sessions: List<WorkoutSessionWithSets>,
    val bodyweightEntries: List<BodyweightEntryEntity>
)

object BackupExportManager {

    private val jsonFormat = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun toJson(snapshot: AppBackupSnapshot): String {
        return jsonFormat.encodeToString(snapshot)
    }

    fun fromJson(jsonString: String): AppBackupSnapshot {
        return jsonFormat.decodeFromString(jsonString)
    }

    /**
     * Conflict-free Smart Cloud Merge when restoring a Google Drive backup on a new phone.
     * - Deduplicates workout sessions by (startedAtEpochMillis, workoutType).
     * - Preserves all unique workouts from both local and cloud snapshots.
     * - Selects the exercise progression state from whichever snapshot was updated/exported more recently
     *   (or has the higher completed workout count).
     */
    fun mergeSnapshots(
        local: AppBackupSnapshot,
        cloud: AppBackupSnapshot
    ): AppBackupSnapshot {
        val mergedSessionsMap = linkedMapOf<String, WorkoutSessionWithSets>()

        for (sessionWithSets in (local.sessions + cloud.sessions)) {
            val key = "${sessionWithSets.session.startedAtEpochMillis}_${sessionWithSets.session.workoutType.name}"
            val existing = mergedSessionsMap[key]
            if (existing == null ||
                (sessionWithSets.session.isCompleted && !existing.session.isCompleted) ||
                sessionWithSets.sets.size > existing.sets.size
            ) {
                mergedSessionsMap[key] = sessionWithSets
            }
        }

        val mergedSessions = mergedSessionsMap.values.sortedBy { it.session.startedAtEpochMillis }

        val mergedBodyweight = (local.bodyweightEntries + cloud.bodyweightEntries)
            .distinctBy { "${it.recordedAtEpochMillis}_${it.weightLb}" }
            .sortedBy { it.recordedAtEpochMillis }

        val preferCloudStates = cloud.sessions.count { it.session.isCompleted } >
            local.sessions.count { it.session.isCompleted } ||
            cloud.exportedAtEpochMillis >= local.exportedAtEpochMillis

        val chosenStates = if (preferCloudStates && cloud.exerciseStates.isNotEmpty()) {
            cloud.exerciseStates
        } else {
            local.exerciseStates
        }

        return AppBackupSnapshot(
            schemaVersion = 1,
            exportedAtEpochMillis = maxOf(local.exportedAtEpochMillis, cloud.exportedAtEpochMillis),
            weightUnit = if (preferCloudStates) cloud.weightUnit else local.weightUnit,
            barbellWeightLb = if (preferCloudStates) cloud.barbellWeightLb else local.barbellWeightLb,
            exerciseStates = chosenStates,
            sessions = mergedSessions,
            bodyweightEntries = mergedBodyweight
        )
    }

    /**
     * Exports all completed workout sessions and sets to a clean CSV spreadsheet string.
     */
    fun toCsv(
        sessions: List<WorkoutSessionWithSets>,
        unit: WeightUnit = WeightUnit.LB
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Date,Workout,Exercise,SetNumber,SetType,TargetWeight,Unit,TargetReps,CompletedReps,Bodyweight")

        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }

        for (sessionWithSets in sessions.sortedBy { it.session.startedAtEpochMillis }) {
            val session = sessionWithSets.session
            val dateStr = dateFormatter.format(Date(session.completedAtEpochMillis ?: session.startedAtEpochMillis))
            val bwStr = session.bodyweightLb?.let { unit.format(it, includeUnit = false) } ?: ""

            val orderedSets = sessionWithSets.sets.sortedWith(
                compareBy({ it.exercise.ordinal }, { !it.isWarmup }, { it.setNumber })
            )

            for (set in orderedSets) {
                val typeStr = if (set.isWarmup) "Warmup" else "Work"
                val weightStr = unit.format(set.targetWeightLb, includeUnit = false)
                val completedStr = set.completedReps?.toString() ?: ""
                sb.appendLine(
                    "\"$dateStr\",${session.workoutType.code},\"${set.exercise.displayName}\",${set.setNumber},$typeStr,$weightStr,${unit.label},${set.targetReps},$completedStr,$bwStr"
                )
            }
        }
        return sb.toString()
    }
}
