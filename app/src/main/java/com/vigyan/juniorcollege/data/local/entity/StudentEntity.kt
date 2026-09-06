package com.vigyan.juniorcollege.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class Gender { MALE, FEMALE, OTHER }

enum class StudentStatus { ACTIVE, INACTIVE, TRANSFER, CANCELLED }

@Entity(
    tableName = "students",
    indices = [
        Index(value = ["admissionNo"], unique = true),
        Index(value = ["classId", "batchId", "sectionId"])
    ]
)
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    val photoUri: String? = null,

    val batchId: Long? = null,
    val admissionNo: String,
    val rollNo: String = "",
    val academicYearId: Long? = null,

    val studentName: String,
    val fatherName: String = "",
    val motherName: String = "",
    val dob: String = "",              // stored as yyyy-MM-dd
    val gender: Gender = Gender.MALE,
    val address: String = "",

    val mobileNo1: String = "",
    val mobileNo2: String = "",
    val whatsappNo: String = "",

    val boardName: String = "CHSE Odisha",
    val courseId: Long? = null,
    val streamId: Long? = null,
    val classId: Long? = null,
    val sectionId: Long? = null,

    // Aadhaar stored encrypted (Fernet-equivalent via AndroidX security-crypto elsewhere)
    val aadhaarEncrypted: String? = null,

    val status: StudentStatus = StudentStatus.ACTIVE,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,     // recycle bin flag

    // Cloud sync tracking (Supabase row UUID once this student has been synced at least once)
    val remoteId: String? = null
)
