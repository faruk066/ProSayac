package com.prosayac.app.di

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.prosayac.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore by preferencesDataStore(name = "supabase_session")

@Singleton
class DataStoreSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionManager: SessionEncryptionManager
) : SessionManager {

    private val SESSION_KEY = stringPreferencesKey("user_session")
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun saveSession(session: UserSession) {
        try {
            val encoded = json.encodeToString(session)
            val encrypted = encryptionManager.encrypt(encoded)
            context.sessionDataStore.edit { prefs ->
                prefs[SESSION_KEY] = encrypted
            }
            if (BuildConfig.DEBUG) {
                Log.d("SessionManager", "Session saved (encrypted, size=${encrypted.length})")
            }
        } catch (e: Exception) {
            Log.e("SessionManager", "Save failed: ${e.message}")
        }
    }

    override suspend fun loadSession(): UserSession? {
        return try {
            val prefs = context.sessionDataStore.data.first()
            val encrypted = prefs[SESSION_KEY] ?: return null
            val decrypted = encryptionManager.decrypt(encrypted)
            json.decodeFromString(decrypted)
        } catch (e: Exception) {
            Log.e("SessionManager", "Load failed: ${e.message}", e)
            null
        }
    }

    override suspend fun deleteSession() {
        context.sessionDataStore.edit { prefs ->
            prefs.remove(SESSION_KEY)
        }
        if (BuildConfig.DEBUG) {
            Log.d("SessionManager", "Session deleted")
        }
    }
}
