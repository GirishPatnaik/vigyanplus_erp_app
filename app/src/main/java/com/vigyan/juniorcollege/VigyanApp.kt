package com.vigyan.juniorcollege

import android.app.Application
import com.vigyan.juniorcollege.data.local.VigyanDatabase
import com.vigyan.juniorcollege.data.repository.*
import com.vigyan.juniorcollege.util.SessionManager
import com.vigyan.juniorcollege.work.AutoBackupScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VigyanApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: VigyanDatabase by lazy { VigyanDatabase.getInstance(this) }

    val sessionManager: SessionManager by lazy { SessionManager(this) }

    val authRepository: AuthRepository by lazy {
        AuthRepository(database.userDao(), database.auditLogDao())
    }
    val studentRepository: StudentRepository by lazy { StudentRepository(database.studentDao()) }
    val attendanceRepository: AttendanceRepository by lazy { AttendanceRepository(database.attendanceDao()) }
    val masterRepository: MasterRepository by lazy { MasterRepository(database.masterDao()) }
    val csvRepository: CsvRepository by lazy {
        CsvRepository(this, database.studentDao(), database.masterDao(), database.importHistoryDao())
    }
    val backupRepository: BackupRepository by lazy { BackupRepository(this) }
    val syncRepository: SyncRepository by lazy {
        SyncRepository(this, database.studentDao(), database.attendanceDao(), database.masterDao())
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            authRepository.ensureDefaultAdmin()
            masterRepository.seedDefaults()
        }
        AutoBackupScheduler.schedule(this)
    }
}
