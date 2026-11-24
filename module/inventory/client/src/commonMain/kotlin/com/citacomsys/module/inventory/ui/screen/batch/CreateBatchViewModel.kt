package com.citacomsys.module.inventory.ui.screen.batch

import androidx.lifecycle.viewModelScope
import com.citacomsys.core.domain.security.UserCapabilityManager
import com.citacomsys.core.domain.security.WorkspaceResourceQuotaGuard
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.module.inventory.data.repository.BaseClientProductRepository
import com.citacomsys.module.inventory.domain.repository.ClientStockBatchRepository
import com.citacomsys.module.inventory.request.RestockRequest
import com.citacomsys.module.inventory.ui.screen.BatchGenerator
import com.citacomsys.module.inventory.ui.screen.PharmacyExpiryRules
import com.citacomsys.module.inventory.validation.StockBatchValidator
import com.citacomsys.types.BaseProduct
import com.citacomsys.types.Money
import com.citacomsys.types.ProductType
import com.citacomsys.types.ResourceType
import com.citacomsys.types.StockBatch
import com.citacomsys.types.UserCapability
import com.citacomsys.types.generateUUID
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/**
 * ViewModel for managing the restock form state and logic.
 */
class CreateBatchViewModel(
    notification: InAppNotificationService,
    private val sessionManager: AppSessionManager,
    private val quotaGuard: WorkspaceResourceQuotaGuard,
    private val capabilityManager: UserCapabilityManager,
    private val repository: ClientStockBatchRepository,
    private val productRepository: BaseClientProductRepository<BaseProduct>
) : BaseViewModel(notification) {

    private val _batchState = MutableStateFlow<UiResult<StockBatch>>(UiResult.Idle)
    val batchState = _batchState.asStateFlow()

    private var writeBranchId: String? = sessionManager.writeTargetBranchId.value
    private var canOverrideBranchId = true

    private var isEditMode: Boolean = false

    private val _productState = MutableStateFlow<UiResult<BaseProduct>>(UiResult.Loading)
    val productState = _productState.asStateFlow()

    private val _saveState = MutableStateFlow<UiResult<Unit>>(UiResult.Idle)
    val saveState = _saveState.asStateFlow()

    private val _isBatchNumberEditable = MutableStateFlow(true)
    val isBatchNumberEditable = _isBatchNumberEditable.asStateFlow()

    private val _validationErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val validationErrors = _validationErrors.asStateFlow()


    // --- Form State ---
    val batchNumber = MutableStateFlow("")
    val quantity = MutableStateFlow("")
    val costPrice = MutableStateFlow("0")
    val sellingPrice = MutableStateFlow("0")
    val expiryDate = MutableStateFlow<LocalDate?>(null)


    fun loadProduct(productId: String) {
        viewModelScope.launch {
            _productState.value = UiResult.Loading
            val result = productRepository.getById(productId)
            _productState.value = result.asUiResult()

            result.onSuccess { product ->
                // CRITICAL GATE: Absolute safety lock for editing historical entries
                if (isEditMode) return@onSuccess

                // Safe to auto-generate for a brand new restock entry
                if (batchNumber.value.isBlank()) {
                    batchNumber.value =
                        BatchGenerator.generateSystemBatch(product.category ?: "GEN")
                }

                if (expiryDate.value == null) {
                    expiryDate.value =
                        PharmacyExpiryRules.calculateDefaultExpiry(product.category ?: "")
                }
            }
        }
    }


    /**
     * Loads an existing batch or initializes form with defaults for a new restock.
     */
    fun loadBatch(batchId: String?) {
        if (batchId == null) {
            isEditMode = false
            canOverrideBranchId = true
            clearForm()
            return
        }

        isEditMode = true
        viewModelScope.launch {
            _batchState.value = UiResult.Loading
            val batch = repository.findById(batchId)
            if (batch != null) {
                _batchState.value = UiResult.Success(batch)
                // Historical values remain completely locked
                batchNumber.value = batch.batchNumber
                quantity.value = batch.initialQuantity.toString()
                costPrice.value = batch.costPrice.amountMinor.toString()
                sellingPrice.value = batch.sellingPrice.amountMinor.toString()
                expiryDate.value = batch.expiryDate
                _isBatchNumberEditable.value = !batch.hasMovements
                writeBranchId = batch.branchId
                canOverrideBranchId = false
            } else {
                _batchState.value = UiResult.Error("Batch not found")
            }
        }
    }

    fun setTargetBranch(id: String?) {
        if (canOverrideBranchId) {
            writeBranchId = id ?: sessionManager.writeTargetBranchId.value
        }
    }


    /**
     * Executes the restock or metadata update.
     */
    fun save(
        productId: String,
        existingBatchId: String?,
        originApp: ProductType,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (!capabilityManager.has(UserCapability.MANAGE_INVENTORY)) {
                    error("You don't have permission")
                }

                val targetBranchId = writeBranchId ?: sessionManager.resolveWriteBranchId()

                quotaGuard.verifyBatchCreationAllowed()

                val errors = StockBatchValidator.validateRestock(
                    originApp = originApp,
                    batchNumber = batchNumber.value,
                    quantity = quantity.value.toIntOrNull() ?: 0,
                    costPriceMinor = costPrice.value.toLongOrNull() ?: 0L,
                    sellingPriceMinor = sellingPrice.value.toLongOrNull() ?: 0L,
                    expiryDate = expiryDate.value
                )

                if (errors.isNotEmpty()) {
                    _validationErrors.value = errors
                    showError("Please fix form errors before continuing.")
                    return@launch
                }

                _validationErrors.value = emptyMap()
                _saveState.value = UiResult.Loading


                val request = RestockRequest(
                    id = existingBatchId ?: generateUUID(),
                    workspaceId = sessionManager.activeWorkspaceId!!,
                    branchId = targetBranchId,
                    productId = productId,
                    batchNumber = batchNumber.value,
                    expiryDate = expiryDate.value,
                    quantity = quantity.value.toIntOrNull() ?: 0,
                    costPrice = Money(amountMinor = costPrice.value.toLongOrNull() ?: 0L),
                    sellingPrice = Money(amountMinor = sellingPrice.value.toLongOrNull() ?: 0L)
                )

                Napier.d { "Stock validation passed, executing restocking..." }

                val user = sessionManager.appUser.value!!
                val actorId = user.id
                val actorName = user.name

                val isNew = existingBatchId.isNullOrEmpty()
                val result = repository.restock(request, actorId, actorName)

                result.onSuccess {
                    if (isNew) {
                        quotaGuard.consumeLocalResource(
                            ResourceType.BATCH,
                            workspaceId = request.workspaceId
                        )
                    }
                    showSuccess("Inventory updated")
                    onSuccess()
                    _saveState.value = UiResult.Idle
                }.onFailure {
                    _saveState.value = UiResult.Error(it.message ?: "Failed to save")
                    showError(it.message)
                }
            } catch (e: Exception) {
                val error = e.message ?: "Unknow error"
                showError(error)
            }
        }
    }

    private fun clearForm() {
        batchNumber.value = ""
        quantity.value = ""
        costPrice.value = "0"
        sellingPrice.value = "0"
        expiryDate.value = null
        writeBranchId = sessionManager.writeTargetBranchId.value
        canOverrideBranchId = true
    }

    fun clearState() {
        _batchState.value = UiResult.Idle
        _saveState.value = UiResult.Idle
    }
}