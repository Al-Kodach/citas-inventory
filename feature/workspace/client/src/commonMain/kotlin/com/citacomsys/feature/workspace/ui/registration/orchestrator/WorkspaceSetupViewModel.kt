package com.citacomsys.feature.workspace.ui.registration.orchestrator

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.viewModelScope
import com.citacomsys.api.platform.remote.ProductPackagePresentationService
import com.citacomsys.api.platform.uiModel.InvoicePreviewUiState
import com.citacomsys.api.workspace.model.WorkspaceProvisioningResult
import com.citacomsys.api.workspace.model.WorkspaceRegistrationRequest
import com.citacomsys.api.workspace.service.WorkspaceProvisioningApiService
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiError
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.core.util.toImageBitmap
import com.citacomsys.device.service.DeviceService
import com.citacomsys.device.service.PdfLineInfo
import com.citacomsys.device.service.ValueType
import com.citacomsys.feature.workspace.ui.registration.SubscriptionPackageReviewHandler
import com.citacomsys.kernel.service.PlatformClientService
import com.citacomsys.types.Address
import com.citacomsys.types.BrandingConfig
import com.citacomsys.types.PhoneNumber
import com.citacomsys.types.ProductType
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeWriteMode
import eu.anifantakis.lib.ksafe.asMutableStateFlow
import eu.anifantakis.lib.ksafe.getStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.time.Clock

enum class WorkspaceSetupStep {
    SPECS,
    PLAN_SELECTION,
    PACKAGE_REVIEW,
    BRANDING,
    REVIEW,
    ACTIVATING
}


@Serializable
data class WorkspaceSetupDraft(
    val organizationId: String = "",
    val name: String = "",
    val businessType: ProductType = ProductType.RETAIL,
    val street: String = "",
    val city: String = "",
    val mainBranchName: String = "Main Store",
    val email: String? = null,
    val phone: String? = null,
    val website: String? = null,
    val taxId: String? = null,
    val branding: BrandingConfig? = null
)

class WorkspaceSetupViewModel(
    private val wsApi: WorkspaceProvisioningApiService,
    private val presentationService: ProductPackagePresentationService,
    private val runtimeService: PlatformClientService,
    private val deviceService: DeviceService,
    notification: InAppNotificationService,
    private val prefs: KSafe
) : BaseViewModel(notification) {

    private val stepKey = "set_up_state"

    val draft by prefs.asMutableStateFlow(
        WorkspaceSetupDraft(),
        viewModelScope,
        "workspace_setup_draft",
        mode = KSafeWriteMode.Plain
    )

    val setUpState = prefs.getStateFlow(
        key = stepKey,
        defaultValue = WorkspaceSetupStep.SPECS,
        scope = viewModelScope
    )

    private val _invoicePreviewState =
        MutableStateFlow<UiResult<InvoicePreviewUiState>>(UiResult.Idle)
    val invoicePreviewState = _invoicePreviewState.asStateFlow()

    private val _previewImages =
        MutableStateFlow<UiResult<List<ImageBitmap?>>>(UiResult.Idle)
    val previewImages: StateFlow<UiResult<List<ImageBitmap?>>> = _previewImages.asStateFlow()

    val organizationConfig = runtimeService.organizationConfig

    val orgBranding = runtimeService.organizationConfig
        .map { it?.branding }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            null
        )

    val packageHandler = SubscriptionPackageReviewHandler(
        scope = viewModelScope,
        presentationService = presentationService,
        kSafe = prefs
    ) { _ -> }


    private val _activationResult =
        MutableStateFlow<UiResult<WorkspaceProvisioningResult>>(UiResult.Idle)
    val activationResult: StateFlow<UiResult<WorkspaceProvisioningResult>> =
        _activationResult.asStateFlow()

    init {
        viewModelScope.launch {
            setUpState.collect { step ->
                if (step == WorkspaceSetupStep.REVIEW) {
                    previewInvoice()
                } else if (step == WorkspaceSetupStep.ACTIVATING && _activationResult.value !is UiResult.Success) {
                    // Prevent blank screen if app was killed during activation step after provisioning completed
                    prefs.putDirect(stepKey, WorkspaceSetupStep.SPECS)
                }
            }
        }
    }

    fun previewInvoice() {
        viewModelScope.launch {
            _invoicePreviewState.value = UiResult.Loading
            val pkg = packageHandler.state.value.pkgProvisionRequest
            _invoicePreviewState.value = wsApi.previewInvoice(pkg).asUiResult()
        }
    }


    fun updateSpecs(state: WorkspaceSetupDraft) {
        draft.value = state
    }

    fun updateBranding(branding: BrandingConfig?) {
        draft.value = draft.value.copy(branding = branding)
    }

    fun navigateTo(step: WorkspaceSetupStep) {
        prefs.putDirect(stepKey, step)
    }

    private fun buildCertificatePdfContent(
        businessName: String,
        productKey: String,
        businessCode: String,
        resultWorkspaceId: String? = null
    ): List<PdfLineInfo> {
        val currentDraft = draft.value
        val pkgState = packageHandler.state.value
        val locationStr = listOfNotNull(
            currentDraft.street.takeIf { it.isNotBlank() },
            currentDraft.city.takeIf { it.isNotBlank() },
            "Ghana"
        ).joinToString(", ")

        return listOfNotNull(
            PdfLineInfo("========================================", ValueType.TEXT),
            PdfLineInfo("CITAS WORKSPACE ACTIVATION CERTIFICATE", ValueType.FONT_BOLD, 18),
            PdfLineInfo("========================================", ValueType.TEXT),
            PdfLineInfo("", ValueType.NEW_LINE),
            PdfLineInfo("BUSINESS CREDENTIALS", ValueType.FONT_BOLD, 14),
            PdfLineInfo("Business Name: $businessName", ValueType.TEXT),
            PdfLineInfo("Business Code: $businessCode", ValueType.TEXT),
            PdfLineInfo("Product Key: $productKey", ValueType.TEXT),
            resultWorkspaceId?.let { PdfLineInfo("Workspace ID: $it", ValueType.TEXT) },
            PdfLineInfo("", ValueType.NEW_LINE),
            PdfLineInfo("WORKSPACE SPECIFICATIONS", ValueType.FONT_BOLD, 14),
            PdfLineInfo("Business Type: ${currentDraft.businessType.name}", ValueType.TEXT),
            PdfLineInfo("Main Branch: ${currentDraft.mainBranchName.ifBlank { businessName }}", ValueType.TEXT),
            PdfLineInfo("Location: $locationStr", ValueType.TEXT),
            currentDraft.email?.let { PdfLineInfo("Email: $it", ValueType.TEXT) },
            currentDraft.phone?.let { PdfLineInfo("Phone: $it", ValueType.TEXT) },
            currentDraft.taxId?.takeIf { it.isNotBlank() }?.let { PdfLineInfo("Tax ID: $it", ValueType.TEXT) },
            PdfLineInfo("", ValueType.NEW_LINE),
            PdfLineInfo("SUBSCRIPTION & DEPLOYMENT", ValueType.FONT_BOLD, 14),
            pkgState.selectedPackage?.let { PdfLineInfo("Package Plan: ${it.name}", ValueType.TEXT) },
            pkgState.selectedBillingTerm?.let { PdfLineInfo("Deployment Mode: ${it.deploymentMode.name}", ValueType.TEXT) },
            pkgState.selectedBillingTerm?.let { PdfLineInfo("Billing Cycle: ${it.billingCycleName}", ValueType.TEXT) },
            PdfLineInfo("", ValueType.NEW_LINE),
            PdfLineInfo("SECURITY & KEY AWARENESS NOTICE", ValueType.FONT_BOLD, 12),
            PdfLineInfo("Keep this certificate and your Product Key strictly confidential.", ValueType.TEXT),
            PdfLineInfo("Generated: ${Clock.System.now()}", ValueType.TEXT),
            PdfLineInfo("========================================", ValueType.TEXT)
        )
    }

    fun loadPreview(
        businessName: String,
        productKey: String,
        businessCode: String,
        workspaceId: String? = null
    ) {
        viewModelScope.launch {
            _previewImages.value = UiResult.Loading
            try {
                val lines = buildCertificatePdfContent(businessName, productKey, businessCode, workspaceId)
                val pdfData = deviceService.generateContentPdf(lines)
                val imageByteArrayList = deviceService.renderPdfToImages(pdfData)
                val bitmaps = imageByteArrayList.map { it.toImageBitmap() }
                _previewImages.value = UiResult.Success(bitmaps)
            } catch (e: Exception) {
                _previewImages.value = e.asUiError()
            }
        }
    }

    fun savePdf(
        businessName: String,
        productKey: String,
        businessCode: String,
        workspaceId: String? = null
    ) {
        viewModelScope.launch {
            val lines = buildCertificatePdfContent(businessName, productKey, businessCode, workspaceId)
            val pdfData = deviceService.generateContentPdf(lines)
            val safeName = businessName.replace(" ", "_")
            deviceService.downloadFile(pdfData, "${safeName}_Activation_Certificate.pdf")
        }
    }

    fun printCertificate(
        businessName: String,
        productKey: String,
        businessCode: String,
        workspaceId: String? = null
    ) {
        viewModelScope.launch {
            val lines = buildCertificatePdfContent(businessName, productKey, businessCode, workspaceId)
            val pdfData = deviceService.generateContentPdf(lines)
            val safeName = businessName.replace(" ", "_")
            deviceService.print(pdfData, "${safeName}_Activation_Certificate.pdf")
        }
    }

    fun submitProvisioning() {
        viewModelScope.launch {
            _activationResult.value = UiResult.Loading

            val request = WorkspaceRegistrationRequest(
                name = draft.value.name,
                mainBranchName = draft.value.mainBranchName.ifBlank { draft.value.name },
                type = draft.value.businessType,
                email = draft.value.email,
                phone = draft.value.phone?.let { PhoneNumber.create(it) },
                address = Address(draft.value.street, draft.value.city, "Ghana"),
                branding = draft.value.branding,
                pkg = packageHandler.state.value.pkgProvisionRequest
            )

            wsApi.provisionWorkspace(request).fold(
                onSuccess = { result ->
                    prefs.putDirect(stepKey, WorkspaceSetupStep.ACTIVATING)
                    _activationResult.value = UiResult.Success(result)
                    runtimeService.refreshOrganizationConfig()
                    loadPreview(
                        draft.value.name,
                        result.productKey,
                        result.businessCode,
                        result.workspaceId
                    )
                },
                onFailure = { _activationResult.value = it.asUiError() }
            )
        }
    }


    fun completed() {
        _activationResult.value = UiResult.Idle
        _invoicePreviewState.value = UiResult.Idle
        _previewImages.value = UiResult.Idle
        prefs.putDirect(stepKey, WorkspaceSetupStep.SPECS)
        draft.value = draft.value.copy(
            name = "",
            businessType = ProductType.RETAIL,
            street = "",
            city = "",
            email = null,
            phone = null,
            branding = null
        )
    }
}
