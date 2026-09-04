package com.vigyan.juniorcollege.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vigyan.juniorcollege.data.repository.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Runs on a daily schedule (see [AutoBackupScheduler]) to copy the live database
 * into the app's backups folder and prune older automatic backups.
 */
class AutoBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            BackupRepository(applicationContext).createAutomaticBackup()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
