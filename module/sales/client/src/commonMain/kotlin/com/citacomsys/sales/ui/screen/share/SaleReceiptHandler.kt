package com.citacomsys.sales.ui.screen.share

import androidx.compose.ui.graphics.ImageBitmap
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiError
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.core.ui.result.getOrNull
import com.citacomsys.device.service.PdfLineInfo
import com.citacomsys.api.sales.SaleDetails
import com.citacomsys.sales.data.repository.ClientSalesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SmsReceiptDialogState(
    val phone: String = "",
    val state: UiResult<Unit> = UiResult.Idle,
    val smsTopAppRequired: Boolean = false
)

data class SaleReceiptState(
    val details: UiResult<SaleDetails> = UiResult.Idle,
    val smsDialog: SmsReceiptDialogState = SmsReceiptDialogState(),
    val pdfPreview: UiResult<List<ImageBitmap>> = UiResult.Idle,
    val operationState: UiResult<Unit> = UiResult.Idle,
    val pdfLines: List<PdfLineInfo> = emptyList()
)

class SaleReceiptHandler(
    private val saleUtil: SaleReceiptUtil,
    private val saleRepository: ClientSalesRepository,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(SaleReceiptState())
    val state = _state.asStateFlow()

    fun loadDetails(
        saleId: String,
        wsId: String,
        onSuccess: suspend () -> Unit = {}
    ) {
        scope.launch {
            _state.update { it.copy(details = UiResult.Loading) }
            val result = saleRepository.getSaleDetails(saleId, wsId).asUiResult()
            _state.update { it.copy(details = result) }
            if (result is UiResult.Success) {
                onSuccess()
            }
        }
    }

    fun generatePdfPreview(staffName: String) {
        val details = _state.value.details.getOrNull() ?: return
        scope.launch {
            _state.update { it.copy(pdfPreview = UiResult.Loading) }
            runCatching {
                val pdfContent = saleUtil.getReceiptPdfContent(details, staffName)
                _state.update { it.copy(pdfLines = pdfContent) }
                val pdfData = saleUtil.generatePdf(pdfContent)
                saleUtil.getReceiptImages(pdfData)
            }.onSuccess { images ->
                _state.update { it.copy(pdfPreview = UiResult.Success(images)) }
            }.onFailure { e ->
                _state.update { it.copy(pdfPreview = e.asUiError()) }
            }
        }
    }

    fun onPhoneChange(phone: String) {
        _state.update { it.copy(smsDialog = it.smsDialog.copy(phone = phone)) }
    }

    fun sendSms(
        saleId: String,
        wsId: String,
        onSuccess: () -> Unit = {}
    ) {
        scope.launch {
            val phone = _state.value.smsDialog.phone
            _state.update { it.copy(smsDialog = it.smsDialog.copy(state = UiResult.Loading)) }

            val result = runCatching { saleUtil.sendDigitalReceipt(phone, saleId, wsId) }
            _state.update { it.copy(smsDialog = it.smsDialog.copy(state = result.asUiResult())) }

            if (result.isSuccess) {
                onSuccess()
            } else {
                val message = result.exceptionOrNull()?.message ?: "Unknown error"
                if (message.contains("Insufficient SMS credits")) {
                    _state.update {
                        it.copy(
                            smsDialog = it.smsDialog.copy(smsTopAppRequired = true)
                        )
                    }
                }
            }
        }
    }

    fun print(saleId: String) = executeOperation { data -> saleUtil.print(data, saleId) }

    fun printReceiptWithPrinterModule(staffName: String) {
        val details = _state.value.details.getOrNull() ?: return
        scope.launch {
            _state.update { it.copy(operationState = UiResult.Loading) }
            val result = runCatching { saleUtil.printReceiptWithJob(details, staffName).getOrThrow() }.asUiResult()
            _state.update { it.copy(operationState = result) }
        }
    }

    fun savePdf(saleId: String) = executeOperation { data -> saleUtil.savePdf(data, saleId) }

    fun sharePdf(saleId: String) = executeOperation { data -> saleUtil.sharePdf(data, saleId) }

    private fun executeOperation(block: suspend (ByteArray) -> Unit) {
        val lines = _state.value.pdfLines.ifEmpty { return }
        scope.launch {
            _state.update { it.copy(operationState = UiResult.Loading) }
            val result = runCatching {
                val pdfData = saleUtil.generatePdf(lines)
                block(pdfData)
            }.asUiResult()
            _state.update { it.copy(operationState = result) }
        }
    }

    fun resetOperationState() {
        _state.update { it.copy(operationState = UiResult.Idle) }
    }
}