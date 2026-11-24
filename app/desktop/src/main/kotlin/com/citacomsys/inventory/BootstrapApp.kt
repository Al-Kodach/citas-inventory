package com.citacomsys.inventory

import com.citacomsys.app.shared.app.AppStageOrchestrator
import com.citacomsys.app.shared.boot.AppPlatform
import com.citacomsys.app.shared.di.appSharedModule
import com.citacomsys.app.shared.di.coreClientModule
import com.citacomsys.app.shared.di.coreFeatures
import com.citacomsys.app.shared.di.serviceModule
import com.citacomsys.core.notification.system.InternalAntilog
import com.citacomsys.core.spi.ProductRegistry
import com.citacomsys.pharmacy.shared.PharmacyProductExtPlugin
import com.citacomsys.retail.shared.RetailProductExtPlugin
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.module.Module
import org.koin.dsl.module

private var koinRef: Koin? = null

/**
 * Bootstraps the Citas Desktop Platform with core modules.
 * Delegates to [AppPlatform] for specialized lifecycle orchestration.
 */
fun bootstrapApp(
    appModules: List<Module> = emptyList()
): Koin {
    koinRef?.let { return it }

    Napier.base(InternalAntilog())
    Napier.i { "Bootstrapping Citas Platform (Desktop)..." }

    val koin = startKoin {
        printLogger(Level.ERROR)
        allowOverride(true)
        modules(
            coreClientModule,
            appSharedModule,
            coreFeatures,
            serviceModule,
            module {
                single{ koin }
                includes(appModules)
            },
        )
    }.koin

    koinRef = koin

//    // Initialize Orchestrator
//    koin.get<AppStageOrchestrator>()

    // Register Product Plugins
    ProductRegistry.register(PharmacyProductExtPlugin())
    ProductRegistry.register(RetailProductExtPlugin())

    // Delegate lifecycle orchestration to the platform layer
    AppPlatform.bootstrap(koin)

//    koin.get<CoroutineScope>().launch(Dispatchers.IO) {
//
//    }

    return koin
}
