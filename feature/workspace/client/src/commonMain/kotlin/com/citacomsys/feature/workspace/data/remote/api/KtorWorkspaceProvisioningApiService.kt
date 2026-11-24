package com.citacomsys.feature.workspace.data.remote.api

import com.citacomsys.api.platform.model.PkgProvisionRequest
import com.citacomsys.api.platform.uiModel.InvoicePreviewUiState
import com.citacomsys.api.workspace.model.WorkspaceProvisioningResult
import com.citacomsys.api.workspace.model.WorkspaceRegistrationRequest
import com.citacomsys.api.workspace.service.WorkspaceProvisioningApiService
import com.citacomsys.core.remote.BaseApi
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody

internal class KtorWorkspaceProvisioningApiService(client: HttpClient) : BaseApi(client),
    WorkspaceProvisioningApiService {
    private val endpoint = "api/v1/workspace/provisioning"

    override suspend fun provisionWorkspace(request: WorkspaceRegistrationRequest): Result<WorkspaceProvisioningResult> =
        runCatching {
            executeRequest {
                client.post("$endpoint/save") {
                    setBody(request)
                }
            }
        }

    override suspend fun previewInvoice(pkg: PkgProvisionRequest): Result<InvoicePreviewUiState> {
        return runCatching {
            executeRequest {
                client.post("$endpoint/invoice-preview") {
                    setBody(pkg)
                }
            }
        }
    }
}
