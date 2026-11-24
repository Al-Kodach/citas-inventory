package com.citacomsys.api.platform.remote

import com.citacomsys.api.platform.model.PkgProvisionRequest
import com.citacomsys.api.platform.uiModel.InvoicePreviewUiState
import com.citacomsys.api.platform.uiModel.PreviewRequest
import com.citacomsys.api.platform.uiModel.ProductPackageUiState
import com.citacomsys.api.platform.uiModel.SmsPackageUiState
import com.citacomsys.types.ResourcePriceBreakdown

interface ProductPackagePresentationService {
    suspend fun getPublicPackageUiStates(): List<ProductPackageUiState>
    suspend fun getSmsPackages(): List<SmsPackageUiState>
    suspend fun calculateQuote(request: PkgProvisionRequest): ResourcePriceBreakdown
    suspend fun getInvoicePreview(request: PreviewRequest): InvoicePreviewUiState
}