package com.citacomsys.app.shared.util

import com.citacomsys.core.notification.InAppNotification
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.notification.CitasNotificationType
import com.citacomsys.core.spi.AppManifest
import com.citacomsys.core.spi.ProductRegistry
import com.citacomsys.core.sync.engine.EntitySyncEngine
import com.citacomsys.kernel.model.EffectiveModulePolicy
import com.citacomsys.kernel.model.ModuleStatus
import com.citacomsys.kernel.service.PlatformClientService
import com.citacomsys.types.ProductModule
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.Koin

class WorkspaceModulePolicyManager(
    private val runtimeService: PlatformClientService,
    private val notificationService: InAppNotificationService,
    private val manifest: AppManifest,
    private val koin: Koin,
    scope: CoroutineScope,
    private val syncEngine: EntitySyncEngine
) {
    private var previous: Map<ProductModule, EffectiveModulePolicy> = emptyMap()

    init {
        Napier.i { "Starting module policy manager..." }
        scope.launch {
            runtimeService.modulesMap.collect { current ->
                if (current.isEmpty()) {
                    Napier.i { "Runtime module provision is empty" }
                    return@collect
                }
                Napier.d { "ServerPolicy changed: ${current.keys.joinToString("\n") { "$it: ${current[it]?.status}" }}" }
                processChanges(previous, current)
                previous = current
            }
        }
    }

    private suspend fun processChanges(
        old: Map<ProductModule, EffectiveModulePolicy>,
        new: Map<ProductModule, EffectiveModulePolicy>
    ) {
        new.forEach { (key, module) ->

            val oldModule = old[key]

            if (oldModule == null || oldModule.status != module.status) {

                when (module.status) {

                    ModuleStatus.SUSPENDED -> {
                        Napier.i { "Deactivate module: $key" }
                        deactivateModule(key)
                        notificationService.notify(
                            InAppNotification(
                                message = module.reason ?: "$key unavailable",
                                type = CitasNotificationType.ERROR
                            )
                        )
                    }

                    ModuleStatus.CRITICAL,
                    ModuleStatus.DEGRADED -> {
                        Napier.i { "Critical module: $key" }
                        notificationService.notify(
                            InAppNotification(
                                message = module.reason ?: "$key degraded",
                                type = CitasNotificationType.WARNING
                            )
                        )
                    }

                    ModuleStatus.ACTIVE -> {
                        Napier.i { "Activate module: $key" }
                        activateModule(key)
                    }
                }
            }
        }
        koin.get<EntitySyncEngine>().syncAll()
    }

    private fun activateModule(module: ProductModule) {
        val appType = runtimeService.activeWorkspaceConfig.value?.type ?: return
        val productPlugin = ProductRegistry.get(appType) ?: return
        val client = productPlugin.manifest[module] ?: return
        client.activateServices(koin)

        // Just update the manifest, the coordinator will reactively addService the plugin
        manifest.addService(client)
    }

    private fun deactivateModule(key: ProductModule) {
        val appType = runtimeService.activeWorkspaceConfig.value?.type ?: return
        val productPlugin = ProductRegistry.get(appType) ?: return
        val client = productPlugin.manifest[key] ?: return

        //  Teardown feature contexts
        client.deactivate(koin)
        client.supportedEntities.forEach { syncEngine.unregister(it) }

        // Update the manifest to drop the module from UI
        manifest.removeService(client)
    }
}