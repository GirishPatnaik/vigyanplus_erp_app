package com.vigyan.juniorcollege.data.repository

import android.content.Context
import android.net.Uri
import com.vigyan.juniorcollege.data.local.VigyanDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupFile(val file: File, val createdAt: Long)

class BackupRepository(private val context: Context) {

    private fun backupDir(): File =
        File(context.getExternalFilesDir(null), "backups").apply { mkdirs() }

    private fun dbFile(): File = context.getDatabasePath(VigyanDatabase.DB_NAME)

    /** Manual backup: copies the live SQLite file into the app's backups folder. */
    fun createManualBackup(): BackupFile {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val target = File(backupDir(), "vigyan_backup_$timestamp.db")
        dbFile().copyTo(target, overwrite = true)
        return BackupFile(target, System.currentTimeMillis())
    }

    /** Automatic backup - same mechanism, called from a WorkManager/Scheduled job. */
    fun createAutomaticBackup(): BackupFile {
        val backup = createManualBackup()
        pruneOldBackups()
        return backup
    }

    /** Keeps only the most recent [keep] backups on disk to avoid unbounded storage growth. */
    fun pruneOldBackups(keep: Int = 10) {
        val all = listBackups()
        if (all.size > keep) {
            all.drop(keep).forEach { it.file.delete() }
        }
    }

    fun listBackups(): List<BackupFile> =
        backupDir().listFiles { f -> f.extension == "db" }
            ?.sortedByDescending { it.lastModified() }
            ?.map { BackupFile(it, it.lastModified()) }
            ?: emptyList()

    /** Exports a chosen backup (or the live DB) to a user-selected Uri (SAF), e.g. Downloads. */
    fun exportDatabase(destinationUri: Uri, sourceFile: File = dbFile()) {
        context.contentResolver.openOutputStream(destinationUri)?.use { out ->
            sourceFile.inputStream().use { input -> input.copyTo(out) }
        }
    }

    /**
     * Imports a .db file selected by the user (SAF) and overwrites the live database.
     * Caller must close all open Room connections and restart the app afterward.
     */
    fun importDatabase(sourceUri: Uri) {
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            dbFile().outputStream().use { out -> input.copyTo(out) }
        }
    }

    fun deleteBackup(backup: BackupFile) {
        backup.file.delete()
    }
}
