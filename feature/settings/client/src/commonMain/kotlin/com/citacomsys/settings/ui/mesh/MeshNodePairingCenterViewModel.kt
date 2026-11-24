package com.citacomsys.settings.ui.mesh

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citacomsys.core.mesh.model.SatelliteStatus
import com.citacomsys.core.mesh.server.EngineState
import com.citacomsys.core.mesh.service.MeshManager
import com.citacomsys.kernel.service.PlatformRuntimeStore
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MeshNodePairingCenterViewModel(
    private val meshManager: MeshManager,
    private val platformStore: PlatformRuntimeStore
) : ViewModel() {

    val hubState: StateFlow<EngineState> = meshManager.state
    val activeSatellites: StateFlow<Map<String, SatelliteStatus>> = meshManager.activeSatellites

    fun toggleEngine() {
        viewModelScope.launch {
            runCatching {
                if (hubState.value == EngineState.Running) {
                    meshManager.stopMesh()
                } else {
                    meshManager.startMesh()
                }
            }.onFailure {
                Napier.e(it) { "Failed to toggle Mother Hub server engine: ${it.message}" }
            }
        }
    }

    fun pokeNode(bindingId: String) {
        meshManager.monitorNode(bindingId)
    }

    fun unregisterNode(hardwareId: String) {
        meshManager.unregisterNode(hardwareId)
    }

    fun stopHub() {
        meshManager.stopMesh()
    }
}
