package com.citacomsys.kernel.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class ServerProductPolicy(
    val workspaceId: String,
    val overallStatus: ModuleStatus = ModuleStatus.ACTIVE,
    val version: Long,
    val modules: List<EffectiveModulePolicy> = emptyList(),
    val evaluatedAt: Instant
)
