package com.citacomsys.settings.ui.mesh

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citacomsys.core.mesh.model.MeshConnectionState
import com.citacomsys.core.mesh.model.MeshPeerInfo
import com.citacomsys.core.mesh.service.MeshManager
import com.citacomsys.kernel.service.PlatformRuntimeStore
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MeshDiscoveryViewModel(
    private val meshManager: MeshManager,
    private val platformStore: PlatformRuntimeStore
) : ViewModel() {

    val discoveredPeers: StateFlow<List<MeshPeerInfo>> = meshManager.discoveredPeers
    val connectionState: StateFlow<MeshConnectionState> = meshManager.connectionState
    val boundPeer: StateFlow<MeshPeerInfo?> = meshManager.boundPeer

    val isOrgConfigured: StateFlow<Boolean> = platformStore.organizationConfig
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun startDiscovery() {
        viewModelScope.launch {
            runCatching {
                meshManager.startDiscovery()
                Napier.i { "Started local LAN discovery for Mother Hubs" }
            }.onFailure {
                Napier.e(it) { "Failed to start local LAN discovery: ${it.message}" }
            }
        }
    }

    fun connectAndJoinHub(peer: MeshPeerInfo) {
        viewModelScope.launch {
            runCatching {
                meshManager.bindPeer(peer)
                Napier.i { "Connecting and joining Mother Hub ${peer.workstationName} at ${peer.ipAddress}:${peer.port}" }
            }.onFailure {
                Napier.e(it) { "Failed to connect to Mother Hub: ${it.message}" }
            }
        }
    }
}
