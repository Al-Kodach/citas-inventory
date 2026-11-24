package com.citacomsys.feature.support.client.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SupportAgent
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.citacomsys.core.spi.FeatureNavigationPlugin
import com.citacomsys.core.spi.CoreFeature
import com.citacomsys.feature.support.api.navigation.SupportHubNavKey
import com.citacomsys.feature.support.api.navigation.SubmitTicketNavKey
import com.citacomsys.feature.support.api.navigation.SupportTicketDetailNavKey
import com.citacomsys.feature.support.client.di.supportFeatureModule
import com.citacomsys.feature.support.client.ui.screen.SupportHubScreen
import com.citacomsys.feature.support.client.ui.screen.SubmitTicketScreen
import com.citacomsys.feature.support.client.ui.screen.SupportTicketDetailScreen
import com.citacomsys.core.spi.Navigator
import com.citacomsys.core.spi.PluginTabItem
import kotlinx.serialization.modules.SerializersModule
import org.koin.compose.koinInject
import org.koin.core.Koin

class SupportCoreFeature : CoreFeature {
    override val id: String = "core.support"
    override val navigationPlugin: FeatureNavigationPlugin = SupportNavPlugin()

    override fun activate(koin: Koin) {
        koin.loadModules(listOf(supportFeatureModule))
    }
}

class SupportNavPlugin : FeatureNavigationPlugin {
    override val id: String = "core.support"
    override val serializersModule: SerializersModule = SerializersModule {
//        polymorphic(NavKey::class) {
//            subclass(SupportHubNavKey::class, SupportHubNavKey.serializer())
//            subclass(SubmitTicketNavKey::class, SubmitTicketNavKey.serializer())
//            subclass(SupportTicketDetailNavKey::class, SupportTicketDetailNavKey.serializer())
//        }
    }

    override val tabs = listOf(
        PluginTabItem(
            route = SupportHubNavKey,
            label = "Support & Reviews",
            order = 950,
            isFooter = true,
            selectedIcon = Icons.Default.SupportAgent,
            unselectedIcon = Icons.Default.SupportAgent
        )
    )

    override fun EntryProviderScope<NavKey>.registerEntries(
        navigator: Navigator,
        isCompact: Boolean
    ) {
        entry<SupportHubNavKey> {
            val viewModel: SupportViewModel = koinInject()
            SupportHubScreen(
                viewModel = viewModel,
                onNavigateSubmit = { type ->
                    navigator.navigateTo(SubmitTicketNavKey(type))
                },
                onNavigateDetail = { ticketId ->
                    navigator.navigateTo(
                        SupportTicketDetailNavKey(
                            ticketId
                        )
                    )
                },
                onBack = { navigator.goBack() }
            )
        }

        entry<SubmitTicketNavKey> { key ->
            val viewModel: SupportViewModel = koinInject()
            SubmitTicketScreen(
                viewModel = viewModel,
                initialType = key.initialType,
                onBack = { navigator.goBack() }
            )
        }

        entry<SupportTicketDetailNavKey> { key ->
            val viewModel: SupportViewModel = koinInject()
            SupportTicketDetailScreen(
                viewModel = viewModel,
                ticketId = key.ticketId,
                onBack = { navigator.goBack() }
            )
        }
    }
}
