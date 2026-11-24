package com.citacomsys.dev

import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.citacomsys.kernel.model.*
import com.citacomsys.types.*
import kotlin.time.Clock

class ConfigureWorkspaceContext(
    private val platformStore: PlatformRuntimeStore
) {

    suspend fun activate() {
        platformStore.updateWorkspaceConfig(
            WorkspaceRuntimeConfig(
                workspaceId = "workspace1",
                name = "Dev App",
                type = ProductType.PHARMACY,
                status = WorkspaceStatus.ACTIVE,
                resource = WorkspaceResource.EMPTY,
                usage = WorkspaceResourcesUsage.EMPTY,
                subscriptionStatus = SubscriptionStatus.ACTIVE,
                branding = null
            )
        )
        platformStore.updateWorkspaceState(
            WorkspaceRuntimeState(
                workspaceId = "workspace1",
                workspaceStatus = WorkspaceStatus.ACTIVE,
                deviceStatus = DeviceStatus.ACTIVE,
                enforcement = WorkspaceEnforcementPolicy.ALWAYS_ALLOW,
                runtimeFlags = RuntimeFlags(),
                productKey = "product_key",
                configVersion = 1,
                serverTime = Clock.System.now()
            )
        )
    }

    suspend fun deactivate() = platformStore.clearAll()
}