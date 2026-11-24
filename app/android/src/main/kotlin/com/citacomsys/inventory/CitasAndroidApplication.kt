package com.citacomsys.inventory

import android.app.Application
import com.citacomsys.app.shared.boot.AppPlatform
import com.citacomsys.app.shared.di.appSharedModule
import com.citacomsys.app.shared.di.coreClientModule
import com.citacomsys.app.shared.di.coreFeatures
import com.citacomsys.app.shared.di.serviceModule
import com.citacomsys.core.spi.ProductRegistry
import com.citacomsys.pharmacy.shared.PharmacyProductExtPlugin
import com.citacomsys.retail.shared.RetailProductExtPlugin
import com.citacomsys.core.notification.system.InternalAntilog
import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.local.LocalNotifications
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module

class CitasAndroidApplication : Application() {
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
            androidContext(this@CitasAndroidApplication)
            modules(
                coreClientModule,
                appSharedModule,
                coreFeatures,
                serviceModule,
                module {
                    single { koin }
                    single { get<Application>().applicationContext }
                }
            )
        }.koin

        ProductRegistry.register(PharmacyProductExtPlugin())
        ProductRegistry.register(RetailProductExtPlugin())

        AppPlatform.bootstrap(koin)
    }
}
