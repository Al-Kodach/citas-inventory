package com.citacomsys.retail.shared.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.citacomsys.core.spi.FeatureNavigationPlugin
import com.citacomsys.core.spi.PlatformFeature
import com.citacomsys.core.ui.ProductExtensionViewRegistry
import com.citacomsys.retail.shared.retailSharedUiModule
import com.citacomsys.retail.shared.presentation.component.RetailProductRenderer
import com.citacomsys.types.EntityType
import com.citacomsys.types.ProductType
import com.citacomsys.types.ProductModule
import com.citacomsys.types.UserCapability
import com.citacomsys.core.spi.Navigator
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.core.Koin

@OptIn(ExperimentalMaterial3Api::class)
class RetailInventoryModule : PlatformFeature {
    override val module = ProductModule.INVENTORY
    override val requiredUserCapabilities = setOf(UserCapability.MANAGE_INVENTORY)

    override val supportedEntities = setOf(EntityType.PRODUCT, EntityType.STOCK_BATCH)

    // Fully Consolidated Navigation 3 Engine for Inventory
    @OptIn(ExperimentalSerializationApi::class)
    override val navigationPlugin: FeatureNavigationPlugin = object : FeatureNavigationPlugin {
        override val id: String = "PharmacyInventoryModule"
        override val serializersModule: SerializersModule = SerializersModule {
            polymorphic(NavKey::class) {
            }
        }

        override fun EntryProviderScope<NavKey>.registerEntries(
            navigator: Navigator,
            isCompact: Boolean
        ) {
            // Register entries for Retail service
        }
    }


    override fun injectUiComponents(koin: Koin) {
        koin.loadModules(listOf(retailSharedUiModule))
        ProductExtensionViewRegistry.register(
            ProductType.RETAIL,
            RetailProductRenderer()
        )
    }

    override fun activateServices(koin: Koin) {
    }

    override fun deactivate(koin: Koin) {
        koin.unloadModules(listOf(retailSharedUiModule))
    }
}
