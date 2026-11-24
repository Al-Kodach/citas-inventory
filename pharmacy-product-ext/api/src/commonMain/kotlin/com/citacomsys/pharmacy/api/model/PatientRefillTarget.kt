package com.citacomsys.pharmacy.api.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class PatientRefillTarget(
    val organizationId: String,
    val saleId: String,
    val customerName: String,
    val customerPhone: String,
    val productName: String,
    val lastPurchaseDate: Instant
)