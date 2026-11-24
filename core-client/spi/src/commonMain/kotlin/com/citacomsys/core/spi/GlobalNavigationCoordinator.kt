package com.citacomsys.core.spi

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.citacomsys.core.domain.security.UserCapabilityManager
import com.citacomsys.types.UserCapability
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus

/**
 * Global Coordinator that manages all plugins and parses global deep link Intents.
 * Now reactive and driven by the [AppManifest].
 */
@OptIn(FlowPreview::class)
class GlobalNavigationCoordinator(
    manifest: AppManifest,
    scope: CoroutineScope,
    private val baseSerializersModule: SerializersModule,
    private val capsManager: UserCapabilityManager
) : NavigationCoordinator {
    private val TAG = "GLOBAL_NAV_COORDINATOR"

    // Core Plugin Registry (Source of Truth for Serializers)
    private val _activePlugins = MutableStateFlow<List<FeatureNavigationPlugin>>(emptyList())
    override val plugins: StateFlow<List<FeatureNavigationPlugin>> =
        _activePlugins.asStateFlow()

    // Explicit Navigation Item Container
    private val _navigationItems = MutableStateFlow<List<PluginTabItem>>(emptyList())
    override val rawNavigationItems: StateFlow<List<PluginTabItem>> = _navigationItems.asStateFlow()

    // Explicit Dashboard Widget Container
    private val _dashboardWidgets = MutableStateFlow<List<PluginDashboardWidget>>(emptyList())
    override val activeDashboardWidgets = _dashboardWidgets.asStateFlow()

    // Explicit Settings Contribution Container
    private val _Plugin_settingsContributions =
        MutableStateFlow<List<PluginSettingsContribution>>(emptyList())
    override val activePluginSettingsContributions = _Plugin_settingsContributions.asStateFlow()

    // Explicit Quick Actions Container
    private val _quickActions = MutableStateFlow<List<QuickAction>>(emptyList())
    override val activeQuickActions = _quickActions.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    override val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    /**
     * Aggregates all serializers from active modules as a StateFlow for reactive Nav3 configuration.
     */
    override val unifiedSerializersModuleFlow: StateFlow<SerializersModule> = _activePlugins
        .map { list ->
            list.map { it.serializersModule }
                .fold(baseSerializersModule) { acc, module -> acc + module }
        }
        .stateIn(
            scope,
            SharingStarted.Eagerly,
            baseSerializersModule
        )

    override val unifiedSerializersModule: SerializersModule
        get() = unifiedSerializersModuleFlow.value

    init {
        combine(
            manifest.coreFeatures,
            manifest.activeServices,
            manifest.operatingMode,
            capsManager.capabilities,
        ) { core, platforms, mode, caps ->
            Payload(core, platforms, mode, caps)
        }.onEach { payload ->
                val targetPlugins =
                    payload.core.map { it.navigationPlugin } + payload.platforms.map { it.navigationPlugin }
                val currentPlugins = _activePlugins.value

                currentPlugins.filter { cp -> targetPlugins.none { it.id == cp.id } }
                    .forEach { unregisterPlugin(it) }
                targetPlugins.filter { tp -> currentPlugins.none { it.id == tp.id } }
                    .forEach { registerPlugin(it) }

                // Recalculate components using both operating mode and active capabilities
                refreshNavigationComponents(payload.mode, payload.caps)
                _isInitialized.value = true
                Napier.i(tag = TAG) { "Global Navigation Coordinator Initialized (${targetPlugins.size} active plugins)" }
            }.launchIn(scope)
    }

    override fun registerPlugin(plugin: FeatureNavigationPlugin) {
        if (_activePlugins.value.any { it.id == plugin.id }) return
        _activePlugins.update { it + plugin }

        Napier.i(tag = TAG) { "Plugin Registered: ${plugin.id}" }
    }

    private fun unregisterPlugin(plugin: FeatureNavigationPlugin) {
        _activePlugins.update { current -> current.filter { it.id != plugin.id } }
        Napier.w(tag = TAG) { "Plugin Unregistered: ${plugin.id}" }
    }

    private fun refreshNavigationComponents(mode: AppOperatingMode, caps: Set<UserCapability>) {
        Napier.i(tag = TAG) { "Navigation plugins state changed, refreshing..."}
        val shadowedRoutes = _activePlugins.value.flatMap { it.overriddenTabs }.toSet()

        // 1. Refined Navigation Tab Filtering Pipeline
        _navigationItems.update {
            _activePlugins.value
                .asSequence()
                .flatMap { plugin ->
                    plugin.tabs.filter { tab ->
                        plugin.isVisible(route = tab.route, item = tab, mode = mode) &&
                                capsManager.hasAll(plugin.getRequiredCapabilities(tab.route))
                    }
                }
                .filterNot { it.route::class in shadowedRoutes }
                .distinctBy { it.route::class }
                .sortedBy { it.order }
                .toList()
        }

        val placementOrder = mapOf(
            DashboardPlacement.HERO to 0,
            DashboardPlacement.ANALYTICS_CAROUSEL to 1,
            DashboardPlacement.GRID to 2,
            DashboardPlacement.LIST to 3,
            DashboardPlacement.SIDEBAR to 4,
            DashboardPlacement.ACTIVITY_STREAM to 5,
            DashboardPlacement.ACTIVITY to 6
        )

        // Filter Dashboard Widgets (Mode-Aware via isVisible, sorted by placement type then order)
        _dashboardWidgets.update {
            _activePlugins.value
                .flatMap { plugin ->
                    plugin.dashboardWidgets.filter { widget ->
                        plugin.isVisible(item = widget, mode = mode)
                    }
                }
                .distinctBy { it.id }
                .sortedWith(
                    compareBy({ placementOrder[it.placement] ?: 99 }, { it.order })
                )
        }

        // Refined Settings Contribution Filtering Pipeline
        _Plugin_settingsContributions.update {
            _activePlugins.value
                .asSequence()
                .flatMap { plugin ->
                    plugin.pluginSettingsContributions.filter { setting ->
                        plugin.isVisible(item = setting, mode = mode) &&
                                capsManager.hasAll(setting.requiredCapabilities)
                    }
                }
                .filter { securedSetting -> caps.containsAll(securedSetting.requiredCapabilities) }
                .distinctBy { it.id }
                .sortedBy { it.order }
                .toList()
        }

        // Refined Quick Actions Filtering Pipeline
        _quickActions.update {
            _activePlugins.value
                .asSequence()
                .flatMap { plugin ->
                    plugin.quickActions.filter { action ->
                        plugin.isVisible(item = action, mode = mode) &&
                                capsManager.hasAll(action.requiredCapabilities)
                    }
                }
                .distinctBy { it.id }
                .sortedBy { it.order }
                .toList()
        }
    }

    /**
     * Logs the current state of the navigation stack for all top-level routes.
     */
    override fun debugBackstack(navigator: Navigator) {
        val state = navigator.state
        val topLevel = state.topLevelRoute
        val stack = state.backStacks[topLevel] ?: emptyList<NavKey>()

        Napier.d {
            """ 
            ┌─── BACKSTACK DEBUG ──────────────────────────────────────────
            │ Top Level: ${topLevel::class.simpleName} (ID: ${topLevel.hashCode()})
            │ Stack Size: ${stack.size}
            │ Items: ${stack.joinToString(" -> ") { it::class.simpleName ?: "null" }}
            └──────────────────────────────────────────────────────────────
            """.trimIndent()
        }
    }

    /**
     * Iterates only over currently licensed and active feature graphs!
     */
    override fun EntryProviderScope<NavKey>.assembleAllEntries(
        navigator: Navigator,
        isCompact: Boolean
    ) {
        debugBackstack(navigator)
        _activePlugins.value.forEach { plugin ->
            with(plugin) {
                registerEntries(navigator, isCompact)
            }
        }
    }

    private data class Payload(
        val core: Set<CoreFeature>,
        val platforms: Set<PlatformFeature>,
        val mode: AppOperatingMode,
        val caps: Set<UserCapability>
    )

    override fun resolveRoute(route: NavKey): NavKey {
        _activePlugins.value.forEach { plugin ->
            val resolved = plugin.resolveRoute(route)
            if (resolved != null) return resolved
        }
        return route
    }
}
