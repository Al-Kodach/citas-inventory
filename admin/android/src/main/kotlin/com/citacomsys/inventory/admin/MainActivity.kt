package com.citacomsys.inventory.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.citacomsys.core.domain.repository.PreferencesRepository
import com.citacomsys.inventory.admin.app.AdminApp
import com.design.AppTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val prefs: PreferencesRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkTheme by prefs.isDarkTheme.collectAsState(initial = null)
            val fontScale by prefs.userFontScale.collectAsState(initial = 1)

            AppTheme(
                isDarkTheme = isDarkTheme,
                fontScale = fontScale
            ) {
                AdminApp(
                    onExit = { finish() }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }
}
