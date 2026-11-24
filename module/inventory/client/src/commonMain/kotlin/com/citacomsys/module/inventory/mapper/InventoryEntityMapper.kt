package com.citacomsys.module.inventory.mapper

import com.citacomsys.core.util.toEpochMilliseconds
import com.citacomsys.database.StockBatchEntity
import com.citacomsys.database.StockMovementEntity
import com.citacomsys.types.ActorType
import com.citacomsys.types.ReferenceType
import com.citacomsys.types.StockBatch
import com.citacomsys.types.StockMovement
import com.citacomsys.types.StockMovementType
import com.citacomsys.types.Money
import kotlinx.datetime.LocalDate

fun StockBatch.toBatchEntity(): StockBatchEntity = StockBatchEntity(
    id = id,
    workspace_id = workspaceId,
    product_id = productId,
    initial_quantity = initialQuantity.toLong(),
    current_quantity = currentQuantity.toLong(),
    branch_id = branchId,
    expiry_date = expiryDate?.toString(),
    expiry_date_ms = expiryDate?.toEpochMilliseconds(),
    created_at = createdAt,
    updated_at = updatedAt,
    is_deleted = isDeleted,
    batch_number = batchNumber,
    cost_price_amount_minor = costPrice.amountMinor,
    cost_price_currency = costPrice.currencyCode,
    price_update_reason = costPriceChangeReason,
    selling_price_amount_minor = sellingPrice.amountMinor,
    selling_price_currency = sellingPrice.currencyCode,
    has_movements = hasMovements,
    actor_id = ""
)

fun StockBatchEntity.toBatch(): StockBatch = StockBatch(
    id = id,
    workspaceId = workspace_id,
    productId = product_id,
    batchNumber = batch_number,
    branchId = branch_id,
    expiryDate = expiry_date?.let {
        LocalDate.parse(it)
    },
    initialQuantity = initial_quantity.toInt(),
    currentQuantity = current_quantity.toInt(),
    costPrice = Money(
        cost_price_amount_minor,
        cost_price_currency
    ),
    sellingPrice = Money(
        selling_price_amount_minor,
        selling_price_currency
    ),
    createdAt = created_at,
    hasMovements = has_movements
)

fun StockMovement.asMovementEntity() = StockMovementEntity(
    id = id,
    workspace_id = workspaceId,
    product_id = productId,
    batch_id = batchId,
    batch_number = batchNumber,
    branch_id = branchId,
    quantity = quantity.toLong(),
    type = type.name,
    reason = reason,
    reference_id = referenceId,
    reference_type = referenceType?.name,
    actor_type = actorType.name,
    created_at = createdAt,
    is_accounted_for = true,
    actor_id = ""
)

fun StockMovementEntity.asDomain() = StockMovement(
    id = id,
    workspaceId = workspace_id,
    productId = product_id,
    batchId = batch_id,
    branchId = branch_id,
    quantity = quantity.toInt(),
    batchNumber = batch_number,
    type = StockMovementType.valueOf(type),
    reason = reason,
    referenceId = reference_id,
    referenceType = reference_type?.let { ReferenceType.valueOf(it) },
    actorType = actor_type.let { ActorType.valueOf(it) },
    createdAt = created_at
)