package com.citacomsys.core.mesh.server

import com.citacomsys.core.mesh.gateway.HubPlugin
import com.citacomsys.core.mesh.gateway.HubServiceGateway
import com.citacomsys.core.mesh.model.NodeActivity
import com.citacomsys.kernel.service.PlatformRuntimeStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

sealed class EngineState {
    object Idle : EngineState()
    object Starting : EngineState()
    object Running : EngineState()
    object Stopped : EngineState()
    data class Error(val cause: Throwable) : EngineState()
}

/**
 * Orchestrator for the Mother Hub's local gateway server.
 */
interface LocalMeshServerEngine {
    fun start(
        port: Int,
        plugins: List<HubPlugin>,
        meshSignals: Flow<NodeActivity>,
        broadcastAlert: (NodeActivity) -> Unit
    )

    fun stop()
    val isRunning: Boolean
    val state: StateFlow<EngineState>
    val tlsFingerprint: String?
}

/**
 * Platform-specific factory for the Hub's Ktor engine.
 */
expect fun createMeshServerEngine(
    gateways: List<HubServiceGateway>,
    platformStore: PlatformRuntimeStore,
    filesDir: String
): LocalMeshServerEngine
