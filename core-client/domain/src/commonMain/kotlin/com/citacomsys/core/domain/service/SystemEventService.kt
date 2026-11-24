package com.citacomsys.core.domain.service

import kotlinx.coroutines.flow.StateFlow


enum class SystemEventConnectionStatus {
    CONNECTED,
    DISCONNECTED,
    CONNECTING
}

interface SystemEventService {
    val connectionStatus: StateFlow<SystemEventConnectionStatus>
    suspend fun connect()
    suspend fun disconnect()
    suspend fun updateSubscription(topics: Set<String>, workspaceId: String?, branchId: String?)
}
