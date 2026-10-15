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
 * Manages the local JWT session token, language preference, and cached user profile.
 * Uses fast in-memory caching backed by SharedPreferences for instant startup and DataStore for persistent state.
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val sharedPrefs = context.getSharedPreferences("cinema_app_prefs", Context.MODE_PRIVATE)

    companion object {
        private val KEY_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_USER = stringPreferencesKey("user_json")
        private val KEY_LANGUAGE = stringPreferencesKey("app_language")
        private const val PREF_KEY_TOKEN = "auth_token"
        private const val PREF_KEY_USER = "user_json"
        private const val PREF_KEY_LANGUAGE = "app_language"
    }

    // ─── Language ─────────────────────────────────────────────────────────────

    @Volatile
    private var _cachedLanguage: String = sharedPrefs.getString(PREF_KEY_LANGUAGE, "en") ?: "en"

    val languageFlow: Flow<String> = context.dataStore.data.map { prefs ->
        val lang = prefs[KEY_LANGUAGE] ?: sharedPrefs.getString(PREF_KEY_LANGUAGE, "en") ?: "en"
        _cachedLanguage = lang
        lang
    }

    fun getLanguageSync(): String = _cachedLanguage

    suspend fun getLanguage(): String {
        val dsLang = context.dataStore.data.first()[KEY_LANGUAGE]
        val resolved = dsLang ?: _cachedLanguage
        _cachedLanguage = resolved
        return resolved
    }

    suspend fun setLanguage(language: String) {
        _cachedLanguage = language
        sharedPrefs.edit().putString(PREF_KEY_LANGUAGE, language).apply()
        context.dataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = language
        }
    }

    // ─── Token ────────────────────────────────────────────────────────────────

    @Volatile
    private var _cachedToken: String? = sharedPrefs.getString(PREF_KEY_TOKEN, null)

    val tokenFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        val token = prefs[KEY_TOKEN] ?: sharedPrefs.getString(PREF_KEY_TOKEN, null)
        _cachedToken = token
        token
    }

    fun getTokenSync(): String? = _cachedToken

    suspend fun getToken(): String? {
        if (!_cachedToken.isNullOrBlank()) return _cachedToken
        val token = context.dataStore.data.first()[KEY_TOKEN] ?: sharedPrefs.getString(PREF_KEY_TOKEN, null)
        _cachedToken = token
        return token
    }

    suspend fun saveToken(token: String) {
        _cachedToken = token
        sharedPrefs.edit().putString(PREF_KEY_TOKEN, token).apply()
        context.dataStore.edit { it[KEY_TOKEN] = token }
    }

    // ─── User ─────────────────────────────────────────────────────────────────

    @Volatile
    private var _cachedUser: UserDto? = sharedPrefs.getString(PREF_KEY_USER, null)?.let {
        runCatching { json.decodeFromString<UserDto>(it) }.getOrNull()
    }

    val userFlow: Flow<UserDto?> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_USER] ?: sharedPrefs.getString(PREF_KEY_USER, null)
        val user = raw?.let { runCatching { json.decodeFromString<UserDto>(it) }.getOrNull() }
        _cachedUser = user
        user
    }

    fun getUserSync(): UserDto? = _cachedUser

    suspend fun getUser(): UserDto? {
        if (_cachedUser != null) return _cachedUser
        val raw = context.dataStore.data.first()[KEY_USER] ?: sharedPrefs.getString(PREF_KEY_USER, null)
        val user = raw?.let { runCatching { json.decodeFromString<UserDto>(it) }.getOrNull() }
        _cachedUser = user
        return user
    }

    suspend fun saveUser(user: UserDto) {
        _cachedUser = user
        val encoded = json.encodeToString(user)
        sharedPrefs.edit().putString(PREF_KEY_USER, encoded).apply()
        context.dataStore.edit { it[KEY_USER] = encoded }
    }

    // ─── Session ──────────────────────────────────────────────────────────────

    fun isLoggedInSync(): Boolean = !_cachedToken.isNullOrBlank()

    suspend fun isLoggedIn(): Boolean = isLoggedInSync() || getToken() != null

    suspend fun clearSession() {
        _cachedToken = null
        _cachedUser = null
        sharedPrefs.edit().remove(PREF_KEY_TOKEN).remove(PREF_KEY_USER).apply()
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_TOKEN)
            prefs.remove(KEY_USER)
            // Notice: PREF_KEY_LANGUAGE is intentionally NOT removed so language choice persists across logout!
        }
    }
}
