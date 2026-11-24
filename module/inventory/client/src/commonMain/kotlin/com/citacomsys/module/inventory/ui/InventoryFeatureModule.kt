package com.citacomsys.module.inventory.ui

import com.citacomsys.core.spi.PlatformFeature
import com.citacomsys.core.sync.engine.EntitySyncEngine
import com.citacomsys.module.inventory.sync.StockBatchSyncHandler
import com.citacomsys.core.spi.FeatureNavigationPlugin
import org.koin.core.Koin

class InventoryFeatureModule : PlatformFeature {
    override val navigationPlugin: FeatureNavigationPlugin = InventoryNavigationPlugin()

    override fun injectUiComponents(koin: Koin) {
        // inventoryClientUiDI already injected
    }

    override fun activateServices(koin: Koin) {
        // Activate service e.g sync engine
    }

    override fun deactivate(koin: Koin) {
    }
}