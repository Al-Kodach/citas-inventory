package com.citacomsys.api.workspace.service

import com.citacomsys.api.billing.PayloadType
import com.citacomsys.api.platform.model.ProvisionedPackage
import com.citacomsys.api.workspace.model.WorkspaceSubscription
import com.citacomsys.api.workspace.model.SubscriptionSummary

interface WorkspaceSubscriptionService {

    suspend fun getSubscriptionSummary(workspaceId: String): SubscriptionSummary

    suspend fun activateTrial(
        workspaceId: String,
        provisioned: ProvisionedPackage
    )

    suspend fun applySubscriptionChange(
        workspaceId: String,
        provisioned: ProvisionedPackage,
        type: PayloadType,
        expectedVersion: Long
    )

    suspend fun validateVersion(
        workspaceId: String,
        expectedVersion: Long
    ): WorkspaceSubscription
}
