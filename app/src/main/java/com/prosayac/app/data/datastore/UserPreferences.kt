package com.prosayac.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

data class AppPreferences(
    val themeMode: String = "system", // "light", "dark", "system"
    val autoSyncEnabled: Boolean = false,
    val offlineMode: Boolean = true,
    val baudRate: Int = 2400,
    val dataBits: Int = 8,
    val stopBits: Int = 1,
    val parity: Int = 0
)

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferenceKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AUTO_SYNC = booleanPreferencesKey("auto_sync")
        val OFFLINE_MODE = booleanPreferencesKey("offline_mode")
        val BAUD_RATE = intPreferencesKey("baud_rate")
        val DATA_BITS = intPreferencesKey("data_bits")
        val STOP_BITS = intPreferencesKey("stop_bits")
        val PARITY = intPreferencesKey("parity")
    }

    val preferencesFlow: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        AppPreferences(
            themeMode = prefs[PreferenceKeys.THEME_MODE] ?: "system",
            autoSyncEnabled = prefs[PreferenceKeys.AUTO_SYNC] ?: false,
            offlineMode = prefs[PreferenceKeys.OFFLINE_MODE] ?: true,
            baudRate = prefs[PreferenceKeys.BAUD_RATE] ?: 2400,
            dataBits = prefs[PreferenceKeys.DATA_BITS] ?: 8,
            stopBits = prefs[PreferenceKeys.STOP_BITS] ?: 1,
            parity = prefs[PreferenceKeys.PARITY] ?: 0
        )
    }

    suspend fun updateThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.THEME_MODE] = mode
        }
    }

    suspend fun updateAutoSync(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.AUTO_SYNC] = enabled
        }
    }

    suspend fun updateOfflineMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.OFFLINE_MODE] = enabled
        }
    }

    suspend fun updateSerialConfig(baudRate: Int, dataBits: Int, stopBits: Int, parity: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferenceKeys.BAUD_RATE] = baudRate
            prefs[PreferenceKeys.DATA_BITS] = dataBits
            prefs[PreferenceKeys.STOP_BITS] = stopBits
            prefs[PreferenceKeys.PARITY] = parity
        }
    }
}