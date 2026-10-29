package com.stronglifts5x5.app.domain

import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages:
 * 1. Picking a specific Google Account logged into the Android device via `AccountManager`.
 * 2. Opening that specific Google Account's Google Drive file system (`com.google.android.apps.docs.storage`)
 *    when creating or restoring a backup file.
 * 3. Launching the official Google Drive app (`com.google.android.apps.docs`) directly so the user
 *    can browse their Google Drive folders and save the backup inside the Drive app UI.
 */
class CloudSyncManager(private val context: Context) {

    companion object {
        const val GOOGLE_DRIVE_PACKAGE = "com.google.android.apps.docs"
        const val GOOGLE_DRIVE_STORAGE_AUTHORITY = "com.google.android.apps.docs.storage"
    }

    /**
     * Builds the system Google Account Picker intent listing all `com.google` accounts
     * logged into the user's Samsung / Android device.
     */
    fun buildChooseGoogleAccountIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AccountManager.newChooseAccountIntent(
                null,
                null,
                arrayOf("com.google"),
                null,
                null,
                null,
                null
            )
        } else {
            @Suppress("DEPRECATION")
            AccountManager.newChooseAccountIntent(
                null,
                null,
                arrayOf("com.google"),
                false,
                null,
                null,
                null,
                null
            )
        }
    }

    /**
     * Builds a URI pointing directly to the selected Google Account's Google Drive root
     * inside Android's Storage Access Framework (`com.google.android.apps.docs.storage`).
     */
    fun buildGoogleDriveInitialUri(accountEmail: String?): Uri {
        return if (!accountEmail.isNullOrBlank()) {
            DocumentsContract.buildRootUri(GOOGLE_DRIVE_STORAGE_AUTHORITY, accountEmail.trim())
        } else {
            DocumentsContract.buildRootsUri(GOOGLE_DRIVE_STORAGE_AUTHORITY)
        }
    }

    /**
     * Builds an ACTION_CREATE_DOCUMENT intent pre-targeted at the user's selected Google Account
     * inside Google Drive (`com.google.android.apps.docs.storage`).
     */
    fun buildCreateGoogleDriveFileIntent(
        fileName: String,
        mimeType: String = "application/json",
        accountEmail: String?
    ): Intent {
        val initialUri = buildGoogleDriveInitialUri(accountEmail)
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, fileName)
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }
    }

    /**
     * Builds an ACTION_OPEN_DOCUMENT intent pre-targeted at the user's selected Google Account
     * inside Google Drive (`com.google.android.apps.docs.storage`).
     */
    fun buildOpenGoogleDriveFileIntent(accountEmail: String?): Intent {
        val initialUri = buildGoogleDriveInitialUri(accountEmail)
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/plain", "*/*"))
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }
    }

    /**
     * Writes the latest JSON backup snapshot to the app's backup file (`stronglifts5x5_cloud_sync.json`).
     */
    suspend fun writeLocalBackupMirror(snapshot: AppBackupSnapshot): File = withContext(Dispatchers.IO) {
        val backupDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val backupFile = File(backupDir, "stronglifts5x5_cloud_sync.json")
        val jsonPayload = BackupExportManager.toJson(snapshot)
        backupFile.writeText(jsonPayload, Charsets.UTF_8)
        backupFile
    }

    /**
     * Writes the JSON snapshot to a temporary file and builds an intent that opens the
     * official Google Drive app (`com.google.android.apps.docs`) directly so the user sees
     * Google Drive's own native Account & Folder picker screen.
     */
    suspend fun buildSaveDirectlyToGoogleDriveAppIntent(
        snapshot: AppBackupSnapshot,
        accountEmail: String?
    ): Result<Intent> = withContext(Dispatchers.IO) {
        runCatching {
            val backupFile = writeLocalBackupMirror(snapshot)

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                backupFile
            )

            val driveIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "stronglifts5x5_cloud_sync.json")
                if (!accountEmail.isNullOrBlank()) {
                    putExtra("authAccount", accountEmail)
                    putExtra(AccountManager.KEY_ACCOUNT_NAME, accountEmail)
                }
                setPackage(GOOGLE_DRIVE_PACKAGE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Verify if the official Google Drive app is installed; otherwise wrap in chooser
            val pm = context.packageManager
            if (driveIntent.resolveActivity(pm) != null) {
                driveIntent
            } else {
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        putExtra(Intent.EXTRA_SUBJECT, "stronglifts5x5_cloud_sync.json")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    "Save Backup to Google Drive"
                )
            }
        }
    }

    fun persistDriveUriPermission(uri: Uri): Boolean {
        return try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            true
        } catch (_: SecurityException) {
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
