package com.citacomsys.settings.ui.mesh

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citacomsys.core.domain.repository.SyncPreferencesRepository
import com.citacomsys.core.mesh.model.MeshConnectionState
import com.citacomsys.core.mesh.model.MeshPeerInfo
import com.citacomsys.core.mesh.model.NodeActivity
import com.citacomsys.core.mesh.model.SatelliteStatus
import com.citacomsys.core.mesh.server.EngineState
import com.citacomsys.core.mesh.service.MeshManager
import com.citacomsys.core.spi.AppManifest
import com.citacomsys.core.spi.AppOperatingMode
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MeshDiagnosticsViewModel(
    private val meshManager: MeshManager,
    private val pref: SyncPreferencesRepository,
    private val appManifest: AppManifest
) : ViewModel() {

    val hubState: StateFlow<EngineState> = meshManager.state
    val activeHub: StateFlow<MeshPeerInfo?> = meshManager.activeHub
    val discoveredPeers: StateFlow<List<MeshPeerInfo>> = meshManager.discoveredPeers
    val activeSatellites: StateFlow<Map<String, SatelliteStatus>> = meshManager.activeSatellites
    val connectionHistory: StateFlow<List<NodeActivity>> = meshManager.connectionHistory
    val connectionState: StateFlow<MeshConnectionState> = meshManager.connectionState
    val boundPeer: StateFlow<MeshPeerInfo?> = meshManager.boundPeer

    val isHub: StateFlow<Boolean> = combine(
        pref.isMotherHubEnabled,
        appManifest.operatingMode,
        hubState
    ) { isHubEnabled, mode, state ->
        isHubEnabled || mode == AppOperatingMode.ORG || state == EngineState.Running
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun startDiscovery() {
        meshManager.startDiscovery()
    }

    fun connectAndJoinHub(peer: MeshPeerInfo) {
        meshManager.bindPeer(peer)
    }

    fun startHubEngine() {
        viewModelScope.launch {
            runCatching {
                meshManager.startEngine()
                Napier.i { "Mother Hub server engine started via UI" }
            }.onFailure {
                Napier.e(it) { "Failed to start Mother Hub server engine: ${it.message}" }
            }
        }
    }

    fun stopHubEngine() {
        meshManager.stopMesh()
    }

    fun pokeNode(bindingId: String) {
        meshManager.monitorNode(bindingId)
    }

    fun unregisterNode(hardwareId: String) {
        meshManager.unregisterNode(hardwareId)
    }

    fun rediscoverHub() {
        meshManager.onHubUnreachable()
    }

    fun stopService() {
        meshManager.stopService()
    }
}
