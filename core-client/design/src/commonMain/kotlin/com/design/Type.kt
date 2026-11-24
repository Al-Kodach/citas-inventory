package com.design

import androidx.compose.material3.Typography
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun AppFontFamily(): FontFamily {
    return FontFamily.SansSerif
}

@Composable
fun AppTypography(fontScale: Int = 1): Typography {
    val windowWidthDp = currentWindowAdaptiveInfoV2().windowSizeClass.minWidthDp
    val deviceScale = when {
        windowWidthDp < 600 -> 1.0f
        windowWidthDp < 840 -> 1.15f
        windowWidthDp < 1200 -> 1.25f
        else -> 1.35f
    }

    val userMultiplier = when {
        fontScale <= 1 -> 1.0f
        fontScale == 2 -> 1.15f
        fontScale == 3 -> 1.30f
        else -> 1.0f + ((fontScale - 1) * 0.15f)
    }

    val scale = deviceScale * userMultiplier

    return Typography(
        headlineLarge = TextStyle(
            fontFamily = AppFontFamily(),
            fontWeight = FontWeight.Bold,
            fontSize = (32 * scale).sp,
            lineHeight = (40 * scale).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = AppFontFamily(),
            fontWeight = FontWeight.Bold,
            fontSize = (28 * scale).sp,
            lineHeight = (36 * scale).sp
        ),
        titleLarge = TextStyle(
            fontFamily = AppFontFamily(),
            fontWeight = FontWeight.Bold,
            fontSize = (22 * scale).sp,
            lineHeight = (28 * scale).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = AppFontFamily(),
            fontWeight = FontWeight.Normal,
            fontSize = (16 * scale).sp,
            lineHeight = (24 * scale).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = AppFontFamily(),
            fontWeight = FontWeight.Normal,
            fontSize = (14 * scale).sp,
            lineHeight = (20 * scale).sp
        ),
        labelMedium = TextStyle(
            fontFamily = AppFontFamily(),
            fontWeight = FontWeight.Normal,
            fontSize = (12 * scale).sp,
            lineHeight = (16 * scale).sp
        )
    )
}
