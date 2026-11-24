package com.citacomsys.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.time.Duration.Companion.seconds

/**
 * JVM Desktop detection for Linux/Windows/Mac.
 * Focuses on Linux (GNOME) which often fails standard detection in Compose Desktop.
 */
actual fun getSystemDarkThemeOverride(): Boolean? {
    return try {
        val os = System.getProperty("os.name")?.lowercase().orEmpty()
        if (os.contains("linux")) {
            // 1. Check GNOME Color Scheme (Modern standard)
            val process = Runtime.getRuntime()
                .exec(arrayOf("gsettings", "get", "org.gnome.desktop.interface", "color-scheme"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val result = reader.readText().trim()

            if (result.contains("dark")) {
                return true
            } else if (result.contains("default") || result.contains("light")) {
                if (result == "'default'") {
                    val themeProcess = Runtime.getRuntime().exec(
                        arrayOf(
                            "gsettings",
                            "get",
                            "org.gnome.desktop.interface",
                            "gtk-theme"
                        )
                    )
                    val themeReader = BufferedReader(InputStreamReader(themeProcess.inputStream))
                    val themeName = themeReader.readText().trim().lowercase()
                    if (themeName.contains("dark") || themeName.contains("black")) return true
                }
                return false
            }
        }
        null
    } catch (e: Exception) {
        null
    }
}

@Composable
actual fun isDesktopSystemInDarkTheme(): Boolean {
    val composeDark = isSystemInDarkTheme()

    return produceState(initialValue = getSystemDarkThemeOverride() ?: composeDark) {
        while (isActive) {
            delay(10.seconds)
            value = getSystemDarkThemeOverride() ?: composeDark
        }
    }.value
}

actual val isDesktopPlatform: Boolean = true
