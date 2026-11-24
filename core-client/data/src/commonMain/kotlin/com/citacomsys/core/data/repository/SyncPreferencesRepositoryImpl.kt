package com.citacomsys.core.data.repository

import com.citacomsys.core.domain.model.LastSyncMetadata
import com.citacomsys.core.domain.model.SyncConfig
import com.citacomsys.core.domain.repository.SyncPreferencesRepository
import com.citacomsys.types.NodeMeshMode
import eu.anifantakis.lib.ksafe.KSafe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal class SyncPreferencesRepositoryImpl(
    private val prefs: KSafe,
    private val workspaceIdProvider: () -> String?,
    private val scope: CoroutineScope
) : SyncPreferencesRepository {
    private val meshSyncKey = "mesh_sync_mode"
    private val hubEnabledKey = "is_mother_hub_enabled"
    private val syncConfigKey = "app_sync_config"
    private val syncMeta = "app_last_sync_metadata"
    private val lastSeqKey = "app_lastSeq"
    private val hubIpKey = "active_hub_ip"
    private val hubPortKey = "active_hub_port"
    private val hubOrgIdKey = "active_hub_org_id"
    private val hubOrgTokenKey = "active_hub_org_token"
    private val hubTlsFingerprintKey = "active_hub_tls_fingerprint"

    private fun getWorkspaceKey(baseKey: String): String {
        val wsId = workspaceIdProvider()
        return if (!wsId.isNullOrBlank()) "ws_${wsId}_$baseKey" else "global_$baseKey"
    }

    override val meshSyncMode: StateFlow<NodeMeshMode> = prefs.getFlow(meshSyncKey, NodeMeshMode.NODE.name)
        .map { NodeMeshMode.safeValueOf(it) }
        .stateIn(scope, SharingStarted.Eagerly, NodeMeshMode.NODE)

    override val isMotherHubEnabled: StateFlow<Boolean> = prefs.getFlow(hubEnabledKey, false)
        .stateIn(scope, SharingStarted.Eagerly, false)

    private val _syncConfig = MutableStateFlow(prefs.getDirect(syncConfigKey, SyncConfig()))
    override val syncConfig: StateFlow<SyncConfig> = _syncConfig.asStateFlow()

    override val activeHubIp: StateFlow<String?> = prefs.getFlow(hubIpKey, null)
        .stateIn(scope, SharingStarted.Eagerly, null)

    override val activeHubPort: StateFlow<Int> = prefs.getFlow(hubPortKey, 8443)
        .stateIn(scope, SharingStarted.Eagerly, 8443)

    override val activeHubOrgId: StateFlow<String?> = prefs.getFlow(hubOrgIdKey, null)
        .stateIn(scope, SharingStarted.Eagerly, null)

    override val activeHubOrgToken: StateFlow<String?> = prefs.getFlow(hubOrgTokenKey, null)
        .stateIn(scope, SharingStarted.Eagerly, null)

    override val activeHubTlsFingerprint: StateFlow<String?> =
        prefs.getFlow(hubTlsFingerprintKey, null)
            .stateIn(scope, SharingStarted.Eagerly, null)

    override fun saveLastSyncMetadata(metadata: LastSyncMetadata) {
        val key = getWorkspaceKey(syncMeta)
        prefs.putDirect(key, metadata)
        if (key != "global_$syncMeta") {
            prefs.putDirect("global_$syncMeta", metadata)
        }
    }

    override fun observeLastSyncMetadata(): Flow<LastSyncMetadata?> {
        val key = getWorkspaceKey(syncMeta)
        val globalKey = "global_$syncMeta"
        return combine(
            prefs.getFlow<LastSyncMetadata?>(key, null),
            prefs.getFlow<LastSyncMetadata?>(globalKey, null)
        ) { wsMeta, globalMeta ->
            wsMeta ?: globalMeta
        }
    }

    override fun setLastEventSeq(seq: Long) {
        prefs.putDirect(lastSeqKey, seq)
    }

    override fun getLastEventSeq(): Long {
        return prefs.getDirect(lastSeqKey, 0L)
    }

    override fun setMeshSyncMode(mode: NodeMeshMode) {
        prefs.putDirect(meshSyncKey, mode.name)
    }

    override fun setMotherHubEnabled(enabled: Boolean) {
        prefs.putDirect(hubEnabledKey, enabled)
    }

    override fun setSyncConfig(config: SyncConfig) {
        prefs.putDirect(syncConfigKey, config)
        _syncConfig.value = config
    }

    override fun setActiveHubInfo(
        ip: String?,
        port: Int,
        orgId: String?,
        token: String?,
        tlsFingerprint: String?
    ) {
        prefs.putDirect(hubIpKey, ip)
        prefs.putDirect(hubPortKey, port)
        prefs.putDirect(hubOrgIdKey, orgId)
        prefs.putDirect(hubOrgTokenKey, token)
        prefs.putDirect(hubTlsFingerprintKey, tlsFingerprint)
    }
}
