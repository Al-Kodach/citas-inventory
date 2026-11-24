package com.citacomsys.core.domain.sync

import com.citacomsys.types.BatchSyncItem
import com.citacomsys.types.DispatchOutcome
import com.citacomsys.types.SyncMetadata

interface SyncEntityDispatcher {
    suspend fun dispatch(item: BatchSyncItem, metadata: SyncMetadata): DispatchOutcome
}
