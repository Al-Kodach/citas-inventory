package com.citacomsys.api.workspace.model

import com.citacomsys.kernel.model.WorkspaceResource
import com.citacomsys.kernel.model.WorkspaceResourcesUsage
import com.citacomsys.types.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@SerialName("WorkspaceSubscription")
@Serializable
data class WorkspaceSubscription(
    val workspaceId: String,
    val status: SubscriptionStatus,
    val billingCycle: BillingCycle,
    val recurringAmount: Money,
    val lineItems: List<PriceLineItem> = emptyList(),
    val startedAt: Instant,
    val endedAt: Instant,
    val updatedAt: Instant,
    val subscriptionVersion: Long
)

@SerialName("SubscriptionSummary")
@Serializable
data class SubscriptionSummary(
    val packageName: String,
    val tier: PackageTier,
    val billingCycle: BillingCycle,
    val recurringAmount: Money,
    val status: SubscriptionStatus,
    val startedAt: Instant,
    val expiresAt: Instant,
    val lineItems: List<PriceLineItem>,
    val allocation: WorkspaceResource,
    val usage: WorkspaceResourcesUsage
)
