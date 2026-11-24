package com.citacomsys.core.notification

import kotlinx.serialization.Serializable

@Serializable
data class ExpiryAlertTarget(
    val organizationId: String,
    val productId: String,
    val productName: String,
    val batchId: String,
    val batchNumber: String?,
    val availableQuantity: Int,
    val expiryDate: String,
    val daysRemaining: Int
)