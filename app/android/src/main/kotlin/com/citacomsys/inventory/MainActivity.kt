package com.citacomsys.inventory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.app.shared.app.AppShell
import com.citacomsys.core.domain.repository.PreferencesRepository
import com.citacomsys.device.service.ActivityProvider
import com.design.AppTheme
import com.design.ThemeManager
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val prefs: PreferencesRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityProvider.currentActivity = this
        enableEdgeToEdge()

        setContent {
            val isDarkTheme by prefs.isDarkTheme.collectAsStateWithLifecycle()
            val fontScale by prefs.userFontScale.collectAsStateWithLifecycle()
            val branding by prefs.activeBranding.collectAsStateWithLifecycle()

            val brandColors = remember(branding, isDarkTheme) {
                ThemeManager.fromBranding(branding, isDark = isDarkTheme == true)
            }

            AppTheme(
                isDarkTheme = isDarkTheme,
                fontScale = fontScale,
                colorScheme = brandColors
            ) {
                AppShell(
                    onExit = { finish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ActivityProvider.currentActivity = this
    }
}
