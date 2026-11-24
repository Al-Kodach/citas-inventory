package com.citacomsys.dev

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.citacomsys.core.domain.repository.PreferencesRepository
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.inventory.bootstrapApp
import com.citacomsys.kernel.service.PlatformClientService
import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.citacomsys.shared.AppConfig
import com.design.AppTheme
import com.design.ThemeManager
import io.github.vinceglb.filekit.FileKit
import org.koin.dsl.module
import java.io.File

fun main() = application {
    println("---------------- Citas DEV App Starting ----------------")
    val appDir = File(System.getProperty("user.home"), ".local/shared/.citacomsys-dev")

    FileKit.init(
        appId = "citas-dev",
        filesDir = File(appDir, "data"),
        cacheDir = File(appDir, "cache")
    )

    val koin = bootstrapApp()

    val prefs = koin.get<PreferencesRepository>()
    val platformService = koin.get<PlatformClientService>()
    val sessionManager = koin.get<AppSessionManager>()
    val store = koin.get<PlatformRuntimeStore>()
    val windowState = remember { WindowState(width = 1200.dp, height = 900.dp) }

    val isDarkTheme by prefs.isDarkTheme.collectAsState()
    val fontScale by prefs.userFontScale.collectAsState()

    val branding by platformService.branding.collectAsState()
    val brandColors = ThemeManager.fromBranding(branding)
    val workStationInfo = remember(branding) {
        sessionManager.workStationInfo
    }

    bootstrapApp(
        listOf(
            module { single<AppConfig> {
                object : AppConfig {
                    override val prefsPrefix: String = "dev"
                    override val appPrefix: String = "dev"
                }
            } }
        )
    )

    Window(
        onCloseRequest = ::exitApplication,
        resizable = true,
        title = "CITACOM SYSTEMS"
    ) {
        AppTheme(
            isDarkTheme = isDarkTheme,
            fontScale = fontScale,
            brandColors = brandColors
        ) {
            DevApp()
        }
    }
}
