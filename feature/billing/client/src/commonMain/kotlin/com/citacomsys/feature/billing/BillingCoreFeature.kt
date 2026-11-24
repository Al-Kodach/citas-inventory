package com.citacomsys.feature.billing

import TransactionHistoryViewModel
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.compose.serialization.serializers.SnapshotStateListSerializer
import com.citacomsys.api.billing.navkeys.BillingHistoryNav
import com.citacomsys.api.billing.navkeys.HistoryInvoiceDetailsNav
import com.citacomsys.api.billing.navkeys.InvoiceDetailsNav
import com.citacomsys.api.billing.navkeys.SmsPurchaseReviewNav
import com.citacomsys.api.billing.navkeys.SmsTopupNav
import com.citacomsys.api.billing.navkeys.SubscriptionHubNav
import com.citacomsys.api.billing.navkeys.SubscriptionManagementNav
import com.citacomsys.api.billing.navkeys.SubscriptionManagementRoute
import com.citacomsys.api.billing.navkeys.TenantBillingNav
import com.citacomsys.api.billing.navkeys.UpgradeReviewNav
import com.citacomsys.api.billing.service.BillingApiService
import com.citacomsys.core.spi.CoreFeature
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.FeatureNavigationPlugin
import com.citacomsys.core.spi.WsNavigationPlugin
import com.citacomsys.core.spi.Navigator
import com.citacomsys.core.spi.PluginTabItem
import com.citacomsys.feature.billing.remote.KtorBillingApiService
import com.citacomsys.feature.billing.repository.BillingRepository
import com.citacomsys.feature.billing.repository.BillingRepositoryImpl
import com.citacomsys.feature.billing.ui.screen.SubscriptionHubScreen
import com.citacomsys.feature.billing.ui.screen.WorkspaceSubscriptionViewModel
import com.citacomsys.feature.billing.ui.screen.UpgradeReviewScreen
import com.citacomsys.feature.billing.ui.screen.billdetails.BillingViewModel
import com.citacomsys.feature.billing.ui.screen.billdetails.InvoiceDetailsView
import com.citacomsys.feature.billing.ui.screen.history.TransactionHistoryScreen
import com.citacomsys.feature.billing.ui.screen.manage.SubscriptionManagementScreen
import com.citacomsys.feature.billing.ui.screen.manage.SubscriptionManagementViewModel
import com.citacomsys.feature.billing.ui.screen.sms.SmsPackageSelectionScreen
import com.citacomsys.feature.billing.ui.screen.sms.SmsPurchaseReviewScreen
import com.citacomsys.feature.billing.ui.screen.sms.SmsPurchaseViewModel
import com.citacomsys.core.spi.scenes.ListDetailScene
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.Koin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val billingClientModule = module {
    single<BillingApiService> { KtorBillingApiService(get()) }
    single<BillingRepository> {
        BillingRepositoryImpl(get())
    }

    viewModelOf(::WorkspaceSubscriptionViewModel)
    viewModelOf(::BillingViewModel)
    viewModelOf(::SmsPurchaseViewModel)
    viewModelOf(::SubscriptionManagementViewModel)
    viewModelOf(::TransactionHistoryViewModel)
}

class BillingCoreFeature : CoreFeature {
    override val id: String = "core.billing"
    override val navigationPlugin: FeatureNavigationPlugin = SubscriptionFeatureNavPlugin()

    override fun activate(koin: Koin) {
        koin.loadModules(listOf(billingClientModule))
    }

    override fun deactivateService(koin: Koin) {
        koin.unloadModules(listOf(billingClientModule))
    }
}


@OptIn(ExperimentalMaterial3AdaptiveApi::class)
class SubscriptionFeatureNavPlugin : WsNavigationPlugin {
    override val id: String = "tenant_billing.module"

    @OptIn(ExperimentalSerializationApi::class)
    override val serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclassesOfSealed<TenantBillingNav>()
        }
    }

    override val tabs: List<PluginTabItem> = listOf(
        PluginTabItem(
            route = SubscriptionHubNav,
            label = "Subscription",
            order = 100,
            selectedIcon = AppIcons.PaymentFilled,
            unselectedIcon = AppIcons.PaymentOutlined
        )
    )

    override fun EntryProviderScope<NavKey>.registerEntries(
        navigator: Navigator,
        isCompact: Boolean
    ) {
        entry<SubscriptionHubNav> {
            SubscriptionHubScreen(
                onNavigate = { route -> navigator.navigateToSingleTop(route) }
            )
        }

        entry<SubscriptionManagementNav> {
            // 1. This VM is now the "Source of Truth" for both screens
            val sharedViewModel: SubscriptionManagementViewModel = koinViewModel()

            // Local backstack for the nested flow
            val backStack: MutableList<SubscriptionManagementRoute> =
                rememberSerializable(serializer = SnapshotStateListSerializer()) {
                    mutableStateListOf(SubscriptionManagementNav)
                }

            // Initialize the local backstack with the STARTING key
            // Crucial: SnapshotStateListSerializer keeps this safe during process death
            val localBackStack = rememberSerializable(serializer = SnapshotStateListSerializer()) {
                mutableStateListOf(SubscriptionManagementNav)
            }

            NavDisplay(
                backStack = backStack,
                entryProvider = entryProvider {
                    // Screen 1: Management / Selection
                    entry<SubscriptionManagementNav> {
                        SubscriptionManagementScreen(
                            viewModel = sharedViewModel,
                            onBack = {
                                navigator.goBack()
                            },
                            onNavigateToPreview = {
                                backStack.add(UpgradeReviewNav)
                            }
                        )
                    }

                    // Screen 2: Review & Confirm
                    entry<UpgradeReviewNav> {
                        UpgradeReviewScreen(
                            viewModel = sharedViewModel,
                            onBack = {
                                // Internal backstack pop
                                backStack.removeLast()
                            }
                        )
                    }
                }
            )
        }


        entry<SmsTopupNav> {
            SmsPackageSelectionScreen(
                onBack = { navigator.goBack() },
                onNavigateToReview = { pkgId ->
                    navigator.navigateToSingleTop(SmsPurchaseReviewNav(pkgId))
                }
            )
        }

        entry<SmsPurchaseReviewNav> { key ->
            SmsPurchaseReviewScreen(
                packageId = key.packageId,
                onBack = { navigator.goBack() },
                onConfirmed = { billId ->
                    navigator.navigateToSingleTop(InvoiceDetailsNav(billId))
                }
            )
        }

        entry<BillingHistoryNav>(
            metadata = ListDetailScene.listPane()
        ) {
            TransactionHistoryScreen(
                onBack = { navigator.goBack() },
                onInvoiceClick = { billId ->
                    navigator.navigateToSingleTop(HistoryInvoiceDetailsNav(billId))
                }
            )
        }

        entry<HistoryInvoiceDetailsNav>(
            metadata = ListDetailScene.detailPane()
        ) { key ->
            InvoiceDetailsView(invoiceId = key.invoiceId, onBack = { navigator.goBack() })
        }

        entry<InvoiceDetailsNav> { key ->
            InvoiceDetailsView(invoiceId = key.invoiceId, onBack = { navigator.goBack() })
        }

    }
}
