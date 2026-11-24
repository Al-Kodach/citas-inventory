package com.citacomsys.core.domain.service

import kotlinx.coroutines.flow.Flow

/**
 * A multiplatform port for checking the device's current network connectivity status.
 */
interface NetworkConnectivityService {
    val connectionStatusFlow: Flow<Boolean>

    suspend fun isOnline(): Boolean
}
