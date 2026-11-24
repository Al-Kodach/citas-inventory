package com.citacomsys.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

/**
 * Android correctly supports isSystemInDarkTheme() via Configuration changes.
 */
actual fun getSystemDarkThemeOverride(): Boolean? = null

@Composable
actual fun isDesktopSystemInDarkTheme(): Boolean = isSystemInDarkTheme()

actual val isDesktopPlatform: Boolean = false
