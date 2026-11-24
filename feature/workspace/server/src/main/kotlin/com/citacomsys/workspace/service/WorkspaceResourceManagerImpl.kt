package com.citacomsys.workspace.service

import com.citacomsys.kernel.model.*
import com.citacomsys.api.workspace.repository.WorkspaceRepository
import com.citacomsys.api.workspace.repository.WorkspaceResourceUsageRepository
import com.citacomsys.api.workspace.service.ResourceCheckResult
import com.citacomsys.api.workspace.service.ResourceCheckResult.*
import com.citacomsys.api.workspace.service.WorkspaceResourceManager
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.types.ResourceType
import com.citacomsys.types.SmsBillMode

internal class WorkspaceResourceManagerImpl(
    private val workspaceResourceRepository: WorkspaceResourceUsageRepository,
    private val workspaceRepository: WorkspaceRepository
) : WorkspaceResourceManager {

    override suspend fun get(workspaceId: String): WorkspaceResource {
        return workspaceResourceRepository.getWorkspaceResourceAllocation(workspaceId)
            ?: error("Workspace resource allocation not found")
    }

    override suspend fun getUsage(workspaceId: String): WorkspaceResourcesUsage {
        return workspaceResourceRepository.getResourceUsage(workspaceId)
    }

    override suspend fun checkAvailability(
        workspaceId: String,
        type: ResourceType,
        quantity: Int
    ): ResourceCheckResult = dbQuery {
        val usage = workspaceResourceRepository.getResourceUsage(workspaceId)
        val alloc = workspaceResourceRepository.getWorkspaceResourceAllocation(workspaceId)
            ?: return@dbQuery Depleted("PLAN", "No active plan found")

        when (type) {
            ResourceType.SMS -> {
                val account = workspaceRepository.getAccount(workspaceId)
                val hasQuota = false
//                val hasQuota = usage.smsUsageCount + quantity <= alloc.smsQuotaPerCycle
                val hasWallet = account.smsBalance >= quantity
                if (hasQuota || hasWallet) Available
                else Depleted("SMS", "Insufficient SMS quota or wallet balance")
            }

            ResourceType.USER -> {
                if (usage.userCount + quantity <= alloc.maxUsers) Available
                else Depleted("USERS", "User limit reached for current plan")
            }

            ResourceType.DEVICE -> {
                if (usage.deviceCount + quantity <= alloc.maxDevice) Available
                else Depleted("DEVICES", "Device limit reached")
            }
            
            ResourceType.PRODUCT -> {
                if (usage.productCountUsed + quantity <= alloc.maxProductsLimit) Available
                else Depleted("PRODUCTS", "Product limit reached")
            }

            ResourceType.BATCH -> {
                if (usage.activeBatchesCount + quantity <= alloc.maxActiveBatchesLimit) Available
                else Depleted("BATCHES", "Batch limit reached")
            }
            ResourceType.BRANCH -> {
                if (usage.branchCount + quantity <= alloc.maxBranches) Available
                else Depleted("BRANCHES", "Branch limit reached")
            }
            ResourceType.PACKAGE -> Available
        }
    }

    override suspend fun consume(workspaceId: String, type: ResourceType, quantity: Int) {
        val result = checkAvailability(workspaceId, type, quantity)
        if (result is Depleted) {
            throw IllegalStateException(result.message)
        }

        dbQuery {
            when (type) {
                ResourceType.USER -> workspaceResourceRepository.incrementUserCount(workspaceId)
                ResourceType.DEVICE -> workspaceResourceRepository.incrementDeviceCount(workspaceId)
                ResourceType.SMS -> consumeSms(workspaceId)
                ResourceType.PRODUCT -> workspaceResourceRepository.incrementProductCountLimit(workspaceId, quantity)
                ResourceType.BATCH -> workspaceResourceRepository.incrementActiveBatchesLimit(workspaceId, quantity)
                ResourceType.BRANCH -> workspaceResourceRepository.incrementBranchCount(workspaceId)
                else -> {}
            }
        }
    }

    override suspend fun consumeSms(workspaceId: String): SmsBillMode = dbQuery {
//        val usage = workspaceResourceRepository.getResourceUsage(workspaceId)
//        val alloc = workspaceResourceRepository.getWorkspaceResourceAllocation(workspaceId)!!
        val account = workspaceRepository.getAccount(workspaceId)

        when {
//            usage.smsUsageCount < alloc.smsQuotaPerCycle -> {
//                workspaceResourceRepository.incrementSmsUsageCount(workspaceId)
//                SmsBillMode.QUOTA
//            }

            account.smsBalance > 0 -> {
                workspaceRepository.decrementSmsBalance(workspaceId)
                SmsBillMode.WALLET
            }

            else -> throw IllegalStateException("SMS balance depleted during processing")
        }
    }

    override suspend fun release(
        workspaceId: String,
        type: ResourceType,
        quantity: Int
    ) = dbQuery {
        when (type) {
            ResourceType.USER -> workspaceResourceRepository.decrementUserCount(workspaceId)
            ResourceType.DEVICE -> workspaceResourceRepository.decrementDeviceCount(workspaceId)
            ResourceType.SMS -> {}
            ResourceType.PRODUCT -> workspaceResourceRepository.incrementProductCountLimit(workspaceId, -quantity)
            ResourceType.BATCH -> workspaceResourceRepository.incrementActiveBatchesLimit(workspaceId, -quantity)
            ResourceType.BRANCH -> workspaceResourceRepository.decrementBranchCount(workspaceId)
            ResourceType.PACKAGE -> {}
        }
    }
}
