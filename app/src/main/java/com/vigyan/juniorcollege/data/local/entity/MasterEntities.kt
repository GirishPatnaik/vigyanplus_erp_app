package com.vigyan.juniorcollege.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "academic_years")
data class AcademicYearEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,          // e.g. "2025-2026"
    val isCurrent: Boolean = false,
    val remoteId: String? = null
)

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,            // e.g. "Science", "Commerce"
    val remoteId: String? = null
)

@Entity(tableName = "streams")
data class StreamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,           // e.g. "PCM", "PCB", "CBA"
    val courseId: Long,
    val remoteId: String? = null
)

@Entity(tableName = "college_classes")
data class ClassEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,            // e.g. "+2 First Year", "+2 Second Year"
    val remoteId: String? = null
)

@Entity(tableName = "batches")
data class BatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,             // e.g. "2025-27"
    val remoteId: String? = null
)

@Entity(tableName = "sections")
data class SectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,              // e.g. "A", "B"
    val remoteId: String? = null
)

@Entity(tableName = "college_details")
data class CollegeDetailsEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "VIGYAN INTERNATIONAL JUNIOR COLLEGE",
    val address: String = "Koraput, Odisha",
    val phone: String = "",
    val email: String = "",
    val affiliationBoard: String = "CHSE Odisha",
    val updatedAt: Long = System.currentTimeMillis(),
    val remoteId: String? = null
)
