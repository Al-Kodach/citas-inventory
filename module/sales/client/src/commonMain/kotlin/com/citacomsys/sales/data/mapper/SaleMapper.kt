package com.citacomsys.sales.data.mapper

import com.citacomsys.database.PaymentEntity
import com.citacomsys.database.SaleEntity
import com.citacomsys.database.SaleItemEntity
import com.citacomsys.database.StockMovementEntity
import com.citacomsys.types.StockMovement
import com.citacomsys.api.sales.Payment
import com.citacomsys.api.sales.PaymentMethod
import com.citacomsys.api.sales.Sale
import com.citacomsys.api.sales.SaleDetails
import com.citacomsys.api.sales.SaleStatus
import com.citacomsys.types.Money
import com.citacomsys.types.generateUUID


fun SaleEntity.toDomain() = Sale(
    id = id,
    workspaceId = workspace_id,
    branchId = branch_id,
    customerId = customer_id,
    totalAmount = Money(total_amount_minor, total_amount_currency),
    totalItems = total_items.toInt(),
    status = SaleStatus.valueOf(status),
    priceVariance = Money(price_variance),
    createdAt = created_at,
    createdBy = actor_id
)

fun SaleItemEntity.toDomain() = SaleDetails.Item(
    productId = product_id,
    productName = product_name,
    quantity = quantity.toInt(),
    unitPrice = Money(sold_price_minor, sold_price_currency),
    batchId = batch_id,
    batchNumber = batch_number,
    expiryDate = null
)

fun StockMovement.asEntity() = StockMovementEntity(
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
    actor_id = "",
    actor_type = actorType.name,
    created_at = createdAt,
    is_accounted_for = true
)

fun PaymentEntity.toPayment(): Payment = Payment(
    id = id,
    saleId = sale_id,
    method = PaymentMethod.valueOf(method),
    amount = Money(amount_minor, currency_code),
    reference = reference,
    createdAt = created_at
)

fun Sale.asEntity() = SaleEntity(
    id = id,
    workspace_id = workspaceId,
    branch_id = branchId,
    customer_id = customerId,
    total_amount_minor = totalAmount.amountMinor,
    total_amount_currency = totalAmount.currencyCode,
    total_items = totalItems.toLong(),
    price_variance = 0,
    status = status.name,
    created_at = createdAt,
    actor_id = createdBy
)

fun List<Payment>.asPaymentsEntity() = map {
    PaymentEntity(
        id = it.id,
        sale_id = it.saleId,
        method = it.method.name,
        amount_minor = it.amount.amountMinor,
        currency_code = it.amount.currencyCode,
        reference = it.reference,
        created_at = it.createdAt
    )
}

fun List<SaleDetails.Item>.asItemsEntity(saleId: String) = map {
    SaleItemEntity(
        id = generateUUID(),
        sale_id = saleId,
        branch_id = "",
        product_id = it.productId,
        product_name = it.productName,
        batch_id = it.batchId,
        batch_number = it.batchNumber,
        quantity = it.quantity.toLong(),
        sold_price_minor = it.unitPrice.amountMinor,
        sold_price_currency = it.unitPrice.currencyCode,
        price_variance = 0
    )
}
