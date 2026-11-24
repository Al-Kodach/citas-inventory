package com.citacomsys.inventory.admin.billing

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.FeatureNavigationPlugin
import com.citacomsys.core.spi.PlatformFeature
import com.citacomsys.inventory.admin.billing.di.adminBillingSharedModule
import com.citacomsys.inventory.admin.billing.ui.AdminBillingListScreen
import com.citacomsys.inventory.admin.billing.ui.AdminBillingWidget
import com.citacomsys.inventory.admin.billing.ui.InvoiceDetailsScreen
import com.citacomsys.types.EntityType
import com.citacomsys.types.UserCapability
import com.citacomsys.core.spi.DashboardPlacement
import com.citacomsys.core.spi.Navigator
import com.citacomsys.core.spi.PluginDashboardWidget
import com.citacomsys.core.spi.PluginTabItem
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.core.Koin

class AdminBillingPlatformFeature : PlatformFeature {
    override val navigationPlugin: FeatureNavigationPlugin = AdminBillingFeatureNavPlugin()

    override val supportedEntities: Set<EntityType> = emptySet()
    override val requiredUserCapabilities: Set<UserCapability> =
        setOf(UserCapability.MANAGE_BILLING)

    override fun activateServices(koin: Koin) {
        koin.loadModules(listOf(adminBillingSharedModule))
    }

    override fun deactivate(koin: Koin) {
        koin.unloadModules(listOf(adminBillingSharedModule))
    }
}


@Serializable
sealed interface AdminBillingNavKey : NavKey

@Serializable
data object AdminAdminBillingNav : AdminBillingNavKey

@SerialName("admin.billing.details")
@Serializable
data class AdminBillingDetailsNav(val invoiceId: String) : AdminBillingNavKey


class AdminBillingFeatureNavPlugin : FeatureNavigationPlugin {
    override val id: String = "admin.billing.module"

    @OptIn(ExperimentalSerializationApi::class)
    override val serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclassesOfSealed<AdminBillingNavKey>()
        }
    }

    override val tabs = listOf(
        PluginTabItem(
            route = AdminAdminBillingNav,
            label = "Billing",
            unselectedIcon = AppIcons.Receipt,
            selectedIcon = AppIcons.ReceiptLong
        )
    )

    override val dashboardWidgets: List<PluginDashboardWidget> = listOf(
        PluginDashboardWidget(
            id = "admin.billing.summary",
            placement = DashboardPlacement.ANALYTICS_CAROUSEL,
            order = 10,
            content = {
                AdminBillingWidget()
            }
        )
    )

    @OptIn(ExperimentalMaterial3Api::class)
    override fun EntryProviderScope<NavKey>.registerEntries(
        navigator: Navigator,
        isCompact: Boolean
    ) {
        entry<AdminAdminBillingNav> {
            AdminBillingListScreen(
                onSelected = { invoiceId ->
                    navigator.navigateToSingleTop(AdminBillingDetailsNav(invoiceId))
                }
            )
        }

        entry<AdminBillingDetailsNav>(
            metadata = DialogSceneStrategy.dialog()
        ) { key ->
            InvoiceDetailsScreen(
                invoiceId = key.invoiceId,
                onBackClick = { navigator.goBack() }
            )
        }
    }
}
