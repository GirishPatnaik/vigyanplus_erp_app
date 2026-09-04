package com.vigyan.juniorcollege.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.vigyan.juniorcollege.data.local.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {
    @Insert
    suspend fun insert(entry: AuditLogEntity)

    @Query("SELECT * FROM audit_log ORDER BY timestamp DESC LIMIT 500")
    fun observeRecent(): Flow<List<AuditLogEntity>>
}
