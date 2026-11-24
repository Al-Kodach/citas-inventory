package com.citacomsys.api.organization.model

import com.citacomsys.types.Money
import kotlinx.serialization.Serializable

@Serializable
data class OrganizationReportSummary(
    val organizationId: String,
    val totalWorkspacesCount: Long,
    val activeUsersCount: Long,
    val totalSalesCount: Long,
    val totalRevenue: Money,
    val stockValuation: Money,
    val expiringBatchesCount: Long
)
