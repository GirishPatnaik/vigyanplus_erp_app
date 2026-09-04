package com.vigyan.juniorcollege.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "import_history")
data class ImportHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val totalRecords: Int,
    val successful: Int,
    val failed: Int,
    val errorDetails: String = "",
    val importDate: Long = System.currentTimeMillis()
)
