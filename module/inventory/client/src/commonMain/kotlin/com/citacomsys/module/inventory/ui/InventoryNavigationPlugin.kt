package com.citacomsys.module.inventory.ui

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.InventoryNav
import com.citacomsys.core.spi.WsNavigationPlugin
import com.citacomsys.core.spi.scenes.ListDetailScene
import com.citacomsys.module.inventory.nav.BatchDetailsNavKey
import com.citacomsys.module.inventory.nav.CreateBatchNavKey
import com.citacomsys.module.inventory.nav.EditBatchNav
import com.citacomsys.module.inventory.nav.InventoryModuleNavKey
import com.citacomsys.module.inventory.nav.ProductFormNav
import com.citacomsys.module.inventory.nav.ProductMetadataNav
import com.citacomsys.module.inventory.nav.ProductStocksNavKey
import com.citacomsys.module.inventory.nav.StockTransferNavKey
import com.citacomsys.module.inventory.ui.screen.batch.BatchDetailsScreen
import com.citacomsys.module.inventory.ui.screen.batch.CreateBatchScreen
import com.citacomsys.module.inventory.ui.screen.product.ProductFormScreen
import com.citacomsys.module.inventory.ui.screen.product.ProductListScreen
import com.citacomsys.module.inventory.ui.screen.product.ProductMetadataScreen
import com.citacomsys.module.inventory.ui.screen.stocks.ProductStocksScreen
import com.citacomsys.module.inventory.ui.screen.stocks.StockTransferScreen
import com.citacomsys.module.inventory.ui.widget.LowStockAlertWidget
import com.citacomsys.core.spi.DashboardPlacement
import com.citacomsys.core.spi.Navigator
import com.citacomsys.core.spi.PluginDashboardWidget
import com.citacomsys.core.spi.PluginTabItem
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

internal class InventoryNavigationPlugin : WsNavigationPlugin {
    override val id = "InventoryFeatureModule"

    @OptIn(ExperimentalSerializationApi::class)
    override val serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclassesOfSealed<InventoryModuleNavKey>()
            subclass(InventoryNav::class, InventoryNav.serializer())
        }
    }

    override val tabs = listOf(
        PluginTabItem(
            route = InventoryNav,
            label = "Stock",
            unselectedIcon = AppIcons.Inventory,
            selectedIcon = AppIcons.Inventory2,
            order = 3
        )
    )

    override val dashboardWidgets: List<PluginDashboardWidget> = listOf(
        PluginDashboardWidget(
            id = "inventory.low_stock",
            placement = DashboardPlacement.LIST,
            order = 10,
            content = {
                LowStockAlertWidget()
            }
        )
    )

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    override fun EntryProviderScope<NavKey>.registerEntries(
        navigator: Navigator,
        isCompact: Boolean
    ) {

        entry<InventoryNav>(
            metadata = ListDetailScene.listPane()
        ) {
            ProductListScreen(
                onProductClick = { productId ->
                    navigator.navigateSingleTop(ProductStocksNavKey(productId))
                },
                onNewProduct = {
                    navigator.navigateSingleTop(ProductFormNav())
                },
                onMetadataClick = { productId ->
                    navigator.navigateSingleTop(ProductMetadataNav(productId))
                },
                onBack = {
                    navigator.goBack()
                }
            )
        }

        // Detail Pane: Physical Stock & Batches
        entry<ProductStocksNavKey>(
            metadata = ListDetailScene.detailPane()
        ) { key ->
            ProductStocksScreen(
                productId = key.productId,
                goBack = {
                    navigator.goBack()
                }
            )
        }

        entry<ProductMetadataNav>(
            metadata = ListDetailScene.detailPane()
        ) { key ->
            ProductMetadataScreen(
                productId = key.productId,
                onBack = { navigator.goBack() },
                onEdit = { navigator.navigateSingleTop(ProductFormNav(key.productId)) },
                onNavigateToStocks = {
                    navigator.goBack()
                    navigator.navigateSingleTop(ProductStocksNavKey(key.productId))
                }
            )
        }

        entry<ProductFormNav>(
            metadata = ListDetailScene.detailPane()
        ) { key ->
            ProductFormScreen(
                productId = key.productId,
                goBack = { navigator.goBack() },
            )
        }

        entry<CreateBatchNavKey>(
            metadata = ListDetailScene.detailPane()
        ) { key ->
            CreateBatchScreen(
                productId = key.productId,
                onGoBack = { navigator.goBack() },
            )
        }

        entry<EditBatchNav>(
            metadata = ListDetailScene.detailPane()
        ) { key ->
            CreateBatchScreen(
                productId = key.productId,
                batchId = key.batchId,
                onGoBack = { navigator.goBack() },
            )
        }

        entry<StockTransferNavKey>(
            metadata = ListDetailScene.detailPane()
        ) { key ->
            StockTransferScreen(
                productId = key.productId,
                batchId = key.batchId,
                onDismiss = { navigator.goBack() }
            )
        }

        entry<BatchDetailsNavKey>(
            metadata = ListDetailScene.detailPane()
        ) { key ->
            BatchDetailsScreen(
                productId = key.productId,
                batchId = key.batchId,
                onBack = { navigator.goBack() }
            )
        }
    }
}


/**
 * Navigates to a new route but ensures only one instance of a specific
 * type exists at the top, while allowing stacking for different sub-panes
 * of the same product.
 */
internal fun Navigator.navigateSingleTop(key: NavKey) {
    val currentTopRoute = state.topLevelRoute
    val stack = state.backStacks[currentTopRoute]

    if (!stack.isNullOrEmpty()) {
        val lastItem = stack.last()

        // If exactly the same (type + data), don't navigate at all
        if (lastItem == key) return

        val shouldReplace = when {
            // If both are sub-panes, check if we are switching products
            lastItem is InventoryModuleNavKey && key is InventoryModuleNavKey -> {
                val switchingProduct = lastItem.productId != key.productId
                val sameScreenType = lastItem::class == key::class

                // Replace if we changed products OR if we are pushing the same screen type
                switchingProduct || sameScreenType
            }

            // Fallback: replace if classes are identical
            lastItem::class == key::class -> true

            else -> false
        }

        if (shouldReplace) {
            stack.removeAt(stack.size - 1)
        }
    }

    // Now perform the actual navigation
    navigateToSingleTop(key)
}