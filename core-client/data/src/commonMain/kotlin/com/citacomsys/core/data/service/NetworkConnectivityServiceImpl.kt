package com.citacomsys.core.data.service

import com.citacomsys.core.domain.service.NetworkConnectivityService
import dev.jordond.connectivity.Connectivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

expect fun createConnectivity(): Connectivity

internal class NetworkConnectivityServiceImpl : NetworkConnectivityService {

    private val connectivityEngine = createConnectivity()

    init {
        connectivityEngine.start()
    }

    override val connectionStatusFlow: Flow<Boolean> =
        connectivityEngine.statusUpdates.map { it.isConnected }

    override suspend fun isOnline(): Boolean {
        return connectivityEngine.status().isConnected
    }
}