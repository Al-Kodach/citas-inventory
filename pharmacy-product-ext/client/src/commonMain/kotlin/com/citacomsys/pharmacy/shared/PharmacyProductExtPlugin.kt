package com.citacomsys.pharmacy.shared

import com.citacomsys.api.sales.nav.SalesDetailsNavKey
import com.citacomsys.core.domain.repository.SearchResult
import com.citacomsys.core.spi.Navigator
import com.citacomsys.core.spi.PlatformFeature
import com.citacomsys.core.spi.ProductExtPlugin
import com.citacomsys.core.ui.ProductExtensionViewRegistry
import com.citacomsys.module.api.customer.nav.CustomerRegistrationNav
import com.citacomsys.module.inventory.nav.ProductStocksNavKey
import com.citacomsys.pharmacy.shared.presentation.component.PharmacyProductRenderer
import com.citacomsys.types.EntityType
import com.citacomsys.types.ProductModule
import com.citacomsys.types.ProductType
import io.github.aakira.napier.Napier
import org.koin.core.Koin

class PharmacyProductExtPlugin : ProductExtPlugin {
    override val id: ProductType = ProductType.PHARMACY

    override val manifest: Map<ProductModule, PlatformFeature> = mapOf()

    override fun onActivate(koin: Koin) {
        Napier.i(tag = "PHARMACY_PLUGIN", message =  "Activating... ")

        koin.loadModules(listOf(pharmacySharedUiModule))
        ProductExtensionViewRegistry.register(
            ProductType.PHARMACY,
            PharmacyProductRenderer()
        )
    }

    override fun onDeactivate(koin: Koin) {
        Napier.i(tag = "PHARMACY_PLUGIN", message =  "Deactivating... ")

        ProductExtensionViewRegistry.unregister(ProductType.PHARMACY)
        koin.unloadModules(listOf(pharmacySharedUiModule))
    }

    override fun onSearchResultClick(result: SearchResult, navigator: Navigator) {
        when (result.type) {
            EntityType.PRODUCT -> navigator.navigateToSingleTop(ProductStocksNavKey(result.id))
            EntityType.SALE -> navigator.navigateToSingleTop(SalesDetailsNavKey(result.id, ""))
            EntityType.CUSTOMER -> navigator.navigateToSingleTop(CustomerRegistrationNav(result.id))
            else -> {}
        }
    }
}
