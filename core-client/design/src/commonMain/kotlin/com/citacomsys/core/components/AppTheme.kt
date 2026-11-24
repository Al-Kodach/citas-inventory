package com.citacomsys.core.components

import androidx.compose.material3.ColorScheme

data class AppTheme(
    val colors: ColorScheme,
    val isDark: Boolean
)

enum class ThemeMode {
    LIGHT, DARK, DEVICE
}