package com.citacomsys.kernel.model

import com.citacomsys.types.ProductModule
import kotlinx.serialization.Serializable

@Serializable
data class WorkspaceProductModuleState(
    val module: ProductModule,
    val allowWrites: Boolean,
    val status: ModuleStatus
)

fun EffectiveModulePolicy.toRuntimeState(): WorkspaceProductModuleState {
    return WorkspaceProductModuleState(
        module = module,
        allowWrites = status == ModuleStatus.ACTIVE,
        status = status
    )
}
