package com.citacomsys.core.barcode

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

interface BarcodeScannerService {
    val isHardwareScanner: Boolean
    val isScanning: StateFlow<Boolean>

    fun startScan(
        onResult: (ScannedBarcode) -> Unit,
        onError: (String) -> Unit
    )

    fun stopScan()

    fun openCamera()

    @Composable
    fun Scan(onResult: (ScannedBarcode) -> Unit, onClose: () -> Unit)
}


expect fun createBarcodeScannerService(): BarcodeScannerService
