package com.citacomsys.core.mesh.server

import com.citacomsys.core.mesh.gateway.HubServiceGateway
import com.citacomsys.core.mesh.service.MeshManager
import com.citacomsys.kernel.service.PlatformRuntimeStore


actual fun createMeshServerEngine(
    gateways: List<HubServiceGateway>,
    platformStore: PlatformRuntimeStore,
    filesDir: String
): LocalMeshServerEngine {
    TODO("Not yet implemented")
}