package com.citacomsys.inventory.admin

import android.app.Application
import com.citacomsys.core.data.di.coreDataModule
import com.citacomsys.core.database.di.databaseModule
import com.citacomsys.core.notification.di.notificationModule
import com.citacomsys.core.notification.system.AppNotificationManager
import com.citacomsys.core.security.sharedSecurityModule
import com.citacomsys.device.di.deviceServiceModule
import com.citacomsys.feature.telemetry.di.telemetryFeatureModuleDi
import com.citacomsys.inventory.admin.app.AdminAppPlatform
import com.citacomsys.settings.settingsFeatureModule
import com.citacomsys.shared.AppConfig
import com.citacomsys.core.notification.system.InternalAntilog
import com.citacomsys.core.remote.di.networkModule
import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.local.LocalNotifications
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module

class CitasAdminApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        Napier.base(InternalAntilog())
        Napier.i { "Bootstrapping Citas Platform (Android)..." }

        KMPNotifier.initialize(
            configuration = NotificationPlatformConfiguration.Android(
                notificationIconResId = R.drawable.ic_launcher_foreground,
                showPushNotification = false
            ),
            LocalNotifications
        )

        val koin = startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@CitasAdminApplication)

            modules(
                coreDataModule,
                databaseModule,
                deviceServiceModule,
                sharedSecurityModule,
                networkModule,
                notificationModule,
                settingsFeatureModule,
                adminAppSharedModule,
                telemetryFeatureModuleDi,
                module {
                    single<AppConfig> { AdminAppConfig() }
                }
            )

        }.koin

        AppNotificationManager.init()
        AdminAppPlatform.bootstrap(koin)
    }
}