package com.citacomsys.core.mesh.server

import com.citacomsys.core.mesh.gateway.HubServiceGateway
import com.citacomsys.core.mesh.service.NettyLocalMeshServerEngine
import com.citacomsys.kernel.service.PlatformRuntimeStore

actual fun createMeshServerEngine(
    gateways: List<HubServiceGateway>,
    platformStore: PlatformRuntimeStore,
    filesDir: String
): LocalMeshServerEngine = NettyLocalMeshServerEngine(
    gateways = gateways,
    platformStore = platformStore,
    filesDir = filesDir
)
