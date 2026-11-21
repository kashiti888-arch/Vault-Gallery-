package com.example.vaultgallery.security

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

enum class BiometricStatus {
    AVAILABLE,
    NONE_ENROLLED,
    UNAVAILABLE
}

class AppLockManager(
    private val secureStore: SecureLocalStore
) {
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _isLockStateResolved = MutableStateFlow(false)
    val isLockStateResolved: StateFlow<Boolean> = _isLockStateResolved.asStateFlow()

    private val _failedAttempts = MutableStateFlow(0)
    val failedAttempts: StateFlow<Int> = _failedAttempts.asStateFlow()

    private val _lockoutUntilMs = MutableStateFlow(0L)
    val lockoutUntilMs: StateFlow<Long> = _lockoutUntilMs.asStateFlow()

    private val _keystoreRecoveryNeeded = MutableStateFlow(false)
    val keystoreRecoveryNeeded: StateFlow<Boolean> = _keystoreRecoveryNeeded.asStateFlow()

    val hasPinConfigured: Flow<Boolean> = secureStore.hasPinConfigured

    suspend fun evaluateInitialLock(appLockEnabled: Boolean) {
        val hasPin = secureStore.hasPinConfigured.first()
        _isLocked.value = appLockEnabled && hasPin
        _isLockStateResolved.value = true
    }

    suspend fun onAppBackgrounded() {
        secureStore.recordBackgroundTimestamp(System.currentTimeMillis())
    }

    suspend fun onAppForegrounded(appLockEnabled: Boolean, autoLockTimeoutSec: Int) {
        if (!appLockEnabled) {
            _isLocked.value = false
            return
        }
        val hasPin = secureStore.hasPinConfigured.first()
        if (!hasPin) {
            _isLocked.value = false
            return
        }
        val lastBg = secureStore.getBackgroundTimestamp()
        if (lastBg == 0L) return
        val elapsedSec = (System.currentTimeMillis() - lastBg) / 1000L
        if (autoLockTimeoutSec <= 0 || elapsedSec >= autoLockTimeoutSec) {
            _isLocked.value = true
        }
    }

    fun lockNow() {
        _isLocked.value = true
    }

    fun unlockDirectly() {
        _failedAttempts.value = 0
        _lockoutUntilMs.value = 0L
        _keystoreRecoveryNeeded.value = false
        _isLocked.value = false
    }

    suspend fun verifyAndUnlockWithPin(pin: String): PinVerificationResult {
        val now = System.currentTimeMillis()
        if (now < _lockoutUntilMs.value) {
            return PinVerificationResult.INVALID
        }
        val result = secureStore.verifyPinDetailed(pin)
        when (result) {
            PinVerificationResult.VALID -> {
                unlockDirectly()
                secureStore.recordBackgroundTimestamp(0L)
            }
            PinVerificationResult.STORAGE_ERROR -> {
                // Never lock the user out permanently if OS Keystore key was invalidated
                _keystoreRecoveryNeeded.value = true
            }
            PinVerificationResult.INVALID -> {
                val attempts = _failedAttempts.value + 1
                _failedAttempts.value = attempts
                if (attempts >= 5) {
                    _lockoutUntilMs.value = now + 30_000L
                    _failedAttempts.value = 0
                }
            }
        }
        return result
    }

    suspend fun setPin(pin: String): Boolean {
        val saved = secureStore.savePin(pin)
        if (saved) _keystoreRecoveryNeeded.value = false
        return saved
    }

    suspend fun clearPinAndUnlock() {
        secureStore.clearPin()
        secureStore.recordBackgroundTimestamp(0L)
        unlockDirectly()
    }

    fun checkBiometricStatus(context: Context): BiometricStatus {
        val manager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        return when (manager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NONE_ENROLLED
            else -> BiometricStatus.UNAVAILABLE
        }
    }

    fun isDeviceCredentialAvailable(context: Context): Boolean {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return km?.isDeviceSecure == true
    }

    fun promptBiometricUnlock(
        activity: FragmentActivity,
        title: String = "Unlock Vault Gallery",
        subtitle: String = "Verify your identity to continue",
        negativeButtonText: String = "Use Vault PIN",
        onAuthenticated: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    unlockDirectly()
                    onAuthenticated()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_CANCELED
                    ) {
                        onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    onError("Biometric not recognized. Try again or enter your Vault PIN.")
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .build()

        runCatching { prompt.authenticate(info) }
            .onFailure { onError("Biometric prompt unavailable. Please unlock with your Vault PIN.") }
    }

    /**
     * OS-level Device Credential fallback (Device PIN / Pattern / Password / Biometric)
     * so users can safely unlock or reset Vault Lock and are never permanently locked out.
     */
    fun promptDeviceCredentialRecovery(
        activity: FragmentActivity,
        onRecovered: () -> Unit,
        onUnavailableOrError: (String) -> Unit
    ) {
        if (!isDeviceCredentialAvailable(activity)) {
            onUnavailableOrError("No Android device lock screen is configured.")
            return
        }
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    unlockDirectly()
                    onRecovered()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_CANCELED
                    ) {
                        onUnavailableOrError(errString.toString())
                    }
                }
            }
        )

        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verify Device Identity")
            .setSubtitle("Use your Android lock screen credential to unlock Vault Gallery")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setDeviceCredentialAllowed(true)
        }

        runCatching { prompt.authenticate(builder.build()) }
            .onFailure { onUnavailableOrError("Device credential verification is unavailable.") }
    }
}
