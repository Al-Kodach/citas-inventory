package com.citacomsys.retail.shared

import com.citacomsys.core.domain.repository.SearchResult
import com.citacomsys.core.spi.Navigator
import com.citacomsys.core.spi.PlatformFeature
import com.citacomsys.core.spi.ProductExtPlugin
import com.citacomsys.core.ui.ProductExtensionViewRegistry
import com.citacomsys.module.inventory.nav.ProductStocksNavKey
import com.citacomsys.retail.shared.presentation.component.RetailProductRenderer
import com.citacomsys.types.EntityType
import com.citacomsys.types.ProductModule
import com.citacomsys.types.ProductType
import io.github.aakira.napier.Napier
import org.koin.core.Koin

class RetailProductExtPlugin : ProductExtPlugin {
    override val id: ProductType = ProductType.RETAIL

    override val manifest: Map<ProductModule, PlatformFeature> = mapOf()

    override fun onActivate(koin: Koin) {
        Napier.i(tag = "RETAIL_PLUGIN", message =  "Activating... ")

        koin.loadModules(listOf(retailSharedUiModule))
        ProductExtensionViewRegistry.register(
            ProductType.RETAIL,
            RetailProductRenderer()
        )
    }

    override fun onDeactivate(koin: Koin) {
        Napier.i(tag = "RETAIL_PLUGIN", message =  "Deactivating... ")

        ProductExtensionViewRegistry.unregister(ProductType.RETAIL)
        koin.unloadModules(listOf(retailSharedUiModule))
    }

    override fun onSearchResultClick(result: SearchResult, navigator: Navigator) {
        when (result.type) {
            EntityType.PRODUCT -> navigator.navigateToSingleTop(ProductStocksNavKey(result.id))
            else -> {}
        }
    }
}
