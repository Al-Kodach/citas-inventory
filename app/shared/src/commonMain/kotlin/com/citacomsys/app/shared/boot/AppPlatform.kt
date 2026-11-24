package com.citacomsys.app.shared.boot

import com.citacomsys.app.shared.app.AppStageOrchestrator
import com.citacomsys.client.customer.data.CustomerDatabaseManager
import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.core.spi.AppManifest
import com.citacomsys.core.spi.CoreFeature
import com.citacomsys.core.spi.NavigationCoordinator
import com.citacomsys.core.spi.PlatformFeature
import com.citacomsys.core.spi.ProductRegistry
import com.citacomsys.core.spi.StartupRunLevel
import com.citacomsys.core.spi.StartupTask
import com.citacomsys.core.sync.engine.EntitySyncEngine
import com.citacomsys.core.sync.handler.EntitySyncHandler
import com.citacomsys.customer.database.CustomerDatabase
import com.citacomsys.feature.api.telemetry.PlatformAppUpdater
import com.citacomsys.home.sync.BusinessDashboardSyncHandler
import com.citacomsys.shared.event.AppLifecycleEvent
import com.citacomsys.shared.event.EventBus
import com.citacomsys.types.ProductType
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.Koin
import kotlin.time.Duration.Companion.milliseconds

/**
 * Orchestrator for the Citas Platform lifecycle core.
 * Focuses strictly on infrastructure bootstrap, telemetry sync loops, and ProductType feature activation.
 */
object AppPlatform {
    private const val TAG = "APP_PLATFORM"

    private val _lifecycleState =
        MutableStateFlow<AppLifecycleEvent>(AppLifecycleEvent.Bootstrapping)
    val lifecycleState = _lifecycleState.asStateFlow()

    private var koinRef: Koin? = null

    fun bootstrap(koin: Koin) {
        koinRef = koin
        Napier.i(tag = TAG) { "Initializing Citas Platform Bootstrap Phase..." }

        emit(AppLifecycleEvent.Bootstrapping)
        val scope = koin.get<CoroutineScope>()

        scope.launch {
            try {
                // Run Fundamental Bootstrap Tasks (Database migrations, Secret Vault setup)
                runTasks(StartupRunLevel.BOOTSTRAP)

                // Ensuring Core Database foundation is established
                Napier.d(tag = TAG) { "Initializing Core Database foundation..." }
                koin.get<InventoryDatabaseManager>().getDatabase()
                koin.get<CustomerDatabaseManager>().getDatabase()

                // Register Global Core Features (App Level)
                val appManifest = koin.get<AppManifest>()
                val coordinator = koin.get<NavigationCoordinator>()

                // Core Feature Registration (Onboarding, Auth, Shell)
                koin.getAll<CoreFeature>().forEach { feature ->
                    Napier.d(tag = TAG) { "Registering Core Feature: ${feature::class.simpleName}" }
                    feature.activate(koin)
                    appManifest.addFeature(feature)
                }

                // Register Platform Features (Sales, Inventory, Pos)
                val platformServices = koin.getAll<PlatformFeature>()
                platformServices.forEach {
                    it.injectUiComponents(koin)
                    it.activateServices(koin)
                    appManifest.addService(it)
                }

                // Wait for coordinator signal
                coordinator.isInitialized.first { it }

                // Register Sync handlers
                val engine = koin.get<EntitySyncEngine>()
                val syncHandlers = koin.getAll<EntitySyncHandler>()
                syncHandlers.forEach { handler ->
                    Napier.d(tag = TAG) { "Registering ${handler::class.simpleName}..." }
                    engine.register(handler)
                }

                emit(AppLifecycleEvent.CoreReady)
                Napier.i(tag = TAG) { "Stage 0: Core infrastructure bootstrap completed." }

                // Stage 1: Check for System/Platform Updates
                runSystemUpdatePhase()
            } catch (e: Exception) {
                Napier.e(tag = TAG, throwable = e) { "CRITICAL: Platform Bootstrap failed." }
                emit(AppLifecycleEvent.CoreReady)
            }
        }
    }

    suspend fun runSystemUpdatePhase() {
        val koin = koinRef ?: return
        runCatching {
            val appUpdater = koin.getOrNull<PlatformAppUpdater>()
            if (appUpdater != null) {
                Napier.i(tag = TAG) { "Stage 1: Checking for critical platform updates..." }
                appUpdater.checkForUpdates()
            }
        }.onFailure { e ->
            Napier.w(tag = TAG) { "Platform update check failed: ${e.message}" }
        }
    }

    suspend fun triggerPostOnboardingPhase(): Result<Unit> = runCatching {
        Napier.i(tag = TAG) { "Stage 3: Triggering POST_ONBOARDING environment tasks..." }
        runTasks(StartupRunLevel.POST_ONBOARDING)
        emit(AppLifecycleEvent.PostOnboarding)
    }

    suspend fun triggerWorkstationPhase(): Result<Unit> = runCatching {
        Napier.i(tag = TAG) { "Stage 4: Triggering WORKSTATION hardware tasks..." }
        runTasks(StartupRunLevel.WORKSTATION)
        emit(AppLifecycleEvent.WorkstationReady)
    }

    fun emit(event: AppLifecycleEvent) {
        _lifecycleState.value = event
        koinRef?.get<CoroutineScope>()?.launch {
            EventBus.publish(event)
        }
    }

    private suspend fun runTasks(level: StartupRunLevel) {
        val koin = koinRef ?: return
        koin.getAll<StartupTask>()
            .filter {
                it.runLevel == level
            }
            .sortedBy { it.order }
            .forEach { task ->
                try {
                    Napier.d(tag = TAG) { "Executing StartupTask [${task.runLevel}]: ${task.id} (Critical=${task.isCritical})" }
                    task.run(koin)
                } catch (e: Exception) {
                    Napier.e(
                        tag = TAG,
                        throwable = e
                    ) { "StartupTask ${task.id} failed at runLevel $level (Critical=${task.isCritical})" }
                    if (task.isCritical) {
                        throw e
                    }
                }
            }

    }
}
