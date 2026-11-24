package com.citacomsys.api.workspace.service

import com.citacomsys.api.platform.model.PkgProvisionRequest
import com.citacomsys.api.platform.uiModel.InvoicePreviewUiState
import com.citacomsys.api.workspace.model.WorkspaceProvisioningResult
import com.citacomsys.api.workspace.model.WorkspaceRegistrationRequest

interface WorkspaceProvisioningService {
    /**
     * Spins up a new business platform instance under an organization.
     */
    suspend fun provisionWorkspace(
        organizationId: String,
        request: WorkspaceRegistrationRequest,
        actorId: String
    ): WorkspaceProvisioningResult

    suspend fun previewInvoice(pkg: PkgProvisionRequest): InvoicePreviewUiState
}
