package com.citacomsys.core.barcode

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock

class WebBarcodeScannerService : BarcodeScannerService {
    override val isHardwareScanner: Boolean = false

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    override fun startScan(onResult: (ScannedBarcode) -> Unit, onError: (String) -> Unit) {
        _isScanning.value = true
    }

    override fun stopScan() {
        _isScanning.value = false
    }

    override fun openCamera() {

    }

    @Composable
    override fun ScannerScreen(onResult: (ScannedBarcode) -> Unit, onClose: () -> Unit) {
        var barcodeInput by remember { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        fun submitCode() {
            val code = barcodeInput.trim()
            if (code.isNotEmpty()) {
                onResult(
                    ScannedBarcode(
                        rawValue = code,
                        format = "WEB_INPUT",
                        timestamp = Clock.System.now().toEpochMilliseconds()
                    )
                )
                stopScan()
                onClose()
            }
        }

        Dialog(
            onDismissRequest = {
                stopScan()
                onClose()
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .width(400.dp)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Barcode Input",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        IconButton(
                            onClick = {
                                stopScan()
                                onClose()
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    OutlinedTextField(
                        value = barcodeInput,
                        onValueChange = { input ->
                            val cleanInput = input.replace("\n", "").replace("\r", "")
                            barcodeInput = cleanInput
                            if (input.contains("\n") || input.contains("\r")) {
                                submitCode()
                            }
                        },
                        label = { Text("Barcode Value") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )

                    Button(
                        onClick = { submitCode() },
                        enabled = barcodeInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Submit")
                    }
                }
            }
        }
    }
}

actual fun createBarcodeScannerService(): BarcodeScannerService = WebBarcodeScannerService()
