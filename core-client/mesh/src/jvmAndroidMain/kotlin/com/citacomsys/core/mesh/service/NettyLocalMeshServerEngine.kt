package com.citacomsys.core.mesh.service

import com.citacomsys.core.mesh.gateway.HubPlugin
import com.citacomsys.core.mesh.gateway.HubServiceGateway
import com.citacomsys.core.mesh.model.NodeActivity
import com.citacomsys.core.mesh.server.EngineState
import com.citacomsys.core.mesh.server.LocalMeshServerEngine
import com.citacomsys.kernel.service.PlatformRuntimeStore
import io.github.aakira.napier.Napier
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

internal class NettyLocalMeshServerEngine(
    private val gateways: List<HubServiceGateway>,
    platformStore: PlatformRuntimeStore,
    filesDir: String
) : LocalMeshServerEngine {

    private val orgId = platformStore.organizationId ?: "citas-default"
    private val tlsStore = MeshTlsIdentityStore(
        File(filesDir, "mesh-hub-$orgId-tls.p12"),
        orgId
    )

    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? =
        null

    private val _state = MutableStateFlow<EngineState>(EngineState.Idle)
    override val state: StateFlow<EngineState> get() = _state.asStateFlow()

    override var isRunning: Boolean = false
        private set

    override
    var tlsFingerprint: String? = null
        private set

    override fun start(
        port: Int,
        plugins: List<HubPlugin>,
        meshSignals: Flow<NodeActivity>,
        broadcastAlert: (NodeActivity) -> Unit
    ) {
        if (isRunning || server != null) {
            runCatching { stop() }
        }
        _state.value = EngineState.Starting

        try {
            val identity = tlsStore.loadOrCreate()
            tlsFingerprint = identity.fingerprintSha256

            server = embeddedServer(
                factory = Netty,
                configure = {
                    sslConnector(
                        keyStore = identity.keyStore,
                        keyAlias = identity.keyAlias,
                        keyStorePassword = { identity.password },
                        privateKeyPassword = { identity.password }
                    ) {
                        this.port = port
                        host = "0.0.0.0"
                    }
                },
                module = {
                    configureHubServer(
                        gateways = gateways,
                        meshSignals = meshSignals,
                        broadcastAlert = broadcastAlert,
                        plugins = plugins
                    )
                }
            ).start(wait = false)

            isRunning = true
            _state.value = EngineState.Running
            Napier.i(tag = "Mesh-Hub") {
                "Mother Hub TLS server listening on 0.0.0.0:$port (SHA-256 ${identity.fingerprintSha256}) [Org: $orgId]"
            }
        } catch (e: Exception) {
            _state.value = EngineState.Error(e)
            Napier.e(tag = "Mesh-Hub", throwable = e) { "Failed to start Mother Hub TLS server" }
        }
    }

    override fun stop() {
        server?.stop(1000, 2000)
        server = null
        isRunning = false
        tlsFingerprint = null
        _state.value = EngineState.Stopped
    }
}
