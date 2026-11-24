package com.citacomsys.api.organization.model

import com.citacomsys.types.BrandingConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Serializable
@SerialName("Organization")
data class Organization(
    val id: String,
    val name: String,
    val email: String?,
    val phone: String?,
    val businessCode: String,
    val ownerId: String?,
    val fleetDna: String,
    val status: OrgStatus = OrgStatus.PENDING_VERIFICATION,
    val brand: BrandingConfig? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)

enum class OrgStatus {
    ACTIVE,
    SUSPENDED,
    PENDING_VERIFICATION
}