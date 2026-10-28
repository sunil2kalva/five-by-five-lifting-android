package com.stronglifts5x5.app.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages automatic background synchronization of the local Room SQLite database
 * to a persistent file inside the user's personal Google Drive (via Android's
 * Storage Access Framework / Google Drive DocumentProvider).
 *
 * Why this works with $0 cloud server cost:
 * - Every Android phone with Google Play Services includes the Google Drive DocumentProvider.
 * - Once the user selects or creates `stronglifts5x5_cloud_sync.json` inside their Google Drive,
 *   we call `takePersistableUriPermission(READ | WRITE)`.
 * - After every completed workout, the app automatically writes the updated JSON snapshot
 *   to that Google Drive URI in the background, and Google Drive uploads it to the user's cloud.
 * - On a new Android phone, selecting that file from Google Drive immediately restores/merges
 *   all historical workouts and links future auto-syncs.
 */
class CloudSyncManager(private val context: Context) {

    fun persistDriveUriPermission(uri: Uri): Boolean {
        return try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            true
        } catch (_: SecurityException) {
            // Even if persistable permission is not supported by a custom provider,
            // one-shot read/write still works for the current session.
            false
        }
    }

    suspend fun writeSnapshotToUri(uri: Uri, snapshot: AppBackupSnapshot): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val jsonPayload = BackupExportManager.toJson(snapshot)
                context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                    out.write(jsonPayload.toByteArray(Charsets.UTF_8))
                    out.flush()
                } ?: error("Unable to open output stream for Google Drive URI: $uri")
            }
        }

    suspend fun readSnapshotFromUri(uri: Uri): Result<AppBackupSnapshot> =
        withContext(Dispatchers.IO) {
            runCatching {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                } ?: error("Unable to open input stream for Google Drive URI: $uri")
                BackupExportManager.fromJson(jsonString)
            }
        }

    suspend fun writeCsvToUri(uri: Uri, csvContent: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                    out.write(csvContent.toByteArray(Charsets.UTF_8))
                    out.flush()
                } ?: error("Unable to open output stream for CSV URI: $uri")
            }
        }
}
