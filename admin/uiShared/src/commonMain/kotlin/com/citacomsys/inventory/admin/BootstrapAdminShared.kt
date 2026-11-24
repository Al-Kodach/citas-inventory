package com.citacomsys.inventory.admin

import com.citacomsys.core.data.di.coreDataModule
import com.citacomsys.core.database.di.databaseModule
import com.citacomsys.core.notification.di.notificationModule
import com.citacomsys.core.remote.di.networkModule
import com.citacomsys.core.security.sharedSecurityModule
import com.citacomsys.device.di.deviceServiceModule
import com.citacomsys.feature.telemetry.di.telemetryFeatureModuleDi
import com.citacomsys.inventory.admin.app.AdminAppPlatform
import com.citacomsys.settings.settingsFeatureModule
import com.citacomsys.shared.AppConfig
import com.citacomsys.types.ProductType
import org.koin.core.Koin
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

class AdminAppConfig : AppConfig {
    override val appPrefix: String = "ADM"
    override val prefsPrefix: String = "admin"
    override val apiSecret: String = "admin.api.secret"
}

expect fun initNapier(context: Any? = null)

private var koinRef: Koin? = null

fun bootstrapAdmin(
    appModules: List<Module> = emptyList()
): Koin {
    koinRef?.let { return it }

    val app = startKoin {
        printLogger()
        initNapier()

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

        loadKoinModules(appModules)
    }

    val koin = app.koin

    koinRef = koin

    AdminAppPlatform.bootstrap(koin)

    return koin
}





