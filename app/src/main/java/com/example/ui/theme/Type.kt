package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val OutfitFontFamily = FontFamily(
    Font(R.font.outfit, FontWeight.Normal),
    Font(R.font.outfit, FontWeight.Medium),
    Font(R.font.outfit, FontWeight.SemiBold),
    Font(R.font.outfit, FontWeight.Bold)
)

val InterFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold)
)

val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium)
)

fun buildVaultTypography(scale: Float = 1.0f): Typography {
    fun Float.scaled() = (this * scale).sp
    return Typography(
        displayLarge = TextStyle(
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 40f.scaled(),
            lineHeight = 46f.scaled(),
            letterSpacing = (-0.5).sp
        ),
        displayMedium = TextStyle(
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32f.scaled(),
            lineHeight = 38f.scaled(),
            letterSpacing = (-0.25).sp
        ),
        headlineLarge = TextStyle(
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28f.scaled(),
            lineHeight = 34f.scaled(),
            letterSpacing = (-0.2).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 23f.scaled(),
            lineHeight = 29f.scaled()
        ),
        headlineSmall = TextStyle(
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 20f.scaled(),
            lineHeight = 26f.scaled()
        ),
        titleLarge = TextStyle(
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18f.scaled(),
            lineHeight = 24f.scaled()
        ),
        titleMedium = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.5f.scaled(),
            lineHeight = 21f.scaled(),
            letterSpacing = 0.1.sp
        ),
        titleSmall = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.5f.scaled(),
            lineHeight = 19f.scaled(),
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.5f.scaled(),
            lineHeight = 22f.scaled()
        ),
        bodyMedium = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14f.scaled(),
            lineHeight = 20f.scaled()
        ),
        bodySmall = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12f.scaled(),
            lineHeight = 16.5f.scaled()
        ),
        labelLarge = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.5f.scaled(),
            lineHeight = 18f.scaled(),
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.5f.scaled(),
            lineHeight = 15f.scaled(),
            letterSpacing = 0.3.sp
        ),
        labelSmall = TextStyle(
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.5f.scaled(),
            lineHeight = 14f.scaled(),
            letterSpacing = 0.4.sp
        )
    )
}
