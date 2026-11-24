package com.citacomsys.inventory.admin


import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.citacomsys.core.domain.repository.PreferencesRepository
import com.citacomsys.core.notification.system.AppNotificationManager
import com.design.Dimensions
import com.citacomsys.inventory.admin.app.AdminApp
import com.design.AppTheme
import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.local.LocalNotifications
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import io.github.vinceglb.filekit.FileKit
import java.io.File

fun main() = application {
    println("---------------- Citas Platform Starting ----------------")
    val appDir = File(System.getProperty("user.home"), ".local/shared/.citacomsys-inventory")

    FileKit.init(
        appId = "citas_admin",
        filesDir = File(appDir, "data"),
        cacheDir = File(appDir, "cache")
    )

    KMPNotifier.initialize(
        configuration = NotificationPlatformConfiguration.Desktop(
            showPushNotification = false
        ),
        LocalNotifications
    )

    AppNotificationManager.init()

    val koin = bootstrapAdmin()

    val prefs = koin.get<PreferencesRepository>()
    val windowState = WindowState(width = 1200.dp, height = 900.dp)

    val isDarkTheme by prefs.isDarkTheme.collectAsState()
    val fontScale by prefs.userFontScale.collectAsState()

    Window(
        onCloseRequest = {
            exitApplication()
        },
        title = "Citas Industrial Platform",
        undecorated = true,
        state = windowState
    ) {
        AppTheme(
            isDarkTheme = isDarkTheme,
            fontScale = fontScale
        ) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent()
                            }
                        }
                    }
            ) {
                AdminApp(
                    onExit = {
                        exitApplication()
                    },
                    topBarWrapper = { topBar ->
                        WindowDraggableArea {
                            topBar()
                        }
                    },
                    trailingContent = {
                        val hubIconSize = Dimensions.iconSize()
                        val barButtonSize = hubIconSize + 14.dp

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 16.dp)
                        ) {
                            IconButton(
                                onClick = { windowState.isMinimized = true },
                                modifier = Modifier.size(barButtonSize)
                            ) {
                                Icon(
                                    Icons.Default.Remove,
                                    "Minimize",
                                    modifier = Modifier.size(hubIconSize)
                                )
                            }
                            IconButton(
                                onClick = {
                                    windowState.placement =
                                        if (windowState.placement == WindowPlacement.Maximized) {
                                            WindowPlacement.Floating
                                        } else {
                                            WindowPlacement.Maximized
                                        }
                                },
                                modifier = Modifier.size(barButtonSize)
                            ) {
                                Icon(
                                    Icons.Default.CropSquare,
                                    "Maximize",
                                    modifier = Modifier.size(hubIconSize - 2.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    exitApplication()
                                },
                                modifier = Modifier.size(barButtonSize),
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    "Close",
                                    modifier = Modifier.size(hubIconSize)
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

