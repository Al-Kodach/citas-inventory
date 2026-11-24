package com.citacomsys.feature.platform.data.remote.api.ktor

import com.citacomsys.api.platform.model.PkgProvisionRequest
import com.citacomsys.api.platform.remote.ProductPackagePresentationService
import com.citacomsys.api.platform.uiModel.ProductPackageUiState
import com.citacomsys.api.platform.uiModel.InvoicePreviewUiState
import com.citacomsys.api.platform.uiModel.PreviewRequest
import com.citacomsys.api.platform.uiModel.SmsPackageUiState
import com.citacomsys.core.remote.BaseApi
import com.citacomsys.types.ResourcePriceBreakdown
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

internal class KtorProductPackagePresentationService(
    client: HttpClient,
    private val endpoint: String = "api/v1/packages/presentation"
) : ProductPackagePresentationService, BaseApi(client) {

    override suspend fun getPublicPackageUiStates(): List<ProductPackageUiState> {
        return executeRequest { client.get("$endpoint/public") }
    }

    override suspend fun getSmsPackages(): List<SmsPackageUiState> {
        return executeRequest { client.get("$endpoint/sms") }
    }

    override suspend fun calculateQuote(request: PkgProvisionRequest): ResourcePriceBreakdown {
        return executeRequest {
            client.post("$endpoint/quote") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    override suspend fun getInvoicePreview(request: PreviewRequest): InvoicePreviewUiState {
        return executeRequest {
            client.post("$endpoint/invoice/previewInvoice") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }
}
