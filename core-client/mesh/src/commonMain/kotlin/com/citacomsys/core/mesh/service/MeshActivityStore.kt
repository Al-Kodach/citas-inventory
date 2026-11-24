package com.citacomsys.core.mesh.service

import com.citacomsys.core.domain.file.PlatformFileSystem
import com.citacomsys.core.mesh.model.NodeActivity
import io.github.aakira.napier.Napier
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okio.Path

/**
 * Disk-backed lightweight buffer for storing and retrieving [NodeActivity] audit logs
 * without flooding device RAM on low-spec workstations.
 */
internal class MeshActivityStore(
    private val fileSystem: PlatformFileSystem
) {
    private val TAG = "MESH_ACTIVITY_STORE"
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    private val activityPath: Path by lazy {
        fileSystem.getInternalDir().resolve("mesh_activity_ledger.json")
    }

    suspend fun saveActivity(activity: NodeActivity) = mutex.withLock {
        runCatching {
            val current = loadActivitiesInternal()
            val updated = (listOf(activity) + current).take(200) // Keep latest 200 items on disk
            val text = json.encodeToString(updated)
            fileSystem.writeBytes(activityPath, text.encodeToByteArray())
            Napier.d(tag = TAG) { "Persisted activity log to disk: ${activity.action}" }
        }.onFailure { e ->
            Napier.e(tag = TAG, throwable = e) { "Failed to save mesh activity log to disk" }
        }
    }

    suspend fun getRecentActivities(limit: Int = 100): List<NodeActivity> = mutex.withLock {
        runCatching {
            loadActivitiesInternal().take(limit)
        }.getOrDefault(emptyList())
    }

    private suspend fun loadActivitiesInternal(): List<NodeActivity> {
        return if (fileSystem.exists(activityPath)) {
            runCatching {
                val text = fileSystem.readText(activityPath)
                json.decodeFromString<List<NodeActivity>>(text)
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
    }
}
