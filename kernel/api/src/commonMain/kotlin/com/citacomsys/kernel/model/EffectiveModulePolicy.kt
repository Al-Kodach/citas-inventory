package com.citacomsys.kernel.model

import com.citacomsys.types.ProductModule
import kotlinx.serialization.Serializable

@Serializable
enum class ModuleStatus {
    ACTIVE,
    SUSPENDED,
    DEGRADED,
    CRITICAL
}

@Serializable
data class EffectiveModulePolicy(
    val module: ProductModule,
    val status: ModuleStatus = ModuleStatus.ACTIVE,
    val reason: String? = null
)
