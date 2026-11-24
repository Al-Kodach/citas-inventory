package com.citacomsys.pharmacy.shared.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.citacomsys.core.spi.FeatureNavigationPlugin
import com.citacomsys.core.spi.PlatformFeature

import com.citacomsys.core.ui.ProductExtensionViewRegistry
import com.citacomsys.pharmacy.shared.pharmacySharedUiModule
import com.citacomsys.pharmacy.shared.presentation.component.PharmacyProductRenderer
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
class PharmacyInventoryModule : PlatformFeature {

    override val module = ProductModule.INVENTORY
    override val requiredUserCapabilities = setOf(UserCapability.MANAGE_INVENTORY)

    // Triggers background sync connections natively when this module state changes
    override val supportedEntities = setOf(EntityType.PRODUCT)

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
            // Register entries for pharmacy service, e.g Patient Management
        }
    }

    override fun injectUiComponents(koin: Koin) {
        koin.loadModules(listOf(pharmacySharedUiModule))
        ProductExtensionViewRegistry.register(
            ProductType.PHARMACY,
            PharmacyProductRenderer()
        )
    }

    // Encapsulated Server-Driven Isolation Lifecycle Hooks
    override fun activateServices(koin: Koin) {
    }

    override fun deactivate(koin: Koin) {
        koin.unloadModules(listOf(pharmacySharedUiModule))
    }
}
