package com.citacomsys.inventory

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.awaitApplication
import com.citacomsys.app.shared.app.AppShell
import com.citacomsys.app.shared.app.AppStageOrchestrator
import com.citacomsys.core.domain.repository.PreferencesRepository
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.system.AppNotificationManager
import com.citacomsys.core.notification.system.InternalAntilog
import com.citacomsys.core.splash.SplashScreen
import com.design.AppTheme
import com.design.ThemeManager
import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.local.LocalNotifications
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.FileKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.koin.core.Koin
import java.io.File


suspend fun main() = awaitApplication {
    println("---------------- Citas Platform Starting ----------------")
    LaunchedEffect(Unit) {
        val appDir = File(System.getProperty("user.home"), ".local/shared/citacomsys")
        val isDev = System.getProperty("ENV")?.lowercase()?.startsWith("dev") == true

        FileKit.init(
            appId = appDir.absolutePath,
            filesDir = File(appDir, "data"),
            cacheDir = File(appDir, "cache")
        )

        val minLogLevel = if (isDev) LogLevel.VERBOSE else LogLevel.ERROR
        Napier.base(InternalAntilog(minLogLevel))

        KMPNotifier.initialize(
            configuration = NotificationPlatformConfiguration.Desktop(
                showPushNotification = true,
                notificationIconPath = "com.citacomsys.core_client_design.design.generated.resources/drawable/ic_citas_logo.xml"
            ),
            LocalNotifications
        )

        AppNotificationManager.init { true }
    }


    val windowState = remember { WindowState(width = 1200.dp, height = 900.dp) }
    var isInitialized by remember { mutableStateOf(false) }
    var koinInstance by remember { mutableStateOf<Koin?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val koin = bootstrapApp()
            // Bootstrap orchestrator to determine initial stage without forcing MAIN_APP DI instantiations upfront
            koin.get<AppStageOrchestrator>()
            koin.get<PreferencesRepository>()
            koin.get<AppSessionManager>()
            koinInstance = koin
        }
        isInitialized = true
    }



    Window(
        onCloseRequest = ::exitApplication,
        title = "Citas Industrial Platform",
//        state = windowState
    ) {
        if (!isInitialized || koinInstance == null) {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        SplashScreen("Initializing Citas Platform...")
                    }
                }
            }
        } else {
             val prefs: PreferencesRepository = koinInject()

            val isDarkTheme by prefs.isDarkTheme.collectAsState()
            val fontScale by prefs.userFontScale.collectAsState()
            val branding by prefs.activeBranding.collectAsState()
            val brandColors = remember(branding, isDarkTheme) {
                ThemeManager.fromBranding(branding, isDark = isDarkTheme == true)
            }

            AppTheme(
                isDarkTheme = isDarkTheme,
                fontScale = fontScale,
                colorScheme = brandColors
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize()
                ) {
                    AppShell(
                        onExit = ::exitApplication,
                        topBarWrapper = { topBar ->
                            WindowDraggableArea {
                                topBar()
                            }
                        }
                    )
                }
            }
        }
    }
}
