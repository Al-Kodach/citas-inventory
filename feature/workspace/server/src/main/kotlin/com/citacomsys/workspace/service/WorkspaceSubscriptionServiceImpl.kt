package com.citacomsys.workspace.service

import com.citacomsys.api.billing.PayloadType
import com.citacomsys.api.workspace.model.*
import com.citacomsys.kernel.model.*
import com.citacomsys.api.platform.model.ProvisionedPackage
import com.citacomsys.api.workspace.repository.WorkspaceRepository
import com.citacomsys.api.workspace.repository.WorkspaceResourceUsageRepository
import com.citacomsys.api.workspace.service.WorkspaceSubscriptionService
import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.core.logging.logApplicationEvent
import com.citacomsys.shared.event.EventBus
import com.citacomsys.shared.event.SubscriptionEvent
import com.citacomsys.types.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

internal class WorkspaceSubscriptionServiceImpl(
    private val workspaceRepository: WorkspaceRepository,
    private val resourceUsageRepository: WorkspaceResourceUsageRepository,
    private val auditor: TelemetryAuditor
) : WorkspaceSubscriptionService {

    override suspend fun validateVersion(
        workspaceId: String,
        expectedVersion: Long
    ): WorkspaceSubscription {

        val current = workspaceRepository.getSubscription(workspaceId)

        if (current.subscriptionVersion != expectedVersion) {
            auditor.logApplicationEvent(
                action = "SUBSCRIPTION_VERSION_MISMATCH",
                entityType = EntityType.TENANT.name,
                entityId = workspaceId,
                summary = "Subscription version mismatch for workspace $workspaceId. Expected: $expectedVersion, Actual: ${current.subscriptionVersion}",
                level = TelemetryLevel.WARNING
            )
            throw IllegalArgumentException("Subscription changed after invoice generation. ExpectedVersion: $expectedVersion, currentVersion: ${current.subscriptionVersion}")
        }

        return current
    }

    override suspend fun getSubscriptionSummary(workspaceId: String): SubscriptionSummary {
        val subscription = workspaceRepository.getSubscription(workspaceId)

        val allocation = resourceUsageRepository.getWorkspaceResourceAllocation(workspaceId)
            ?: error("No resource allocation found for workspace")

        val usage = resourceUsageRepository.getResourceUsage(workspaceId)

        return SubscriptionSummary(
            packageName = allocation.name,
            tier = allocation.tier,
            billingCycle = subscription.billingCycle,
            recurringAmount = subscription.recurringAmount,
            status = subscription.status,
            startedAt = subscription.startedAt,
            expiresAt = subscription.endedAt,
            lineItems = subscription.lineItems,
            allocation = allocation,
            usage = usage
        )
    }

    override suspend fun activateTrial(
        workspaceId: String,
        provisioned: ProvisionedPackage
    ) {
        val now = Clock.System.now()
        val ws = workspaceRepository.get(workspaceId).getOrThrow()

        workspaceRepository.saveSubscription(
            workspaceId,
            WorkspaceSubscription(
                workspaceId = workspaceId,
                status = SubscriptionStatus.TRIAL,
                billingCycle = provisioned.billingCycle,
                recurringAmount = provisioned.calculatedPrice,
                lineItems = provisioned.lineItems,
                startedAt = now,
                endedAt = now.plus(provisioned.trialDays.days),
                updatedAt = now,
                subscriptionVersion = 1
            )
        )

        auditor.logApplicationEvent(
            action = "TRIAL_ACTIVATED",
            entityType = EntityType.TENANT.name,
            entityId = workspaceId,
            summary = "Trial activated for package ${provisioned.packageName}",
            level = TelemetryLevel.INFO
        )

        broadcast(workspaceId, ws.organizationId)
    }

    override suspend fun applySubscriptionChange(
        workspaceId: String,
        provisioned: ProvisionedPackage,
        type: PayloadType,
        expectedVersion: Long
    ) {
        val current = validateVersion(workspaceId, expectedVersion)
        val now = Clock.System.now()

        val currentExpiry = current.endedAt
        val baseTime = maxOf(now, currentExpiry)
        val newExpiry = baseTime + provisioned.billingCycle.duration()

        val newAllocation = WorkspaceResource(
            packageId = provisioned.packageId,
            name = provisioned.packageName,
            tier = provisioned.tier,
            modules = provisioned.modules,
            maxUsers = provisioned.maxUsers,
            maxDevice = provisioned.maxDevices,
            maxBranches = provisioned.maxBranches,
            maxProductsLimit = provisioned.maxProductsLimit,
            maxActiveBatchesLimit = provisioned.maxActiveBatchesLimit,
            historyRetentionMonths = provisioned.historyRetentionMonths,
            updatedAt = now
        )
        resourceUsageRepository.saveWorkspaceResourceAllocation(workspaceId, newAllocation)

        workspaceRepository.saveSubscription(
            workspaceId,
            current.copy(
                status = SubscriptionStatus.ACTIVE,
                billingCycle = provisioned.billingCycle,
                recurringAmount = provisioned.calculatedPrice,
                lineItems = provisioned.lineItems,
                endedAt = newExpiry,
                updatedAt = now,
                subscriptionVersion = current.subscriptionVersion + 1
            )
        )

        workspaceRepository.updateDeployMode(workspaceId, provisioned.deploymentMode)

        if (type == PayloadType.RENEWAL || (type == PayloadType.UPGRADE && current.status != SubscriptionStatus.TRIAL)) {
            resourceUsageRepository.resetSmsCycle(workspaceId)
        }

        auditor.logApplicationEvent(
            action = if (type == PayloadType.RENEWAL) "SUBSCRIPTION_RENEWED" else "SUBSCRIPTION_UPGRADED",
            entityType = EntityType.TENANT.name,
            entityId = workspaceId,
            summary = "Applied ${type.name} for ${provisioned.packageName}. New expiry: $newExpiry",
            level = TelemetryLevel.INFO
        )

        broadcast(workspaceId, orgId = "")
    }

    private suspend fun broadcast(workspaceId: String, orgId: String) {
        EventBus.publish(SubscriptionEvent(workspaceId, orgId))
    }
}
