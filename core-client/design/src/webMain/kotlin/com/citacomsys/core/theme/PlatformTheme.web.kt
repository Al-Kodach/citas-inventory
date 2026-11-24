package com.citacomsys.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

/**
 * Web standard uses media queries correctly.
 */
actual fun getSystemDarkThemeOverride(): Boolean? = null

@Composable
actual fun isDesktopSystemInDarkTheme(): Boolean = isSystemInDarkTheme()

actual val isDesktopPlatform: Boolean = true
