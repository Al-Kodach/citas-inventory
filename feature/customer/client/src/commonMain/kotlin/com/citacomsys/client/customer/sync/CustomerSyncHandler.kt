package com.citacomsys.client.customer.sync

import com.citacomsys.core.domain.model.SyncTarget
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.sync.handler.EntitySyncHandler
import com.citacomsys.core.sync.handler.SingleReplayResult
import com.citacomsys.database.PendingRequestEntity
import com.citacomsys.module.api.customer.CustomerRegistrationRequest
import com.citacomsys.module.api.customer.CustomerService
import com.citacomsys.module.api.customer.dao.CustomerLocalDao
import com.citacomsys.shared.sync.SyncPriority
import com.citacomsys.types.EntityType
import io.github.aakira.napier.Napier
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

internal class CustomerSyncHandler(
    private val service: CustomerService,
    private val localDao: CustomerLocalDao,
    private val sessionManager: AppSessionManager
) : EntitySyncHandler {
    override val entityType = EntityType.CUSTOMER
    override val priority = SyncPriority.HIGHEST
    override val dependsOn = emptySet<EntityType>()

    private val pendingResult = mutableMapOf<String, PendingRequestEntity>()

    override suspend fun getPayload(request: PendingRequestEntity, json: Json): Any? {
        val customerId = request.entity_id

        pendingResult[request.id] = request

        return when (request.eventAction) {
            "CREATE_CUSTOMER" -> {
                val entity = localDao.findById(customerId)
                    ?: throw SingleReplayResult.Exception(SingleReplayResult.PermanentFailure("Customer data lost for $customerId"))

                CustomerRegistrationRequest(
                    fullName = entity.fullName,
                    workspaceId = request.workspace_id,
                    phone = entity.phone,
                    email = entity.email,
                    branchId = request.branch_id,
                    actorId = request.actor_id,
                    actorName = request.actor_name,
                    isConsented = entity.isAlertsConsented,
                )
            }
            "UPDATE_CONSENT" -> {
                request.payload
            }
            "DELETE_CUSTOMER" -> {
                null
            }
            else -> null
        }
    }

    override suspend fun onSuccess(requestId: String, response: HttpResponse) {
        val request = pendingResult[requestId] ?: return

        try {
            if (response.status.isSuccess()) {
                if (request.eventAction == "CREATE_CUSTOMER") {
                    localDao.deleteById(request.entity_id)
                }
                pendingResult.remove(requestId)
            }
        } catch (e: Exception) {
            Napier.e(e) { "Failed to process customerState sync response" }
        }
    }

    override suspend fun onSync(
        targets: List<SyncTarget>
    ): Result<Unit> = runCatching {
        targets.forEach { target ->
            if (sessionManager.isSubscribed(target.workspaceId)) {
                val maxUpdatedAt = localDao.maxUpdateAt(target.workspaceId, target.branchId)

                val result = service.getChangeList(
                    workspaceId = target.workspaceId,
                    branchId = target.branchId,
                    since = maxUpdatedAt
                )

                localDao.save(result.updated)

                result.deleted.forEach {
                    localDao.deleteById(it)
                }
            }
        }
    }
}
