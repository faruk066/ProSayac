package com.prosayac.app.di

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

private val Context.sessionDataStore by preferencesDataStore(name = "supabase_session")

class DataStoreSessionManager(private val context: Context) : SessionManager {

    private val SESSION_KEY = stringPreferencesKey("user_session")
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun saveSession(session: UserSession) {
        try {
            val encoded = json.encodeToString(session)
            Log.d("SessionManager", "Saving session, length=${encoded.length}")
            context.sessionDataStore.edit { prefs ->
                prefs[SESSION_KEY] = encoded
            }
            Log.d("SessionManager", "Session saved successfully")
        } catch (e: Exception) {
            Log.e("SessionManager", "Save failed: ${e.message}")
        }
    }

    override suspend fun loadSession(): UserSession? {
        return try {
            val prefs = context.sessionDataStore.data.first()
            val raw = prefs[SESSION_KEY]
            Log.d("SessionManager", "Load raw=${raw?.take(50)}")
            raw?.let { json.decodeFromString(it) }
        } catch (e: Exception) {
            Log.e("SessionManager", "Load failed: ${e.message}", e)
            null
        }
    }

    override suspend fun deleteSession() {
        context.sessionDataStore.edit { prefs ->
            prefs.remove(SESSION_KEY)
        }
    }
}
