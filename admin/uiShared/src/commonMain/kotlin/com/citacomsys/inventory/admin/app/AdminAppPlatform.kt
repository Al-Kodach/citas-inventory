package com.citacomsys.inventory.admin.app

import com.citacomsys.core.spi.AppManifest
import com.citacomsys.core.spi.CoreFeature
import com.citacomsys.core.spi.NavigationCoordinator
import com.citacomsys.core.spi.PlatformFeature
import com.citacomsys.core.spi.StartupRunLevel
import com.citacomsys.core.spi.StartupTask
import com.citacomsys.shared.event.AppLifecycleEvent
import com.citacomsys.shared.event.EventBus
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.Koin

private var koinRef: Koin? = null

object AdminAppPlatform {
    private const val TAG = "APP_PLATFORM"

    private val _lifecycleState =
        MutableStateFlow<AppLifecycleEvent>(AppLifecycleEvent.Bootstrapping)
    val lifecycleState = _lifecycleState.asStateFlow()

    fun bootstrap(koin: Koin) {
        koinRef = koin
        Napier.i(tag = TAG) { "Initializing Citas Platform Bootstrap Phase..." }

        emit(AppLifecycleEvent.Bootstrapping)
        val scope = koin.get<CoroutineScope>()

        scope.launch {
            try {
                // Initialize Orchestrator
                koin.get<AdminAppStageOrchestrator>()

                // Run Fundamental Bootstrap Tasks (Database migrations, Secret Vault setup)
                runTasks(StartupRunLevel.BOOTSTRAP)

                // Register Global Core Features (App Level)
                val appManifest = koin.get<AppManifest>()
                val coordinator = koin.get<NavigationCoordinator>()

                // Core Feature Registration (Onboarding, Auth, Shell)
                koin.getAll<CoreFeature>().forEach { feature ->
                    Napier.d(tag = TAG) { "Registering Core Feature: ${feature::class.simpleName}" }
                    feature.activate(koin)
                    appManifest.addFeature(feature)
                }

                koin.getAll<PlatformFeature>().forEach { feature ->
                    Napier.d(tag = TAG) { "Registering Platform Feature: ${feature::class.simpleName}" }
                    feature.activateServices(koin)
                    appManifest.addService(feature)
                }

                // Wait for coordinator signal
                coordinator.isInitialized.first { it }
                emit(AppLifecycleEvent.CoreReady)
                runTasks(StartupRunLevel.POST_ONBOARDING)
            } catch (e: Exception) {
                Napier.e(tag = TAG, throwable = e) { "CRITICAL: Platform Bootstrap failed." }
//                emit(AppLifecycleEvent.CoreReady)
            }
        }
    }

    private fun emit(event: AppLifecycleEvent) {
        _lifecycleState.value = event
        koinRef?.get<CoroutineScope>()?.launch {
            EventBus.publish(event)
        }
    }

    private suspend fun runTasks(level: StartupRunLevel) {
        val koin = koinRef ?: return
        koin.getAll<StartupTask>()
            .filter { it.runLevel == level }
            .sortedBy { it.order }
            .forEach { task ->
                try {
                    Napier.d(tag = TAG) { "Executing StartupTask [${task.runLevel}]: ${task.id}" }
                    task.run(koin)
                } catch (e: Exception) {
                    Napier.e(
                        tag = TAG,
                        throwable = e
                    ) { "StartupTask ${task.id} failed at runLevel $level" }
                }
            }
    }

}
