package com.citacomsys.core.spi

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Defines the functional mode of the application.
 * Tiered access: SALES < TECH < ADMIN.
 */
enum class AppOperatingMode(val level: Int) {
    ORG(0),  // organization mode
    WS(1),  // workspace mode
}

/**
 * SECURITY COMPLIANCE MODULE: Modular Feature Registry & Operating Modes
 * Framework Alignment: NIST SP 800-53 AC-2, SI-7
 */
interface AppManifest {
    val coreFeatures: StateFlow<Set<CoreFeature>>
    val activeServices: StateFlow<Set<PlatformFeature>>
    val operatingMode: StateFlow<AppOperatingMode>

    fun addFeature(feature: CoreFeature)
    fun removeFeature(feature: CoreFeature)

    fun addService(service: PlatformFeature)
    fun removeService(service: PlatformFeature)

    fun setOperatingMode(mode: AppOperatingMode)
}

class DynamicAppManifest : AppManifest {
    private val _coreFeatures = MutableStateFlow<Set<CoreFeature>>(emptySet())
    override val coreFeatures: StateFlow<Set<CoreFeature>> = _coreFeatures.asStateFlow()

    private val _activePlatformFeatures = MutableStateFlow<Set<PlatformFeature>>(emptySet())
    override val activeServices: StateFlow<Set<PlatformFeature>> = _activePlatformFeatures.asStateFlow()

    private val _operatingMode = MutableStateFlow(AppOperatingMode.ORG)
    override val operatingMode: StateFlow<AppOperatingMode> = _operatingMode.asStateFlow()

    override fun addFeature(feature: CoreFeature) {
        _coreFeatures.update { it + feature }
    }

    override fun removeFeature(feature: CoreFeature) {
        _coreFeatures.update { it - feature }
    }

    override fun addService(service: PlatformFeature) {
        _activePlatformFeatures.update { it + service }
    }

    override fun removeService(service: PlatformFeature) {
        _activePlatformFeatures.update { it - service }
    }

    override fun setOperatingMode(mode: AppOperatingMode) {
        _operatingMode.value = mode
    }
}
