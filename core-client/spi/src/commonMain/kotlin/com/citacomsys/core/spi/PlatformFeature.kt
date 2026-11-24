package com.citacomsys.core.spi

import com.citacomsys.types.UserCapability
import com.citacomsys.types.EntityType
import com.citacomsys.types.ProductModule
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.Koin

/**
 * Core features (Auth, Backup, Profile) that always stay active once a workspace is initialized.
 */
interface CoreFeature {
    val id: String
    val navigationPlugin: FeatureNavigationPlugin

    // Activate background service
    fun activate(koin: Koin)

    // De-Activate background service
    fun deactivateService(koin: Koin) {}
}

/**
 * Licensed Server-driven modules (Sales, Inventory) that activateServices dynamically.
 */
interface PlatformFeature {
    val module: ProductModule? get() = null
    val navigationPlugin: FeatureNavigationPlugin
    val supportedEntities: Set<EntityType> get() = emptySet()
    val requiredUserCapabilities: Set<UserCapability> get() = emptySet()

    fun injectUiComponents(koin: Koin) {}
    fun activateServices(koin: Koin)
    fun deactivate(koin: Koin)
}

enum class StartupRunLevel {
    BOOTSTRAP,          // Infrastructure Layer
    POST_ONBOARDING,    // Environment Layer
    WORKSTATION,
    WORKSPACE_READY     // Data Layer
}

/**
 * Tasks that must run during the platform startup sequence.
 */
interface StartupTask {
    val id: String
    val runLevel: StartupRunLevel get() = StartupRunLevel.BOOTSTRAP
    val order: Int get() = 100
    val isCritical: Boolean get() = false
    suspend fun run(koin: Koin)
}