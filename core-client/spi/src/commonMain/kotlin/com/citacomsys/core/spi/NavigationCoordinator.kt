package com.citacomsys.core.spi

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.modules.SerializersModule

/**
 * Interface defining the contract for a navigation coordinator that manages plugins
 * and global navigation state.
 */
interface NavigationCoordinator {
    val plugins: StateFlow<List<FeatureNavigationPlugin>>
    val activePluginSettingsContributions: StateFlow<List<PluginSettingsContribution>>
    val unifiedSerializersModule: SerializersModule
    val unifiedSerializersModuleFlow: StateFlow<SerializersModule>
    val activeDashboardWidgets: StateFlow<List<PluginDashboardWidget>>
    val activeQuickActions: StateFlow<List<QuickAction>> get() = MutableStateFlow(emptyList())
    val rawNavigationItems: StateFlow<List<PluginTabItem>>
    val isInitialized: StateFlow<Boolean>

    /**
     * Resolves a target route, potentially applying authorization or redirection logic.
     */
    fun resolveRoute(route: NavKey): NavKey {
        return route
    }

    /**
     * Diagnostic tool to log the current navigation state.
     */
    fun debugBackstack(navigator: Navigator)

    /**
     * Assembles all UI entries from active plugins into the current navigation graph.
     */
    fun EntryProviderScope<NavKey>.assembleAllEntries(navigator: Navigator, isCompact: Boolean)
    fun registerPlugin(plugin: FeatureNavigationPlugin)
}
