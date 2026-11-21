package com.example.vaultgallery.ui.security

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.vaultgallery.security.PinVerificationResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AppLockOverlay(
    biometricAvailable: Boolean,
    deviceCredentialAvailable: Boolean,
    keystoreRecoveryNeeded: Boolean,
    failedAttempts: Int,
    lockoutUntilMs: Long,
    onVerifyPin: suspend (String) -> PinVerificationResult,
    onRequestBiometric: () -> Unit,
    onRequestDeviceCredentialRecovery: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var enteredPin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Block back navigation to underlying gallery routes while locked
    BackHandler(enabled = true) { }

    val isLockedOut = lockoutUntilMs > nowMs
    val remainingSec = ((lockoutUntilMs - nowMs) / 1000L).coerceAtLeast(0L)

    LaunchedEffect(lockoutUntilMs) {
        while (System.currentTimeMillis() < lockoutUntilMs) {
            nowMs = System.currentTimeMillis()
            delay(500L)
        }
        nowMs = System.currentTimeMillis()
    }

    LaunchedEffect(biometricAvailable) {
        if (biometricAvailable) {
            onRequestBiometric()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("app_lock_overlay")
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_vault_shield_banner_1795258699019),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .align(Alignment.TopCenter)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.35f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 20.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = "Vault Locked",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Vault Gallery Locked",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = when {
                        keystoreRecoveryNeeded -> "Android security credentials changed. Verify your device lock to recover."
                        isLockedOut -> "Too many attempts. Try again in ${remainingSec}s or verify with Device Lock."
                        else -> "Enter your Vault PIN or unlock with Biometrics"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isLockedOut || keystoreRecoveryNeeded) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // PIN Dots Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val slots = maxOf(4, enteredPin.length).coerceAtMost(8)
                    repeat(slots) { idx ->
                        val filled = idx < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    if (filled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                errorText?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Keypad (scroll-safe for landscape screens too!)
            Column(
                modifier = Modifier.widthIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { digit ->
                            KeypadDigitButton(
                                label = digit,
                                enabled = !isLockedOut,
                                onClick = {
                                    if (enteredPin.length < 8) {
                                        enteredPin += digit
                                        errorText = null
                                    }
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !isLockedOut && enteredPin.isNotEmpty()) {
                                enteredPin = enteredPin.dropLast(1)
                            }
                            .testTag("lock_backspace")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Delete digit",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    KeypadDigitButton(
                        label = "0",
                        enabled = !isLockedOut,
                        onClick = {
                            if (enteredPin.length < 8) {
                                enteredPin += "0"
                                errorText = null
                            }
                        }
                    )

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !isLockedOut && enteredPin.length >= 4) {
                                val attempt = enteredPin
                                scope.launch {
                                    when (onVerifyPin(attempt)) {
                                        PinVerificationResult.VALID -> {
                                            enteredPin = ""
                                            errorText = null
                                        }
                                        PinVerificationResult.INVALID -> {
                                            enteredPin = ""
                                            errorText = "Incorrect PIN (attempt ${failedAttempts + 1} of 5)"
                                        }
                                        PinVerificationResult.STORAGE_ERROR -> {
                                            enteredPin = ""
                                            errorText = "Security storage changed. Use Device Lock recovery below."
                                        }
                                    }
                                }
                            }
                            .testTag("lock_submit_pin")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Unlock with PIN",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (biometricAvailable) {
                    Button(
                        onClick = onRequestBiometric,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("lock_biometric_button")
                    ) {
                        Icon(Icons.Filled.Fingerprint, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Unlock with Biometrics")
                    }
                }

                if (deviceCredentialAvailable || keystoreRecoveryNeeded) {
                    TextButton(
                        onClick = onRequestDeviceCredentialRecovery,
                        modifier = Modifier.testTag("lock_device_recovery_button")
                    ) {
                        Icon(
                            Icons.Filled.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            if (keystoreRecoveryNeeded) "Verify Device Lock & Reset Vault Lock"
                            else "Unlock with Android Device Credential"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadDigitButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag("lock_key_$label")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
