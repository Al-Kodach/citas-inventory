package com.citacomsys.module.inventory.request

import com.citacomsys.types.Money
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant

@Serializable
data class RestockRequest(
    val id: String,
    val workspaceId: String,
    val branchId: String,
    val productId: String,
    val batchNumber: String,
    val expiryDate: LocalDate?,
    val quantity: Int,
    val costPrice: Money,
    val sellingPrice: Money,
    val createdAt: Instant = Clock.System.now()
) {
    fun validate() {
        if (quantity <= 0) {
            error("Quantity must be greater than zero")
        }
        if (costPrice.amount <= 0 || costPrice.amount > sellingPrice.amount) {
            error("Cost basePrice must be greater than zero or less than selling basePrice")
        }
    }
}
