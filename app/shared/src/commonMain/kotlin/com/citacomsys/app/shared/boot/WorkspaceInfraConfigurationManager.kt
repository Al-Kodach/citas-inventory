package com.citacomsys.app.shared.boot

import com.citacomsys.core.spi.AppManifest
import com.citacomsys.core.spi.PlatformWorkManagerEngine
import com.citacomsys.core.spi.ProductRegistry
import com.citacomsys.core.spi.StartupRunLevel
import com.citacomsys.core.spi.StartupTask
import com.citacomsys.shared.event.AppLifecycleEvent
import com.citacomsys.shared.event.EventBus
import com.citacomsys.types.ProductType
import eu.anifantakis.lib.ksafe.KSafe
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.Koin
import kotlin.time.Duration.Companion.milliseconds

class WorkspaceInfraConfigurationManager(
    private val prefs: KSafe,
    private val workManagerEngine: PlatformWorkManagerEngine,
    private val scope: CoroutineScope,
    private val manifest: AppManifest
) : StartupTask {
    override val id: String = "workspace.infra.config"
    override val runLevel: StartupRunLevel = StartupRunLevel.WORKSTATION
    override val order: Int = 10000
    private val TAG = "WORKSPACE_INFRA_M"
    private var koinRef: Koin? = null
    private var currentInitializedAppType: ProductType? = null
    private var pendingWsConfigurationEvent: AppLifecycleEvent.WorkspaceSelected? = null

    private var lastAccessWorkspace: AppLifecycleEvent.WorkspaceSelected?
        get() = prefs.getDirect("LAST_VIEW", null)
        set(value) = prefs.putDirect("LAST_VIEW", value)

    override suspend fun run(koin: Koin) {
        koinRef = koin

        scope.launch {
            EventBus.subscribe<AppLifecycleEvent.WorkspaceSelected>().collect {
                Napier.d(tag = TAG, message = "workspace selection event received for ${it.workspaceId}")
                pendingWsConfigurationEvent = it
                initializeProductApp(it)
            }
        }

        scope.launch {
            EventBus.subscribe<AppLifecycleEvent.ExistWorkspace>().collect {
                Napier.d(tag = TAG, message = "Workspace Exit received -> Deactivating current product plugin & workspace workers")
                pendingWsConfigurationEvent?.workspaceId?.let { wsId ->
                    workManagerEngine.cancelWorkspaceWorkers(wsId)
                }
                lastAccessWorkspace = null
                pendingWsConfigurationEvent = null
                val activeType = currentInitializedAppType
                currentInitializedAppType = null
                if (activeType != null) {
                    koinRef?.let { koin ->
                        runCatching {
                            ProductRegistry.get(activeType)?.onDeactivate(koin)
                        }
                    }
                }
            }
        }

        scope.launch {
            EventBus.subscribe<AppLifecycleEvent.WorkspaceConfigCompleted>().collect {
                Napier.d(tag = TAG, message = "Workspace configuration complete event received")
                lastAccessWorkspace = pendingWsConfigurationEvent
                pendingWsConfigurationEvent?.workspaceId?.let { wsId ->
                    Napier.i(tag = TAG, message = "Activating workspace background workers for workspace $wsId")
                    workManagerEngine.activateWorkspaceWorkers(wsId)
                }
            }
        }

        lastAccessWorkspace?.let {
            Napier.d(tag = TAG, message = "Restoring last accessed workspace: ${it.workspaceId}")
            EventBus.publish(it)
        }
    }

    private fun initializeProductApp(event: AppLifecycleEvent.WorkspaceSelected) {
        val type = event.productType

        if (currentInitializedAppType == type && currentInitializedAppType != null) {
            Napier.i(tag = TAG) { "Same product app type $type. Skipping module reload, triggering workspace refresh task." }
            emit(AppLifecycleEvent.WorkspaceInfraReady)
            return
        }

        val koin = koinRef ?: return

        Napier.i(tag = TAG) { "Activating ProductType Context: $type" }

        scope.launch {
            delay(150.milliseconds)
            emit(AppLifecycleEvent.WorkspaceInfraConfiguration)
            delay(150.milliseconds)

            try {
                if (currentInitializedAppType != null && currentInitializedAppType != type) {
                    Napier.i(tag = TAG) { "Deactivating previous ProductType plugin: $currentInitializedAppType" }
                    ProductRegistry.get(currentInitializedAppType!!)?.onDeactivate(koin)
                }

                val plugin = ProductRegistry.get(type)
                    ?: error("No plugin found for product app: $type. Ensure industry plugin is registered.")

                // Special features for this product app(E.g. Patient service, etc.) Note: not sales, pos
                val services = plugin.manifest

                if (services.isEmpty()) {
                    Napier.i(tag = TAG) { "No services found for $type"}
                } else {
                    Napier.i(tag = TAG) { "Registering services for $type..."}

                    services.values.forEach { feature ->
                        Napier.d(tag = TAG) {"Plugin-feature-module: ${feature.module}"}
                        feature.injectUiComponents(koin)
                        manifest.addService(feature)
                        feature.activateServices(koin)
                    }
                }

                plugin.onActivate(koin)
                currentInitializedAppType = type
                Napier.i(tag = TAG) { "Workspace infra configuration completed for $type." }
                emit(AppLifecycleEvent.WorkspaceInfraReady)
            } catch (e: Exception) {
                Napier.e(tag = TAG, throwable = e) { "Failed to initialize ProductType: $type" }
                emit(AppLifecycleEvent.WorkspaceInfraReady)
            }
        }
    }

    private fun emit(event: AppLifecycleEvent) {
        AppPlatform.emit(event)
    }
}
