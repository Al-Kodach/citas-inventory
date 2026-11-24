package com.citacomsys.core.sync.engine

import com.citacomsys.core.sync.handler.EntitySyncHandler
import com.citacomsys.core.domain.service.DataSyncTrigger
import com.citacomsys.core.spi.StartupTask
import com.citacomsys.core.sync.SyncStatus
import com.citacomsys.shared.event.EntityChangeEvent
import com.citacomsys.shared.sync.SyncPriority
import com.citacomsys.types.EntityType
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface EntitySyncEngine : DataSyncTrigger, StartupTask {
    val status: SharedFlow<SyncStatus>

    val isSyncing: StateFlow<Boolean>
    val isUploading: StateFlow<Boolean>
    val activeSyncCount: StateFlow<Int>

    fun register(handler: EntitySyncHandler)

    fun unregister(entityType: EntityType)

    fun resetCircuit(entityType: EntityType)

    suspend fun requestSync(event: EntityChangeEvent)
    suspend fun requestSync(entityType: EntityType)

    override suspend fun syncAll()
    fun getPriority(entityType: EntityType): SyncPriority
}
