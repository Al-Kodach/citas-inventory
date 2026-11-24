package com.citacomsys.api.workspace.service

import com.citacomsys.api.workspace.model.SubscriptionSummary
import com.citacomsys.api.workspace.model.WorkspaceSubscription
import com.citacomsys.types.BrandingConfig

interface WorkspaceApiService {

    /**
     * Retrieves the complete subscription and billing breakdown.
     */
    suspend fun getSubscription(): Result<WorkspaceSubscription>
    suspend fun getSubscriptionSummary(): SubscriptionSummary

    /**
     * Updates the workspace's public identity (Name, Email, etc).
     */
    suspend fun updateProfile(name: String, email: String?, phone: String?): Result<Unit>

    /**
     * Updates the visual branding specifically for this business unit.
     * The workspace or organization branding
     */
    suspend fun updateBranding(config: BrandingConfig?): Result<Unit>
}
