package com.skai.lofintrackerapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferences(private val context: Context) {

    companion object {
        val USER_NAME = stringPreferencesKey("user_name")
        val APP_THEME = stringPreferencesKey("app_theme")
        val HAS_SEEN_TUTORIAL = booleanPreferencesKey("has_seen_tutorial")
        val CURRENCY = stringPreferencesKey("currency")
        val APP_LOCK_KEY = booleanPreferencesKey("app_lock_enabled")
        val REMINDER_DAYS = intPreferencesKey("reminder_days")
        val APP_INSTALL_TIMESTAMP = longPreferencesKey("app_install_timestamp") // New preference key
    }

    // Default to empty string instead of null to allow the UI to progress on first run
    val userName: Flow<String> = context.dataStore.data.map { it[USER_NAME] ?: "" }
    val appTheme: Flow<String> = context.dataStore.data.map { it[APP_THEME] ?: "system" }
    val hasSeenTutorial: Flow<Boolean> = context.dataStore.data.map { it[HAS_SEEN_TUTORIAL] ?: false }
    val currency: Flow<String> = context.dataStore.data.map { it[CURRENCY] ?: "INR" }
    val isAppLockEnabled: Flow<Boolean> = context.dataStore.data.map { it[APP_LOCK_KEY] ?: false }
    val reminderDays: Flow<Int> = context.dataStore.data.map { it[REMINDER_DAYS] ?: 1 }
    val appInstallTimestamp: Flow<Long?> = context.dataStore.data.map { it[APP_INSTALL_TIMESTAMP] } // New flow for timestamp

    suspend fun saveUserName(name: String) { context.dataStore.edit { it[USER_NAME] = name.trim() } }
    suspend fun saveAppTheme(theme: String) { context.dataStore.edit { it[APP_THEME] = theme } }
    suspend fun saveHasSeenTutorial(hasSeen: Boolean) { context.dataStore.edit { it[HAS_SEEN_TUTORIAL] = hasSeen } }
    suspend fun saveCurrency(currency: String) { context.dataStore.edit { it[CURRENCY] = currency } }
    suspend fun saveAppLockEnabled(enabled: Boolean) { context.dataStore.edit { it[APP_LOCK_KEY] = enabled } }
    suspend fun saveReminderDays(days: Int) { context.dataStore.edit { it[REMINDER_DAYS] = days } }
    suspend fun saveAppInstallTimestamp(timestamp: Long) { context.dataStore.edit { it[APP_INSTALL_TIMESTAMP] = timestamp } } // New function to save timestamp
}
