package com.citacomsys.sales.sync

import com.citacomsys.api.sales.dto.*
import com.citacomsys.api.sales.PaymentMethod
import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.core.domain.model.SyncTarget
import com.citacomsys.core.sync.handler.EntitySyncHandler
import com.citacomsys.core.sync.handler.SingleReplayResult
import com.citacomsys.database.PendingRequestEntity
import com.citacomsys.types.EntityType
import com.citacomsys.types.Money
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json

internal class SalesSyncHandler(
    private val dbManager: InventoryDatabaseManager
) : EntitySyncHandler {

    override val entityType: EntityType = EntityType.SALE
    override val dependsOn: Set<EntityType> = setOf(EntityType.STOCK_BATCH, EntityType.CUSTOMER)

    override suspend fun getPayload(request: PendingRequestEntity, json: Json): Any? {
        if (request.eventAction != "CREATE") return null

        val saleId = request.entity_id

        return try {
            val db = dbManager.getDatabase()
            val saleEntity = db.saleQueries.select(saleId).executeAsOneOrNull()
                ?: throw SingleReplayResult.Exception(SingleReplayResult.PermanentFailure("Sale data lost for $saleId"))

            val items = db.saleQueries.selectItemsBySaleId(saleId).executeAsList().map {
                SaleItemRequest(
                    productId = it.product_id,
                    quantity = it.quantity.toInt(),
                    cachedUnitPrice = Money(it.sold_price_minor, it.sold_price_currency)
                )
            }

            val payments = db.saleQueries.selectPaymentsBySaleId(saleId).executeAsList().map {
                PaymentRequest(
                    amount = Money(it.amount_minor, it.currency_code),
                    method = PaymentMethod.valueOf(it.method),
                    reference = it.reference
                )
            }

            CreateSaleRequest(
                id = saleEntity.id,
                workspaceId = saleEntity.workspace_id,
                branchId = saleEntity.branch_id,
                customerId = saleEntity.customer_id,
                items = items,
                payments = payments,
                actorId = saleEntity.actor_id,
                actorName = request.actor_name,
                createdAt = saleEntity.created_at
            )

        } catch (e: Exception) {
            Napier.e(e) { "Failed to reconstruct sale push payload for $saleId" }
            throw e
        }
    }

    override suspend fun onSync(
        targets: List<SyncTarget>
    ): Result<Unit> {
        Napier.d { "Retail product sync triggered with target: $targets" }

        return Result.success(Unit)
    }
}
