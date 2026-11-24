package com.citacomsys.client.support.module

import com.citacomsys.core.spi.PlatformFeature
import com.citacomsys.core.notification.InAppNotification
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.notification.CitasNotificationType
import com.citacomsys.core.spi.AppManifest
import com.citacomsys.kernel.service.PlatformClientService
import com.citacomsys.core.sync.engine.EntitySyncEngine
import com.citacomsys.kernel.model.*
import com.citacomsys.types.ProductModule
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.Koin

class ModulePolicyManager(
    private val runtimeService: PlatformClientService,
    private val notificationService: InAppNotificationService,
    private val productManifest: Map<ProductModule, PlatformFeature>,
    private val appManifest: AppManifest,
    private val koin: Koin,
    scope: CoroutineScope,
    private val syncEngine: EntitySyncEngine
) {
    private var previous: Map<ProductModule, EffectiveModulePolicy> = emptyMap()

    init {
        Napier.i { "Starting module policy manager..." }
        scope.launch {
            runtimeService.modulesMap.collect { current ->
                if (current.isEmpty()) return@collect
                Napier.d { "ServerPolicy changed: ${current.keys.joinToString("\n") { "$it: ${current[it]?.status}" }}" }
                processChanges(previous, current)
                previous = current
            }
        }
    }

    private fun activateModule(module: ProductModule) {
        val client = productManifest[module] ?: return
        // Activate DI and sync handlers
        client.activateServices(koin)
        appManifest.addService(client)
        // Sync handlers already registered
    }

    private fun deactivateModule(key: ProductModule) {
        val client = productManifest[key] ?: return

        // Teardown feature contexts
        appManifest.removeService(client)
        client.deactivate(koin)
        client.supportedEntities.forEach { syncEngine.unregister(it) }
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
        syncEngine.syncAll()
    }
}
