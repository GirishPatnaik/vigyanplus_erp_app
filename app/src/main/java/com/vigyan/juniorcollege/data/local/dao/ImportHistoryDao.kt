package com.vigyan.juniorcollege.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.vigyan.juniorcollege.data.local.entity.ImportHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportHistoryDao {
    @Insert
    suspend fun insert(entity: ImportHistoryEntity): Long

    @Query("SELECT * FROM import_history ORDER BY importDate DESC")
    fun observeAll(): Flow<List<ImportHistoryEntity>>
}
