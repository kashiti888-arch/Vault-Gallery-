package com.example.vaultgallery.security

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private val Context.vaultSecureDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "vault_gallery_secure_store"
)

enum class PinVerificationResult {
    VALID,
    INVALID,
    STORAGE_ERROR
}

/**
 * Encrypted local key-value store backed by DataStore and Android Keystore.
 * Passcodes are salted + PBKDF2-HMAC-SHA256 stretched and encrypted at rest.
 * Never logs or persists plaintext PINs, passwords, or tokens.
 */
class SecureLocalStore(
    private val context: Context,
    private val cryptoManager: KeystoreCryptoManager
) {
    private val pinVerifierKey = stringPreferencesKey("enc_pin_verifier")
    private val pinPlainVerifierFallbackKey = stringPreferencesKey("hashed_pin_verifier_v1")
    private val lastBackgroundTimestampKey = longPreferencesKey("last_background_ms")
    private val authorizedBridgeConsentKey = booleanPreferencesKey("authorized_bridge_consent")
    private val authorizedScopeEncKey = stringPreferencesKey("enc_authorized_scopes")
    private val lastConsentChangedKey = longPreferencesKey("authorized_consent_updated_ms")
    private val lastThemeQuickKey = stringPreferencesKey("quick_theme_mode")
    private val mediaPermissionRequestedKey = booleanPreferencesKey("media_perm_requested_once")

    val hasPinConfigured: Flow<Boolean> = context.vaultSecureDataStore.data.map { prefs ->
        !prefs[pinVerifierKey].isNullOrBlank() || !prefs[pinPlainVerifierFallbackKey].isNullOrBlank()
    }

    val quickThemeMode: Flow<String> = context.vaultSecureDataStore.data.map { prefs ->
        prefs[lastThemeQuickKey] ?: "DARK"
    }

    val authorizedBridgeEnabled: Flow<Boolean> = context.vaultSecureDataStore.data.map { prefs ->
        prefs[authorizedBridgeConsentKey] ?: false
    }

    val lastConsentUpdatedMs: Flow<Long> = context.vaultSecureDataStore.data.map { prefs ->
        prefs[lastConsentChangedKey] ?: 0L
    }

    val mediaPermissionRequestedOnce: Flow<Boolean> = context.vaultSecureDataStore.data.map { prefs ->
        prefs[mediaPermissionRequestedKey] ?: false
    }

    suspend fun markMediaPermissionRequested() {
        context.vaultSecureDataStore.edit { it[mediaPermissionRequestedKey] = true }
    }

    suspend fun saveQuickThemeMode(mode: String) {
        context.vaultSecureDataStore.edit { it[lastThemeQuickKey] = mode }
    }

    suspend fun savePin(pin: String): Boolean {
        if (pin.length < 4 || pin.length > 8 || !pin.all { it.isDigit() }) return false
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = derivePinHash(pin, salt)
        val serialized = "${Base64.encodeToString(salt, Base64.NO_WRAP)}:${Base64.encodeToString(hash, Base64.NO_WRAP)}"

        val encrypted = runCatching { cryptoManager.encrypt(serialized) }.getOrNull()
        context.vaultSecureDataStore.edit { prefs ->
            if (encrypted != null) {
                prefs[pinVerifierKey] = encrypted
                prefs.remove(pinPlainVerifierFallbackKey)
            } else {
                // Fallback when AndroidKeyStore is unavailable (e.g. JVM test or broken vendor keystore):
                // Still stores ONLY salted PBKDF2-HMAC-SHA256 digest, never plaintext PIN.
                prefs[pinPlainVerifierFallbackKey] = serialized
                prefs.remove(pinVerifierKey)
            }
        }
        return true
    }

    suspend fun verifyPinDetailed(pin: String): PinVerificationResult {
        val prefs = context.vaultSecureDataStore.data.first()
        val encrypted = prefs[pinVerifierKey]
        val serialized: String = if (!encrypted.isNullOrBlank()) {
            cryptoManager.decrypt(encrypted) ?: return PinVerificationResult.STORAGE_ERROR
        } else {
            prefs[pinPlainVerifierFallbackKey] ?: return PinVerificationResult.STORAGE_ERROR
        }

        val parts = serialized.split(":")
        if (parts.size != 2) return PinVerificationResult.STORAGE_ERROR
        val salt = runCatching { Base64.decode(parts[0], Base64.NO_WRAP) }.getOrNull()
            ?: return PinVerificationResult.STORAGE_ERROR
        val expectedHash = runCatching { Base64.decode(parts[1], Base64.NO_WRAP) }.getOrNull()
            ?: return PinVerificationResult.STORAGE_ERROR

        val candidateHash = derivePinHash(pin, salt)
        return if (MessageDigest.isEqual(expectedHash, candidateHash)) {
            PinVerificationResult.VALID
        } else {
            PinVerificationResult.INVALID
        }
    }

    suspend fun verifyPin(pin: String): Boolean =
        verifyPinDetailed(pin) == PinVerificationResult.VALID

    suspend fun clearPin() {
        context.vaultSecureDataStore.edit { prefs ->
            prefs.remove(pinVerifierKey)
            prefs.remove(pinPlainVerifierFallbackKey)
        }
    }

    suspend fun recordBackgroundTimestamp(timestampMs: Long) {
        context.vaultSecureDataStore.edit { prefs ->
            prefs[lastBackgroundTimestampKey] = timestampMs
        }
    }

    suspend fun getBackgroundTimestamp(): Long {
        return context.vaultSecureDataStore.data.first()[lastBackgroundTimestampKey] ?: 0L
    }

    suspend fun setAuthorizedBridgeConsent(enabled: Boolean, scopesSummary: String) {
        val encryptedScopes = if (enabled) {
            runCatching { cryptoManager.encrypt(scopesSummary) }.getOrDefault("")
        } else ""
        context.vaultSecureDataStore.edit { prefs ->
            prefs[authorizedBridgeConsentKey] = enabled
            if (enabled && encryptedScopes.isNotBlank()) {
                prefs[authorizedScopeEncKey] = encryptedScopes
            } else {
                prefs.remove(authorizedScopeEncKey)
            }
            prefs[lastConsentChangedKey] = System.currentTimeMillis()
        }
    }

    fun isHardwareKeystoreReady(): Boolean = cryptoManager.isKeystoreAvailable()

    private fun derivePinHash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, HASH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    companion object {
        private const val PBKDF2_ITERATIONS = 120_000
        private const val HASH_BITS = 256
    }
}
