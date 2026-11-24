package com.citacomsys.core.mesh.service

import com.citacomsys.core.mesh.model.MeshPeerInfo
import kotlinx.coroutines.flow.Flow

interface DiscoveryService {
    fun startDiscovery(organizationId: String): Flow<List<MeshPeerInfo>>
    fun stopDiscovery()
    fun broadcastPresence(info: MeshPeerInfo)
}
