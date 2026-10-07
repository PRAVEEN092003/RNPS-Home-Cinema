package com.cineplex.app.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cineplex.app.data.model.UserDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cinema_session")

/**
 * Manages the local JWT session token and cached user profile.
 * Uses DataStore Preferences for persistence.
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val KEY_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_USER = stringPreferencesKey("user_json")
    }

    @Volatile
    private var _cachedToken: String? = null

    // ─── Token ────────────────────────────────────────────────────────────────

    val tokenFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        val token = prefs[KEY_TOKEN]
        _cachedToken = token
        token
    }

    fun getTokenSync(): String? = _cachedToken

    suspend fun getToken(): String? {
        val token = context.dataStore.data.first()[KEY_TOKEN]
        _cachedToken = token
        return token
    }

    suspend fun saveToken(token: String) {
        _cachedToken = token
        context.dataStore.edit { it[KEY_TOKEN] = token }
    }

    // ─── User ─────────────────────────────────────────────────────────────────

    val userFlow: Flow<UserDto?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER]?.let { runCatching { json.decodeFromString<UserDto>(it) }.getOrNull() }
    }

    suspend fun getUser(): UserDto? {
        val raw = context.dataStore.data.first()[KEY_USER] ?: return null
        return runCatching { json.decodeFromString<UserDto>(raw) }.getOrNull()
    }

    suspend fun saveUser(user: UserDto) {
        context.dataStore.edit { it[KEY_USER] = json.encodeToString(user) }
    }

    // ─── Session ──────────────────────────────────────────────────────────────

    suspend fun isLoggedIn(): Boolean = getToken() != null

    suspend fun clearSession() {
        _cachedToken = null
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_TOKEN)
            prefs.remove(KEY_USER)
        }
    }
}
