package com.prompter.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the BYOK API key encrypted with an Android Keystore AES key.
 * The key is never stored in plain text, logs, history, or the APK.
 */
class AiSettings(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var model: String
        get() = prefs.getString(KEY_MODEL, AiDefaults.MODEL)?.takeIf { it.isNotBlank() }
            ?: AiDefaults.MODEL
        set(value) {
            prefs.edit().putString(KEY_MODEL, value.trim().ifBlank { AiDefaults.MODEL }).apply()
        }

    var baseUrl: String
        get() = AiDefaults.normalizeBaseUrl(
            prefs.getString(KEY_BASE_URL, AiDefaults.BASE_URL).orEmpty(),
        )
        set(value) {
            prefs.edit().putString(KEY_BASE_URL, AiDefaults.normalizeBaseUrl(value)).apply()
        }

    fun hasApiKey(): Boolean = !getApiKey().isNullOrBlank()

    fun getApiKey(): String? {
        val encrypted = prefs.getString(KEY_API_KEY, null) ?: return null
        return try {
            decrypt(encrypted)
        } catch (_: Exception) {
            // A key can become unreadable after a backup restore or keystore
            // invalidation. Treat it as absent rather than crashing the app.
            prefs.edit().remove(KEY_API_KEY).apply()
            null
        }
    }

    fun setApiKey(value: String) {
        val trimmed = value.trim()
        require(trimmed.isNotEmpty()) { "API key must not be empty" }
        prefs.edit().putString(KEY_API_KEY, encrypt(trimmed)).apply()
    }

    fun clearApiKey() {
        prefs.edit().remove(KEY_API_KEY).apply()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        require(combined.size > IV_BYTES) { "Invalid encrypted API key" }
        val iv = combined.copyOfRange(0, IV_BYTES)
        val ciphertext = combined.copyOfRange(IV_BYTES, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(128, iv),
        )
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getEntry(KEY_ALIAS, null)
        if (existing is KeyStore.SecretKeyEntry) return existing.secretKey
        if (existing != null) keyStore.deleteEntry(KEY_ALIAS)

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE,
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        private const val PREFS_NAME = "prompter_ai_settings"
        private const val KEY_API_KEY = "openrouter_api_key"
        private const val KEY_MODEL = "model"
        private const val KEY_BASE_URL = "base_url"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "prompter_openrouter_key_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_BYTES = 12
    }
}
