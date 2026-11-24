package com.citacomsys.core.domain.repository

import com.citacomsys.core.domain.model.LastSyncMetadata
import com.citacomsys.core.domain.model.SyncConfig
import com.citacomsys.types.NodeMeshMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SyncPreferencesRepository {
    val meshSyncMode: StateFlow<NodeMeshMode>
    val isMotherHubEnabled: StateFlow<Boolean>
    val syncConfig: StateFlow<SyncConfig>

    val activeHubIp: StateFlow<String?>
    val activeHubPort: StateFlow<Int>
    val activeHubOrgId: StateFlow<String?>
    val activeHubOrgToken: StateFlow<String?>
    val activeHubTlsFingerprint: StateFlow<String?>

    fun saveLastSyncMetadata(metadata: LastSyncMetadata)
    fun observeLastSyncMetadata(): Flow<LastSyncMetadata?>
    fun setLastEventSeq(seq: Long)
    fun getLastEventSeq(): Long

    fun setMeshSyncMode(mode: NodeMeshMode)
    fun setMotherHubEnabled(enabled: Boolean)
    fun setSyncConfig(config: SyncConfig)

    fun setActiveHubInfo(
        ip: String?,
        port: Int,
        orgId: String?,
        token: String?,
        tlsFingerprint: String? = null
    )
}
