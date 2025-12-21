// In ...data/UserPreferences.kt
package com.skai.lofintrackerapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferences(private val context: Context) {

    companion object {
        val USER_NAME = stringPreferencesKey("user_name")
        val APP_THEME = stringPreferencesKey("app_theme")
        val HAS_SEEN_TUTORIAL = booleanPreferencesKey("has_seen_tutorial")
        val CURRENCY = stringPreferencesKey("currency") // <-- ADDED
        val APP_LOCK_KEY = booleanPreferencesKey("app_lock_enabled")
    }

    val userName: Flow<String?> = context.dataStore.data.map { it[USER_NAME] }
    val appTheme: Flow<String> = context.dataStore.data.map { it[APP_THEME] ?: "system" }
    val hasSeenTutorial: Flow<Boolean> = context.dataStore.data.map { it[HAS_SEEN_TUTORIAL] ?: false }

    // Default to INR if not set
    val currency: Flow<String> = context.dataStore.data.map { it[CURRENCY] ?: "INR" }

    val isAppLockEnabled: Flow<Boolean> = context.dataStore.data.map { it[APP_LOCK_KEY] ?: false }

    suspend fun saveUserName(name: String) { context.dataStore.edit { it[USER_NAME] = name } }
    suspend fun saveAppTheme(theme: String) { context.dataStore.edit { it[APP_THEME] = theme } }
    suspend fun saveHasSeenTutorial(hasSeen: Boolean) { context.dataStore.edit { it[HAS_SEEN_TUTORIAL] = hasSeen } }

    // --- ADDED ---
    suspend fun saveCurrency(currency: String) { context.dataStore.edit { it[CURRENCY] = currency } }

    suspend fun saveAppLockEnabled(enabled: Boolean) { context.dataStore.edit { it[APP_LOCK_KEY] = enabled } }
}