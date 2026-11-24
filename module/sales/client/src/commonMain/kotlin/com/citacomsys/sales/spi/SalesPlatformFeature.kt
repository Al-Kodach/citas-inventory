package com.citacomsys.sales.spi

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.citacomsys.api.billing.navkeys.SubscriptionHubNav
import com.citacomsys.api.sales.nav.PosCartNavKey
import com.citacomsys.api.sales.nav.PosModuleNavKey
import com.citacomsys.api.sales.nav.PosNavKey
import com.citacomsys.api.sales.nav.PosPaymentNavKey
import com.citacomsys.api.sales.nav.SalesDetailsNavKey
import com.citacomsys.api.sales.nav.SalesListNav
import com.citacomsys.api.sales.nav.ReceiptNavKey
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.PlatformFeature
import com.citacomsys.core.spi.WsNavigationPlugin
import com.citacomsys.core.spi.scenes.ListDetailScene
import com.citacomsys.sales.di.salesClientUiModuleDI
import com.citacomsys.sales.ui.screen.details.SaleDetailsScreen
import com.citacomsys.sales.ui.screen.details.ReceiptScreen
import com.citacomsys.sales.ui.screen.pos.PosCartScreen
import com.citacomsys.sales.ui.screen.pos.PosCatalogRoute
import com.citacomsys.sales.ui.screen.pos.PosPaymentScreen
import com.citacomsys.sales.ui.screen.sales.SalesHistoryScreen
import com.citacomsys.types.EntityType
import com.citacomsys.types.ProductModule
import com.citacomsys.types.UserCapability
import com.citacomsys.core.spi.FeatureNavigationPlugin
import com.citacomsys.core.spi.Navigator
import com.citacomsys.core.spi.PluginTabItem
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.Koin

class SalesPlatformFeature : PlatformFeature {

    override val module = ProductModule.SALES
    override val requiredUserCapabilities = setOf(UserCapability.PROCESS_SALES)
    override val supportedEntities = setOf(EntityType.SALE)

    // Packaged, completely self-contained Navigation 3 Plugin configuration
    @OptIn(ExperimentalSerializationApi::class)
    override val navigationPlugin: FeatureNavigationPlugin = object : WsNavigationPlugin {
        override val id = "module.sales"

        override val serializersModule: SerializersModule = SerializersModule { }

        // Expose items directly to feed into the parent UI Layout Scaffold
        override val tabs = listOf(
            PluginTabItem(
                route = PosNavKey,
                label = "POS",
                selectedIcon = AppIcons.PointOfSale,
                unselectedIcon = AppIcons.PointOfSale,
                order = 2
            ),
            PluginTabItem(
                route = SalesListNav,
                label = "Sales",
                selectedIcon = AppIcons.Payment,
                unselectedIcon = AppIcons.Payment,
                order = 3
            )
        )

        override fun EntryProviderScope<NavKey>.registerEntries(
            navigator: Navigator,
            isCompact: Boolean
        ) {
            entry<PosNavKey>(metadata = ListDetailScene.listPane()) {
                PosCatalogRoute { navigator.navigateToSingleTop(PosCartNavKey) }
            }

            entry<PosCartNavKey>(metadata = ListDetailScene.detailPane()) {
                PosCartScreen(
                    onCheckout = { navigator.navigateToSingleTop(PosPaymentNavKey) },
                    onBack = { navigator.goBack() }
                )
            }

            entry<PosPaymentNavKey>(metadata = ListDetailScene.detailPane()) {
                PosPaymentScreen(
                    onBack = { navigator.goBack() }
                )
            }

            entry<SalesListNav>(metadata = ListDetailScene.listPane()) {
                SalesHistoryScreen(
                    onBack = navigator::goBack,
                    onSaleSelected = { id, wsId ->
                        navigator.navigateToSingleTop(SalesDetailsNavKey(id, wsId))
                    }
                )
            }

            entry<SalesDetailsNavKey>(
                metadata = ListDetailScene.detailPane()
            ) { key ->
                SaleDetailsScreen(
                    saleId = key.id,
                    wsId = key.wsId,
                    onBack = { navigator.goBack() },
                    onSmsTop = { navigator.navigateToSingleTop(SubscriptionHubNav) },
                    onViewReceipt = { id ->
                        navigator.navigateToSingleTop(ReceiptNavKey(id))
                    }
                )
            }

            entry<ReceiptNavKey>(
                metadata = ListDetailScene.detailPane()
            ) { key ->
                ReceiptScreen(
                    saleId = key.id,
                    onBack = { navigator.goBack() }
                )
            }
        }

        override fun getRequiredCapabilities(key: NavKey): Set<UserCapability> {
            return when (key) {
                is PosNavKey -> setOf(UserCapability.VIEW_POS)
                is SalesListNav -> setOf(UserCapability.VIEW_SALES)
                else -> emptySet()
            }
        }
    }

    override fun injectUiComponents(koin: Koin) {
        koin.loadModules(listOf(salesClientUiModuleDI))
    }

    override fun activateServices(koin: Koin) {}

    override fun deactivate(koin: Koin) {
        koin.unloadModules(listOf(salesClientUiModuleDI))
    }
}

/**
 * Navigates to a new route but ensures only one instance of a specific
 * type exists at the top, while allowing stacking for different sub-panes
 * of the same product.
 */
internal fun Navigator.handlePosNavigation(key: NavKey) {
    val currentTopRoute = state.topLevelRoute
    val stack = state.backStacks[currentTopRoute]

    if (!stack.isNullOrEmpty()) {
        val lastItem = stack.last()

        // If exactly the same (type + data), don't navigate at all
        if (lastItem == key) return

        val shouldHandle = lastItem is PosModuleNavKey && key is PosModuleNavKey

        // Next view is cart
        if (shouldHandle && lastItem == PosNavKey) {
            navigateToSingleTop(PosCartNavKey)
            return
        }

        if (shouldHandle && lastItem == PosCartNavKey) {
            navigateToSingleTop(PosPaymentNavKey)
            return
        }

        val shouldReplace = when {
            // Fallback: replace if classes are identical
            lastItem::class == key::class -> true

            else -> false
        }

        // Going back
        if (shouldReplace) {
            stack.removeAt(stack.size - 1)
        }

//        navigateToSingleTop(key)
    }
//
//    // Now perform the actual navigation
//    navigateToSingleTop(key)
}