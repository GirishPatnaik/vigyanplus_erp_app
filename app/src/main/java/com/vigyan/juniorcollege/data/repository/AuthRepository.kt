package com.vigyan.juniorcollege.data.repository

import com.vigyan.juniorcollege.data.local.dao.AuditLogDao
import com.vigyan.juniorcollege.data.local.dao.UserDao
import com.vigyan.juniorcollege.data.local.entity.AuditLogEntity
import com.vigyan.juniorcollege.data.local.entity.UserEntity
import com.vigyan.juniorcollege.data.local.entity.UserRole
import com.vigyan.juniorcollege.util.PasswordHasher

sealed class LoginResult {
    data class Success(val user: UserEntity) : LoginResult()
    object InvalidCredentials : LoginResult()
    object AccountDisabled : LoginResult()
}

class AuthRepository(
    private val userDao: UserDao,
    private val auditLogDao: AuditLogDao
) {
    suspend fun getUserById(id: Long): UserEntity? = userDao.findById(id)

    suspend fun ensureDefaultAdmin() {
        if (userDao.count() == 0) {
            userDao.upsert(
                UserEntity(
                    username = "admin",
                    passwordHash = PasswordHasher.hash("admin123"),
                    role = UserRole.ADMINISTRATOR,
                    fullName = "System Administrator"
                )
            )
        }
    }

    suspend fun login(username: String, password: String): LoginResult {
        val user = userDao.findByUsername(username.trim()) ?: return LoginResult.InvalidCredentials
        if (!user.isActive) return LoginResult.AccountDisabled
        return if (PasswordHasher.verify(password, user.passwordHash)) {
            log(user, "LOGIN", "User logged in")
            LoginResult.Success(user)
        } else {
            LoginResult.InvalidCredentials
        }
    }

    suspend fun changePassword(user: UserEntity, currentPassword: String, newPassword: String): Boolean {
        if (!PasswordHasher.verify(currentPassword, user.passwordHash)) return false
        userDao.update(user.copy(passwordHash = PasswordHasher.hash(newPassword)))
        log(user, "CHANGE_PASSWORD", "Password changed")
        return true
    }

    suspend fun log(user: UserEntity, action: String, details: String = "") {
        auditLogDao.insert(AuditLogEntity(userId = user.id, userName = user.fullName, action = action, details = details))
    }

    suspend fun logout(user: UserEntity) {
        log(user, "LOGOUT", "User logged out")
    }
}
