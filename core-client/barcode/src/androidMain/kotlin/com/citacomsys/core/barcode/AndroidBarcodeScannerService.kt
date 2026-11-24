package com.citacomsys.core.barcode

import android.Manifest
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.ScannerView
import kotlin.time.Clock

class AndroidBarcodeScannerService : BarcodeScannerService {
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
    override fun Scan(onResult: (ScannedBarcode) -> Unit, onClose: () -> Unit) {
        var hasPermission by remember { mutableStateOf(false) }
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            hasPermission = isGranted
            if (!isGranted) onClose()
        }

        LaunchedEffect(Unit) {
            launcher.launch(Manifest.permission.CAMERA)
        }

        if (hasPermission) {
            Dialog(
                onDismissRequest = {
                    stopScan()
                    onClose()
                },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    ScannerView(
                        codeTypes = listOf(
                            org.ncgroup.kscan.BarcodeFormat.FORMAT_EAN_13,
                            org.ncgroup.kscan.BarcodeFormat.FORMAT_EAN_8,
                            org.ncgroup.kscan.BarcodeFormat.FORMAT_UPC_A,
                            org.ncgroup.kscan.BarcodeFormat.FORMAT_CODE_128,
                            org.ncgroup.kscan.BarcodeFormat.FORMAT_QR_CODE,
                            org.ncgroup.kscan.BarcodeFormat.FORMAT_DATA_MATRIX
                        ),
                        result = { result ->
                            when (result) {
                                is BarcodeResult.OnSuccess -> {
                                    onResult(
                                        ScannedBarcode(
                                            result.barcode.data,
                                            result.barcode.format,
                                            Clock.System.now().toEpochMilliseconds()
                                        )
                                    )
                                    stopScan()
                                }

                                is BarcodeResult.OnFailed -> {
                                    // Ignore frame read failures
                                }

                                else -> {
                                    // OnCanceled or other state
                                }
                            }
                        }
                    )

                    IconButton(
                        onClick = {
                            stopScan()
                            onClose()
                        },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close Scanner",
                            tint = Color.White
                        )
                    }

                    Text(
                        "Scan Barcode / QR",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 64.dp)
                    )
                }
            }
        }
    }
}

actual fun generateQrBitmap(
    content: String,
    width: Int,
    height: Int
): ImageBitmap {
    val writer = QRCodeWriter()
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val bitMatrix: BitMatrix = writer.encode(
        content,
        BarcodeFormat.QR_CODE,
        width,
        height,
        hints
    )
    val w = bitMatrix.width
    val h = bitMatrix.height
    val bmp = createBitmap(w, h, Bitmap.Config.RGB_565)
    for (x in 0 until w) {
        for (y in 0 until h) {
            val color = if (bitMatrix.get(
                    x,
                    y
                )
            ) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            bmp[x, y] = color
        }
    }
    return bmp.asImageBitmap()
}

actual fun createBarcodeScannerService(): BarcodeScannerService = AndroidBarcodeScannerService()
