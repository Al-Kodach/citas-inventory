package com.citacomsys.core.database.dao

import com.citacomsys.database.PendingRequestEntity
import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.types.EntityType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

enum class RequestMethod {
    GET, POST, DELETE, PUT
}

/**
 * Data Access Object for the generic offline request queue.
 * This provides a durable, crash-resilient queue for pending API calls.
 */
interface PendingRequestDao {
    fun observeAll(): Flow<List<PendingRequestEntity>>
    fun observeByType(entityType: EntityType): Flow<List<PendingRequestEntity>>

    suspend fun save(request: PendingRequestEntity)

    suspend fun insert(
        id: String,
        endpoint: String,
        method: RequestMethod,
        entityType: EntityType,
        entityId: String,
        workspaceId: String,
        branchId: String,
        routingChainJson: String,
        payload: String? = null,
        dependantId: String? = null,
        eventAction: String = "CREATE",
        actorId: String = "",
        actorName: String = ""
    )

    suspend fun deleteById(id: String)
    suspend fun getAndLockPendingRequestsOrdered(limit: Int): List<PendingRequestEntity>
    suspend fun moveToDeadLetterLog(id: String, reason: String)
    suspend fun unmarkAsSyncing(failures: Map<String, String?>)
    suspend fun getByType(entityType: EntityType): List<PendingRequestEntity>
    suspend fun getById(id: String): PendingRequestEntity?

    fun observeCount(): Flow<Long?>
    suspend fun getDependentRequests(failedEntityId: String): List<String>
    suspend fun moveBatchToDeadLetterLog(ids: List<String>, reason: String)
}

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
internal class SqlDelightPendingRequestDao(
    private val manager: InventoryDatabaseManager
) : PendingRequestDao {

    override fun observeAll(): Flow<List<PendingRequestEntity>> =
        manager.observesList { it.pendingRequestQueries.observeAll() }

    override fun observeByType(entityType: EntityType): Flow<List<PendingRequestEntity>> =
        manager.observesList { it.pendingRequestQueries.observeByType(entityType) }

    override suspend fun save(request: PendingRequestEntity) {
        val db = manager.getDatabase()
        db.transaction {
            db.pendingRequestQueries.insert(request)
        }
    }

    override suspend fun insert(
        id: String,
        endpoint: String,
        method: RequestMethod,
        entityType: EntityType,
        entityId: String,
        workspaceId: String,
        branchId: String,
        routingChainJson: String,
        payload: String?,
        dependantId: String?,
        eventAction: String,
        actorId: String,
        actorName: String
    ) {
        val db = manager.getDatabase()
        db.transaction {
            db.pendingRequestQueries.insertRequest(
                id = id,
                payload = payload,
                workspace_id = workspaceId,
                endpoint = endpoint,
                method = method,
                entityType = entityType,
                entity_id = entityId,
                branch_id = branchId,
                routing_chain_json = routingChainJson,
                dependant_id = dependantId,
                eventAction = eventAction,
                actor_id = actorId,
                actor_name = actorName,
                createdAt = Clock.System.now().toString()
            )
        }
    }

    override suspend fun deleteById(id: String) {
        val db = manager.getDatabase()
        db.transaction {
            db.pendingRequestQueries.deleteById(id)
        }
    }

    override suspend fun getAndLockPendingRequestsOrdered(
        limit: Int
    ): List<PendingRequestEntity> {
        val db = manager.getDatabase()
        return db.transactionWithResult {
            var requestsToProcess: List<PendingRequestEntity> = emptyList()
            db.pendingRequestQueries.transaction {
                requestsToProcess =
                    db.pendingRequestQueries.getPendingRequests(limit.toLong()).executeAsList()
                if (requestsToProcess.isNotEmpty()) {
                    db.pendingRequestQueries.markAsSyncing(requestsToProcess.map { it.id })
                }
            }
            requestsToProcess
        }
    }

    override suspend fun moveToDeadLetterLog(id: String, reason: String) {
        val db = manager.getDatabase()
        db.transaction {
            db.pendingRequestQueries.moveToDeadLetterLog(
                id = id,
                reason = reason,
                timestamp = Clock.System.now()
            )
        }
    }

    override suspend fun unmarkAsSyncing(failures: Map<String, String?>) {
        val db = manager.getDatabase()
        db.transaction {
            if (failures.isEmpty()) return@transaction

            db.pendingRequestQueries.transaction {
                failures.forEach { (id, reason) ->
                    db.pendingRequestQueries.unmarkAsSyncing(
                        id = id,
                        reason = reason ?: "Unknown sync error",
                        timestamp = Clock.System.now()
                    )
                }
            }
        }
    }

    override suspend fun getByType(entityType: EntityType): List<PendingRequestEntity> {
        val db = manager.getDatabase()
        return db.transactionWithResult {
            db.pendingRequestQueries.getByType(entityType).executeAsList()
        }
    }

    override suspend fun getById(id: String): PendingRequestEntity? {
        val db = manager.getDatabase()
        return db.transactionWithResult {
            db.pendingRequestQueries.getById(id).executeAsOneOrNull()
        }
    }

    override fun observeCount(): Flow<Long?> =
        manager.observeSingle { it.pendingRequestQueries.count() }

    override suspend fun getDependentRequests(failedEntityId: String): List<String> {
        val db = manager.getDatabase()
        return db.transactionWithResult {
            db.pendingRequestQueries.getDependentRequests(failedEntityId).executeAsList()
        }
    }

    override suspend fun moveBatchToDeadLetterLog(
        ids: List<String>,
        reason: String
    ) {
        val db = manager.getDatabase()
        db.transaction {
            db.pendingRequestQueries.moveBatchToDeadLetterLog(
                ids = ids,
                reason = reason,
                timestamp = Clock.System.now()
            )
        }
    }
}
