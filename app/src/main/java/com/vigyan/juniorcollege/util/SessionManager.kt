package com.vigyan.juniorcollege.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "vigyan_session")

class SessionManager(private val context: Context) {

    private val KEY_USER_ID = longPreferencesKey("user_id")
    private val KEY_USER_NAME = stringPreferencesKey("user_name")
    private val KEY_USER_ROLE = stringPreferencesKey("user_role")

    val userId: Flow<Long?> = context.dataStore.data.map { it[KEY_USER_ID] }
    val userName: Flow<String?> = context.dataStore.data.map { it[KEY_USER_NAME] }
    val userRole: Flow<String?> = context.dataStore.data.map { it[KEY_USER_ROLE] }

    suspend fun login(userId: Long, name: String, role: String) {
        context.dataStore.edit {
            it[KEY_USER_ID] = userId
            it[KEY_USER_NAME] = name
            it[KEY_USER_ROLE] = role
        }
    }

    suspend fun logout() {
        context.dataStore.edit { it.clear() }
    }
}
