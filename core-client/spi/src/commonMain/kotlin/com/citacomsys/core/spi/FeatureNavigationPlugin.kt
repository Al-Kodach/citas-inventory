package com.citacomsys.core.spi

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.citacomsys.types.UserCapability
import kotlinx.serialization.modules.SerializersModule
import kotlin.reflect.KClass

data class PluginTabItem(
    val route: NavKey,
    val label: String,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector,
    val order: Int = 100,
    val isFooter: Boolean = false
)

enum class DashboardPlacement {
    HERO,
    ANALYTICS_CAROUSEL,
    ACTIVITY_STREAM,
    GRID,
    ACTIVITY,
    SIDEBAR,
    LIST
}

data class PluginDashboardWidget(
    val id: String,
    val placement: DashboardPlacement,
    val content: @Composable () -> Unit,
    val order: Int = 100,
    val preferredSpan: (Boolean) -> Int = { if (it) 12 else 6 }
)

data class QuickAction(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val onClick: (Navigator) -> Unit,
    val order: Int = 100,
    val requiredCapabilities: Set<UserCapability> = emptySet()
)

enum class SettingsGroup {
    ACCOUNT,
    NODE,
    HUB,
    FLEET,
    SECURITY,
    SYSTEM,
    PERSONALIZATION
}

sealed interface PluginSettingsContribution {
    val id: String
    val group: SettingsGroup
    val order: Int
    val requiredCapabilities: Set<UserCapability> get() = emptySet()

    data class CustomWidget(
        override val id: String,
        override val group: SettingsGroup,
        override val order: Int = 100,
        override val requiredCapabilities: Set<UserCapability> = emptySet(),
        val content: @Composable () -> Unit
    ) : PluginSettingsContribution
}

interface FeatureNavigationPlugin {
    val id: String
    val tabs: List<PluginTabItem> get() = emptyList()
    val dashboardWidgets: List<PluginDashboardWidget> get() = emptyList()
    val quickActions: List<QuickAction> get() = emptyList()
    val pluginSettingsContributions: List<PluginSettingsContribution> get() = emptyList()
    val serializersModule: SerializersModule

    val overriddenTabs: Set<KClass<out NavKey>> get() = emptySet()

    fun isVisible(
        route: NavKey? = null,
        item: Any,
        mode: AppOperatingMode
    ): Boolean = true

    fun EntryProviderScope<NavKey>.registerEntries(navigator: Navigator, isCompact: Boolean)

    fun getRequiredCapabilities(key: NavKey): Set<UserCapability> = emptySet()

    fun resolveRoute(route: NavKey): NavKey? {
        return null
    }
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class PlatformUri {
    val host: String?
    val path: String?
    fun getQueryParameter(key: String): String?

    companion object {
        fun parse(url: String): PlatformUri?
    }
}


interface WsNavigationPlugin : FeatureNavigationPlugin {
    override fun isVisible(route: NavKey?, item: Any, mode: AppOperatingMode): Boolean {
        return mode == AppOperatingMode.WS
    }
}

interface OrgNavigationPlugin : FeatureNavigationPlugin {
    override fun isVisible(route: NavKey?, item: Any, mode: AppOperatingMode): Boolean {
        return mode == AppOperatingMode.ORG
    }
}

