package com.example.vaultgallery.security

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class AuthorizedBridgeStatus(
    val isAuthorized: Boolean,
    val keystoreProtected: Boolean,
    val lastUpdatedMs: Long,
    val protocolVersion: String = "v1-local-zero-trust"
)

/**
 * Architectural boundary for future separately authenticated companion/dashboard integrations.
 * Strictly opt-in, encrypted at rest with Android Keystore, authenticated, and immediately revocable.
 * Performs zero background collection, zero telemetry, and zero permission bypass.
 */
class AuthorizedDataContract(
    private val secureStore: SecureLocalStore
) {
    val status: Flow<AuthorizedBridgeStatus> = combine(
        secureStore.authorizedBridgeEnabled,
        secureStore.lastConsentUpdatedMs
    ) { enabled, updated ->
        AuthorizedBridgeStatus(
            isAuthorized = enabled,
            keystoreProtected = secureStore.isHardwareKeystoreReady(),
            lastUpdatedMs = updated
        )
    }

    suspend fun revokeAllAuthorizations() {
        secureStore.setAuthorizedBridgeConsent(enabled = false, scopesSummary = "")
    }

    suspend fun setExplicitAuthorization(enabled: Boolean) {
        val summary = if (enabled) "explicit_user_opt_in:metadata_sync_ready" else ""
        secureStore.setAuthorizedBridgeConsent(enabled = enabled, scopesSummary = summary)
    }
}
