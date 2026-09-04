package com.vigyan.juniorcollege.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AttendanceStatus { PRESENT, ABSENT }

@Entity(
    tableName = "attendance",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["studentId", "date"], unique = true), Index(value = ["date"])]
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val date: String,                 // yyyy-MM-dd
    val status: AttendanceStatus,
    val classId: Long? = null,
    val batchId: Long? = null,
    val markedByUserId: Long? = null,
    val markedAt: Long = System.currentTimeMillis()
)
