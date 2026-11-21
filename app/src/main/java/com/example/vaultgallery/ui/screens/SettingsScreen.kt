package com.example.vaultgallery.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.vaultgallery.data.db.AppSettings
import com.example.vaultgallery.security.AuthorizedBridgeStatus
import com.example.vaultgallery.security.BiometricStatus

@Composable
fun SettingsScreen(
    settings: AppSettings,
    hasPinConfigured: Boolean,
    biometricStatus: BiometricStatus,
    authorizedBridgeStatus: AuthorizedBridgeStatus,
    onSetThemeMode: (String) -> Unit,
    onSetFontScale: (Float) -> Unit,
    onSetGridColumns: (Int) -> Unit,
    onSetSortOrder: (String) -> Unit,
    onSetDefaultSection: (String) -> Unit,
    onSetSlideshowInterval: (Int) -> Unit,
    onSetSlideshowLoop: (Boolean) -> Unit,
    onConfigurePin: (String, (Boolean) -> Unit) -> Unit,
    onDisableAppLock: () -> Unit,
    onSetBiometricEnabled: (Boolean) -> Unit,
    onSetAutoLockTimeout: (Int) -> Unit,
    onLockNow: () -> Unit,
    onClearThumbnailCache: () -> Unit
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. APPEARANCE
        item(key = "section_appearance") {
            SettingsGroupCard(title = "Appearance", icon = Icons.Filled.Palette) {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("LIGHT" to "Light", "DARK" to "Dark", "SYSTEM" to "System").forEach { (key, label) ->
                        FilterChip(
                            selected = settings.themeMode.equals(key, ignoreCase = true),
                            onClick = { onSetThemeMode(key) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("theme_chip_$key")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Font Size",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.9f to "Small", 1.0f to "Normal", 1.15f to "Large").forEach { (scale, label) ->
                        FilterChip(
                            selected = kotlin.math.abs(settings.fontScale - scale) < 0.04f,
                            onClick = { onSetFontScale(scale) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("font_chip_$label")
                        )
                    }
                }
            }
        }

        // 2. GALLERY
        item(key = "section_gallery") {
            SettingsGroupCard(title = "Gallery", icon = Icons.Filled.AutoAwesomeMosaic) {
                Text(
                    text = "Grid Size (${settings.gridColumns} columns)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 3, 4, 5).forEach { cols ->
                        FilterChip(
                            selected = settings.gridColumns == cols,
                            onClick = { onSetGridColumns(cols) },
                            label = { Text("${cols}×") },
                            modifier = Modifier.testTag("grid_chip_$cols")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Sort Order",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("DATE_DESC" to "Newest", "NAME_ASC" to "A–Z", "SIZE_DESC" to "Largest").forEach { (key, label) ->
                        FilterChip(
                            selected = settings.sortOrder == key,
                            onClick = { onSetSortOrder(key) },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Default Start Section",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("HOME" to "Home", "PHOTOS" to "Photos", "ALBUMS" to "Albums").forEach { (key, label) ->
                        FilterChip(
                            selected = settings.defaultSection.equals(key, ignoreCase = true),
                            onClick = { onSetDefaultSection(key) },
                            label = { Text(label) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("VIDEOS" to "Videos", "DOCUMENTS" to "Docs", "FAVORITES" to "Favorites").forEach { (key, label) ->
                        FilterChip(
                            selected = settings.defaultSection.equals(key, ignoreCase = true),
                            onClick = { onSetDefaultSection(key) },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Slideshow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Slideshow Interval: ${settings.slideshowIntervalSec}s",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Slider(
                    value = settings.slideshowIntervalSec.toFloat(),
                    onValueChange = { onSetSlideshowInterval(it.toInt()) },
                    valueRange = 2f..12f,
                    steps = 9
                )
                SettingsToggleRow(
                    title = "Loop Slideshow",
                    subtitle = "Automatically restart from the first photo",
                    checked = settings.slideshowLoop,
                    onCheckedChange = onSetSlideshowLoop
                )
            }
        }

        // 3. SHIELD (App Lock, Biometric, Auto-Lock)
        item(key = "section_shield") {
            SettingsGroupCard(title = "Shield", icon = Icons.Filled.Shield) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_vault_shield_banner_1795258699019),
                        contentDescription = "Vault Shield protection",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Android Keystore AES-256-GCM",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))

                SettingsToggleRow(
                    title = "App Lock",
                    subtitle = if (hasPinConfigured) {
                        "Require Vault PIN or Biometrics to open"
                    } else {
                        "Set a Vault PIN to protect your gallery"
                    },
                    checked = settings.appLockEnabled && hasPinConfigured,
                    onCheckedChange = { wantEnable ->
                        if (wantEnable) {
                            showPinDialog = true
                        } else {
                            onDisableAppLock()
                        }
                    },
                    testTag = "shield_app_lock_switch"
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { showPinDialog = true },
                        modifier = Modifier.testTag("shield_set_pin_button")
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (hasPinConfigured) "Change PIN" else "Set Vault PIN")
                    }
                    if (hasPinConfigured && settings.appLockEnabled) {
                        OutlinedButton(
                            onClick = onLockNow,
                            modifier = Modifier.testTag("shield_lock_now_button")
                        ) {
                            Text("Lock Now")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                val bioSubtitle = when (biometricStatus) {
                    BiometricStatus.AVAILABLE -> "Use fingerprint or face unlock via BiometricPrompt"
                    BiometricStatus.NONE_ENROLLED -> "Enroll biometrics in Android Settings to use"
                    BiometricStatus.UNAVAILABLE -> "Biometric hardware unavailable on this device"
                }
                SettingsToggleRow(
                    title = "Biometric Unlock",
                    subtitle = bioSubtitle,
                    checked = settings.biometricEnabled && biometricStatus == BiometricStatus.AVAILABLE,
                    enabled = biometricStatus == BiometricStatus.AVAILABLE,
                    onCheckedChange = onSetBiometricEnabled,
                    leadingIcon = Icons.Filled.Fingerprint,
                    testTag = "shield_biometric_switch"
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Auto-Lock Timeout",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val timeoutOptions = listOf(0 to "Now", 30 to "30s", 60 to "1m", 300 to "5m", 900 to "15m")
                    items(timeoutOptions, key = { it.first }) { (sec, label) ->
                        FilterChip(
                            selected = settings.autoLockTimeoutSec == sec,
                            onClick = { onSetAutoLockTimeout(sec) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        // 4. VAULT (Local Security & Storage Management)
        item(key = "section_vault") {
            SettingsGroupCard(title = "Vault", icon = Icons.Filled.VerifiedUser) {
                Text(
                    text = "Local Security & Cache",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Keystore Encryption: ${if (authorizedBridgeStatus.keystoreProtected) "Hardware-Backed AES-256-GCM" else "PBKDF2 Local Protection"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "All favorites, recent items, PDF bookmarks, and lock credentials stay strictly on this device. Reserved for future authorized encrypted backup features.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onClearThumbnailCache,
                    modifier = Modifier.testTag("vault_clear_cache_button")
                ) {
                    Text("Clear Thumbnail Cache")
                }
            }
        }

        // 5. PROFILE (Local Private Mode)
        item(key = "section_profile") {
            SettingsGroupCard(title = "Profile", icon = Icons.Filled.AccountCircle) {
                Text(
                    text = "Local Private Device Mode",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Vault Gallery operates offline on this device with no sign-in required. No media, analytics, or telemetry leave your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 6. ABOUT
        item(key = "section_about") {
            SettingsGroupCard(title = "About", icon = Icons.Filled.Info) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vault Gallery Version", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "1.0.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AboutActionRow(
                    title = "Privacy",
                    icon = Icons.Filled.Policy,
                    onClick = { showInfoDialog = "PRIVACY" }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AboutActionRow(
                    title = "Help & Tips",
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    onClick = { showInfoDialog = "HELP" }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AboutActionRow(
                    title = "Open-Source Licenses",
                    icon = Icons.Filled.Check,
                    onClick = { showInfoDialog = "LICENSES" }
                )
            }
        }
    }

    if (showPinDialog) {
        SetVaultPinDialog(
            isChanging = hasPinConfigured,
            onDismiss = { showPinDialog = false },
            onSavePin = { pin ->
                onConfigurePin(pin) { ok ->
                    if (ok) showPinDialog = false
                }
            }
        )
    }

    showInfoDialog?.let { topic ->
        val (title, body) = when (topic) {
            "PRIVACY" -> "Privacy Commitment" to
                "Vault Gallery stores your media index, favorites, bookmarks, and preferences locally on your device. Passcodes are PBKDF2-hashed and encrypted using Android Keystore AES-256-GCM. No background tracking, telemetry, or unauthorized data sharing ever occurs."
            "HELP" -> "Help & Tips" to
                "• Use Pick Media or Open Document anytime for zero-permission Photo Picker and Storage Access Framework access.\n• Pinch to zoom or double-tap photos in the full-screen viewer.\n• Tap any PDF page to toggle full-screen reading, use thumbnails for fast jumping, or bookmark pages for later."
            else -> "Open-Source Licenses" to
                "Vault Gallery is built with open-source AndroidX & Kotlin libraries:\n\n• Jetpack Compose & Material 3 (Apache 2.0)\n• AndroidX Room, DataStore, Navigation, Biometric, DocumentFile (Apache 2.0)\n• AndroidX Media3 ExoPlayer (Apache 2.0)\n• Coil & Coil Video (Apache 2.0)\n• Kotlin Coroutines (Apache 2.0)\n• Outfit, Inter, JetBrains Mono Fonts (SIL OFL 1.1)"
        }
        AlertDialog(
            onDismissRequest = { showInfoDialog = null },
            title = { Text(title) },
            text = { Text(body, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    testTag: String = ""
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier
        )
    }
}

@Composable
private fun AboutActionRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SetVaultPinDialog(
    isChanging: Boolean,
    onDismiss: () -> Unit,
    onSavePin: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isChanging) "Change Vault PIN" else "Create Vault PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Choose a 4 to 8 digit PIN. It is salted, PBKDF2-hashed, and encrypted with Android Keystore.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pin = it },
                    label = { Text("Enter PIN (4–8 digits)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_setup_input")
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) confirmPin = it },
                    label = { Text("Confirm PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_confirm_input")
                )
                errorText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        pin.length < 4 -> errorText = "PIN must be at least 4 digits."
                        pin != confirmPin -> errorText = "PINs do not match."
                        else -> onSavePin(pin)
                    }
                },
                modifier = Modifier.testTag("pin_save_button")
            ) {
                Text("Save PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
