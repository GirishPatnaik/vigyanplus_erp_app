package com.vigyan.juniorcollege.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole { ADMINISTRATOR, STAFF }

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val role: UserRole,
    val fullName: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
