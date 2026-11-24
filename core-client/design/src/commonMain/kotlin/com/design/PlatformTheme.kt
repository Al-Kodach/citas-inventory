package com.citacomsys.core.theme

import androidx.compose.runtime.Composable

/**
 * Returns true if the platform-specific logic strongly detects a dark theme.
 * Returns null if the platform should fallback to standard Compose detection.
 */
expect fun getSystemDarkThemeOverride(): Boolean?

/**
 * High-fidelity system dark mode detection for Desktop.
 */
@Composable
expect fun isDesktopSystemInDarkTheme(): Boolean

/**
 * True if running on Desktop (JVM) or Web, false on Android/Mobile touch platforms.
 */
expect val isDesktopPlatform: Boolean
