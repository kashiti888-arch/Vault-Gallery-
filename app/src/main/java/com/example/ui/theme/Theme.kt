package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val VaultDarkColorScheme = darkColorScheme(
    primary = ChampagneGold,
    onPrimary = OnChampagneGold,
    primaryContainer = ChampagneGoldMuted,
    onPrimaryContainer = ChampagneGoldLight,
    secondary = PlatinumBlue,
    onSecondary = OnPlatinumBlue,
    secondaryContainer = PlatinumBlueContainer,
    onSecondaryContainer = ObsidianOnSurface,
    tertiary = EmeraldAccent,
    onTertiary = OnChampagneGold,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = ObsidianOnSurface,
    background = ObsidianBg,
    onBackground = ObsidianOnSurface,
    surface = ObsidianSurface,
    onSurface = ObsidianOnSurface,
    surfaceVariant = ObsidianSurfaceContainer,
    onSurfaceVariant = ObsidianOnSurfaceVariant,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = ObsidianSurfaceContainerLow,
    surfaceContainer = ObsidianSurfaceContainer,
    surfaceContainerHigh = ObsidianSurfaceContainerHigh,
    surfaceContainerHighest = ObsidianSurfaceContainerHighest,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutlineVariant,
    error = RoseErrorDark,
    onError = OnChampagneGold,
    errorContainer = RoseErrorContainerDark,
    onErrorContainer = ObsidianOnSurface
)

private val VaultLightColorScheme = lightColorScheme(
    primary = BronzeGold,
    onPrimary = OnBronzeGold,
    primaryContainer = BronzeGoldContainer,
    onPrimaryContainer = OnBronzeGoldContainer,
    secondary = SlateBlue,
    onSecondary = OnBronzeGold,
    secondaryContainer = SlateBlueContainer,
    onSecondaryContainer = AlabasterOnSurface,
    tertiary = EmeraldContainer,
    onTertiary = OnBronzeGold,
    background = AlabasterBg,
    onBackground = AlabasterOnSurface,
    surface = AlabasterSurface,
    onSurface = AlabasterOnSurface,
    surfaceVariant = AlabasterSurfaceContainer,
    onSurfaceVariant = AlabasterOnSurfaceVariant,
    surfaceContainerLowest = AlabasterSurface,
    surfaceContainerLow = AlabasterSurfaceContainerLow,
    surfaceContainer = AlabasterSurfaceContainer,
    surfaceContainerHigh = AlabasterSurfaceContainerHigh,
    outline = AlabasterOutline,
    outlineVariant = AlabasterOutlineVariant,
    error = RoseErrorLight,
    onError = OnBronzeGold,
    errorContainer = RoseErrorContainerLight,
    onErrorContainer = AlabasterOnSurface
)

val VaultShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun VaultGalleryTheme(
    themeMode: String = "SYSTEM",
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode.uppercase()) {
        "DARK" -> true
        "LIGHT" -> false
        else -> systemDark
    }

    val colorScheme = if (useDark) VaultDarkColorScheme else VaultLightColorScheme
    val typography = remember(fontScale) { buildVaultTypography(fontScale) }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !useDark
            controller.isAppearanceLightNavigationBars = !useDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = VaultShapes,
        content = content
    )
}
