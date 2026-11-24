package com.citacomsys.api.workspace.service

import com.citacomsys.api.platform.model.PkgProvisionRequest
import com.citacomsys.api.platform.uiModel.InvoicePreviewUiState
import com.citacomsys.api.workspace.model.WorkspaceProvisioningResult
import com.citacomsys.api.workspace.model.WorkspaceRegistrationRequest

interface WorkspaceProvisioningApiService {
    /**
     * Activates a new business platform for an existing organization.
     */
    suspend fun provisionWorkspace(request: WorkspaceRegistrationRequest): Result<WorkspaceProvisioningResult>
    suspend fun previewInvoice(pkg: PkgProvisionRequest): Result<InvoicePreviewUiState>
  }
