package com.prosayac.app.di

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypts/decrypts session data using Tink (AES-256-GCM) with master key
 * stored in Android Keystore.
 *
 * Replaces the deprecated EncryptedSharedPreferences approach.
 */
@Singleton
class SessionEncryptionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val aead: Aead by lazy {
        val handle = AndroidKeysetManager.Builder()
            .withSharedPref(context, "session_keyset", "session_crypto_prefs")
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri("android-keystore://session_master_key")
            .build()
            .keysetHandle
        handle.getPrimitive(Aead::class.java)
    }

    fun encrypt(plaintext: String): String {
        val encrypted = aead.encrypt(plaintext.toByteArray(Charsets.UTF_8), null)
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    fun decrypt(ciphertext: String): String {
        val decoded = Base64.decode(ciphertext, Base64.NO_WRAP)
        val decrypted = aead.decrypt(decoded, null)
        return String(decrypted, Charsets.UTF_8)
    }
}
