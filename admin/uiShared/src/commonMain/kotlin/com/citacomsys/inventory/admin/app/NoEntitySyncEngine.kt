package com.citacomsys.inventory.admin.app

import com.citacomsys.core.sync.SyncStatus
import com.citacomsys.core.sync.engine.EntitySyncEngine
import com.citacomsys.core.sync.handler.EntitySyncHandler
import com.citacomsys.shared.sync.SyncPriority
import com.citacomsys.shared.event.EntityChangeEvent
import com.citacomsys.types.EntityType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.Koin

internal class NoEntitySyncEngine: EntitySyncEngine {
    override val status: SharedFlow<SyncStatus> = MutableSharedFlow()
    override val isSyncing: StateFlow<Boolean> = MutableStateFlow(false)
    override val isUploading: StateFlow<Boolean> = MutableStateFlow(false)
    override val activeSyncCount: StateFlow<Int> = MutableStateFlow(0)

    override fun register(handler: EntitySyncHandler) {}

    override fun unregister(entityType: EntityType) {}

    override fun resetCircuit(entityType: EntityType) {
    }

    override suspend fun requestSync(event: EntityChangeEvent) {
    }

    override suspend fun requestSync(entityType: EntityType) {
    }

    override suspend fun syncAll() {
    }

    override fun getPriority(entityType: EntityType): SyncPriority {
        return SyncPriority.NORMAL
    }

    override val id: String
        get() = "NoEntitySyncEngine"

    override suspend fun run(koin: Koin) {

    }
}