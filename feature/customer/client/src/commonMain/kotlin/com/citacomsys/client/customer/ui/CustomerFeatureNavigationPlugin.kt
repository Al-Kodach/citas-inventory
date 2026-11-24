package com.citacomsys.client.customer.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.citacomsys.client.customer.ui.screen.register.CustomerRegistrationScreen
import com.citacomsys.client.customer.ui.screen.list.CustomerListScreen
import com.citacomsys.client.customer.ui.screen.ledger.CustomerDetailsScreen
import com.citacomsys.client.customer.ui.widget.CustomerGrowthWidget
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.OrgNavigationPlugin
import com.citacomsys.module.api.customer.nav.CustomerListNav
import com.citacomsys.module.api.customer.nav.CustomerRegistrationNav
import com.citacomsys.module.api.customer.nav.CustomerDetailsNav
import com.citacomsys.types.UserCapability
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import com.citacomsys.core.spi.Navigator
import com.citacomsys.core.spi.PluginTabItem
import com.citacomsys.core.spi.DashboardPlacement
import com.citacomsys.core.spi.PluginDashboardWidget
import com.citacomsys.core.spi.scenes.BottomSheetSceneStrategy
import com.citacomsys.core.spi.scenes.ListDetailScene

class CustomerFeatureNavigationPlugin : OrgNavigationPlugin {
    override val id: String = "CustomerModule"

    override val serializersModule: SerializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(CustomerListNav::class, CustomerListNav.serializer())
            subclass(CustomerRegistrationNav::class, CustomerRegistrationNav.serializer())
            subclass(CustomerDetailsNav::class, CustomerDetailsNav.serializer())
        }
    }

    override val tabs = listOf(
        PluginTabItem(
            route = CustomerListNav,
            label = "Customers",
            selectedIcon = AppIcons.People,
            unselectedIcon = AppIcons.People,
            order = 5
        )
    )

    override val dashboardWidgets = listOf(
        PluginDashboardWidget(
            id = "customerState.growth",
            placement = DashboardPlacement.LIST,
            order = 10,
            content = {
                CustomerGrowthWidget()
            }
        )
    )

    override fun EntryProviderScope<NavKey>.registerEntries(
        navigator: Navigator,
        isCompact: Boolean
    ) {
        entry<CustomerListNav>(
            metadata = ListDetailScene.listPane()
        ) {
            CustomerListScreen(
                onBackClick = if (isCompact) navigator::goBack else null,
                onNewCustomer = { navigator.navigateToSingleTop(CustomerRegistrationNav()) },
                onSelectedCustomer = { cId, wsId ->
                    navigator.navigateToSingleTop(CustomerDetailsNav(cId, wsId))
                }
            )
        }

        entry<CustomerRegistrationNav>(
            metadata = BottomSheetSceneStrategy.bottomSheet()
        ) { key ->
            CustomerRegistrationScreen(
                customerId = key.customerId,
                goBack = { navigator.goBack() }
            )
        }

        entry<CustomerDetailsNav>(
            metadata = ListDetailScene.detailPane()
        ) { key ->
            CustomerDetailsScreen(
                customerId = key.customerId,
                workspaceId = key.workspaceId,
                onBack = { navigator.goBack() },
                onEditCustomer = {
                    navigator.navigateToSingleTop(CustomerRegistrationNav(it))
                }
            )
        }
    }

    override fun getRequiredCapabilities(key: NavKey): Set<UserCapability> {
        return when (key) {
            is CustomerListNav -> setOf(UserCapability.VIEW_CUSTOMERS)
            is CustomerRegistrationNav -> setOf(UserCapability.MANAGE_CUSTOMERS)
            is CustomerDetailsNav -> setOf(UserCapability.VIEW_CUSTOMERS)
            else -> emptySet()
        }
    }
}
