package com.citacomsys.core.barcode

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.sarxos.webcam.Webcam
import com.google.zxing.BarcodeFormat
//import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.Result
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.ncgroup.kscan.ScannerController
import java.awt.AWTEvent
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.event.AWTEventListener
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import org.ncgroup.kscan.BarcodeFormat as KScanBarcodeFormat
import org.ncgroup.kscan.BarcodeResult as KScanBarcodeResult
import org.ncgroup.kscan.ScannerView

class JvmBarcodeScannerService : BarcodeScannerService {
    override val isHardwareScanner: Boolean = true

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var scanCallback: ((ScannedBarcode) -> Unit)? = null
    private var errorCallback: ((String) -> Unit)? = null

    private val keyBuffer = StringBuilder()
    private var lastKeyTime = 0L

    init {
        setupGlobalHardwareScannerListener()
    }

    /**
     * Intercepts rapid keystrokes from USB / Bluetooth hardware barcode laser scanners
     * across the application window using a global AWT Event Listener.
     */
    private fun setupGlobalHardwareScannerListener() {
        try {
            Toolkit.getDefaultToolkit().addAWTEventListener(
                AWTEventListener { event ->
                    if (event is KeyEvent && event.id == KeyEvent.KEY_TYPED) {
                        val now = System.currentTimeMillis()
                        val char = event.keyChar

                        // Hardware scanners send characters rapidly (< 50ms per key)
                        if (now - lastKeyTime > 100) {
                            keyBuffer.clear()
                        }
                        lastKeyTime = now

                        if (char == '\n' || char == '\r') {
                            val scannedValue = keyBuffer.toString().trim()
                            if (scannedValue.length >= 3) {
                                val result = ScannedBarcode(
                                    rawValue = scannedValue,
                                    format = "USB_HARDWARE_SCANNER",
                                    timestamp = now
                                )
                                scanCallback?.invoke(result)
                            }
                            keyBuffer.clear()
                        } else if (!char.isISOControl()) {
                            keyBuffer.append(char)
                        }
                    }
                },
                AWTEvent.KEY_EVENT_MASK
            )
        } catch (e: Exception) {
            Napier.d(
                tag = "JvmBarcodeScanner",
                message = "Failed to register AWT Event Listener",
                throwable = e
            )
            // Headless or non-AWT environment fallback
        }
    }

    override fun startScan(onResult: (ScannedBarcode) -> Unit, onError: (String) -> Unit) {
        this.scanCallback = onResult
        this.errorCallback = onError
        _isScanning.value = true
    }

    override fun stopScan() {
        _isScanning.value = false
    }

    override fun openCamera() {
    }

    @Composable
    override fun Scan(onResult: (ScannedBarcode) -> Unit, onClose: () -> Unit) {
        var barcodeInput by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var selectedTab by remember { mutableStateOf(0) } // 0 = Webcam, 1 = USB Scanner / Manual
        val focusRequester = remember { FocusRequester() }

        // Register active callback for global hardware scanner listener
        DisposableEffect(Unit) {
            startScan(
                onResult = { scanned ->
                    onResult(scanned)
                    stopScan()
                    onClose()
                },
                onError = { err -> errorMessage = err }
            )
            onDispose {
                stopScan()
            }
        }

        fun submitCode(code: String, format: String = "MANUAL_OR_HARDWARE") {
            val cleanCode = code.trim()
            if (cleanCode.isNotEmpty()) {
                onResult(
                    ScannedBarcode(
                        rawValue = cleanCode,
                        format = format,
                        timestamp = Clock.System.now().toEpochMilliseconds()
                    )
                )
                stopScan()
                onClose()
            }
        }

        fun decodeImageAndSubmit(image: BufferedImage) {
            val decoded = decodeBarcodeFromImage(image)
            if (decoded != null) {
                onResult(decoded)
                stopScan()
                onClose()
            } else {
                errorMessage = "No valid 1D/2D barcode found in image."
            }
        }

        fun handleClipboardPaste() {
            val img = readImageFromClipboard()
            if (img != null) {
                decodeImageAndSubmit(img)
            } else {
                errorMessage = "No image found on clipboard."
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
                    .width(480.dp)
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
                                text = "Barcode / QR  Scanner",
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

                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Webcam") },
                            icon = { Icon(Icons.Default.Videocam, contentDescription = null) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("USB Scanner") },
                            icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) }
                        )
                    }

                    if (selectedTab == 0) {
                        DesktopWebcamScanner(
                            onResult = { scanned ->
                                onResult(scanned)
                                stopScan()
                                onClose()
                            },
                            onClose = onClose
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Point a USB/Bluetooth hardware scanner at a barcode, or paste an image containing a barcode/QR code.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = barcodeInput,
                                onValueChange = { input ->
                                    val cleanInput = input.replace("\n", "").replace("\r", "")
                                    barcodeInput = cleanInput
                                    if (input.contains("\n") || input.contains("\r")) {
                                        submitCode(cleanInput, "USB_HARDWARE_SCANNER")
                                    }
                                },
                                label = { Text("Hardware Scanner / Manual Barcode") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { handleClipboardPaste() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Paste Image")
                                }

                                Button(
                                    onClick = { submitCode(barcodeInput) },
                                    enabled = barcodeInput.isNotBlank(),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Submit")
                                }
                            }
                        }
                    }

                    errorMessage?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    /**
     * Decodes 1D/2D barcodes (EAN-13, UPC, Code 128, QR Code, Data Matrix) from a [BufferedImage] using ZXing.
     */
    private fun decodeBarcodeFromImage(image: BufferedImage): ScannedBarcode? {
        val hints = mapOf<DecodeHintType, Any>(
            DecodeHintType.TRY_HARDER to true
        )
        return decodeFrameWithZxing(image, hints)
    }

    private fun readImageFromClipboard(): BufferedImage? {
        return try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            if (clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
                clipboard.getData(DataFlavor.imageFlavor) as? BufferedImage
            } else null
        } catch (_: Exception) {
            null
        }
    }
}

@Composable
private fun DesktopWebcamScanner(
    onResult: (ScannedBarcode) -> Unit,
    onClose: () -> Unit
) {
    var webcams by remember { mutableStateOf(emptyList<Webcam>()) }
    var selectedWebcamIndex by remember { mutableStateOf(0) }
    var selectedResolutionIndex by remember { mutableStateOf(0) }
    var isTorchOn by remember { mutableStateOf(false) }
    var currentFrame by remember { mutableStateOf<ImageBitmap?>(null) }
    var webcamError by remember { mutableStateOf<String?>(null) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // Load available webcams
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val list = Webcam.getWebcams()
                webcams = list
            } catch (_: Exception) {
            }
        }
    }

    // Camera capture and scanning coroutine
    LaunchedEffect(selectedWebcamIndex, selectedResolutionIndex) {
        webcamError = null
        currentFrame = null
        var activeWebcam: Webcam? = null
        var isScanningActive = true

        withContext(Dispatchers.IO) {
            try {
                val availableWebcams = try {
                    Webcam.getWebcams()
                } catch (_: Exception) {
                    emptyList()
                }
                activeWebcam = availableWebcams.getOrNull(selectedWebcamIndex) ?: try {
                    Webcam.getDefault()
                } catch (_: Exception) {
                    null
                }

                if (activeWebcam == null) {
                    withContext(Dispatchers.Main) {
                        webcamError = "No webcam camera found on this computer."
                    }
                    return@withContext
                }

                try {
                    val sizes = activeWebcam.viewSizes
                    if (sizes.isNotEmpty()) {
                        val sizeIndex = selectedResolutionIndex.coerceIn(0, sizes.lastIndex)
                        activeWebcam.viewSize = sizes[sizeIndex]
                    }
                } catch (_: Exception) {
                    // Fallback to default resolution
                }

                if (!activeWebcam.isOpen) {
                    activeWebcam.open()
                }

                val hints = mapOf<DecodeHintType, Any>(
                    DecodeHintType.TRY_HARDER to true,
                    DecodeHintType.POSSIBLE_FORMATS to listOf(
                        BarcodeFormat.EAN_13,
                        BarcodeFormat.UPC_A,
                        BarcodeFormat.EAN_8,
                        BarcodeFormat.CODE_128,
                        BarcodeFormat.CODE_39,
                        BarcodeFormat.CODE_93,
                        BarcodeFormat.CODABAR,
                        BarcodeFormat.QR_CODE,
                        BarcodeFormat.DATA_MATRIX
                    )
                )

                while (isScanningActive && activeWebcam.isOpen) {
                    val bufferedImage = activeWebcam.image
                    if (bufferedImage != null) {
                        val composeBitmap = bufferedImage.toComposeImageBitmap()
                        withContext(Dispatchers.Main) {
                            currentFrame = composeBitmap
                        }

                        // Try decoding center target region first (where viewfinder reticle is aligned)
                        val w = bufferedImage.width
                        val h = bufferedImage.height
                        val cropW = (w * 0.80).toInt()
                        val cropH = (h * 0.65).toInt()
                        val cropX = (w - cropW) / 2
                        val cropY = (h - cropH) / 2

                        val croppedImage = try {
                            bufferedImage.getSubimage(cropX, cropY, cropW, cropH)
                        } catch (_: Exception) {
                            bufferedImage
                        }

                        // Upscale cropped region 1.75x to sharpen fine EAN-13 vertical lines
                        val upscaledCropped = scaleImageForZxing(croppedImage, 1.75)

                        val scannedResult = decodeFrameWithZxing(upscaledCropped, hints)
                            ?: decodeFrameWithZxing(croppedImage, hints)
                            ?: decodeFrameWithZxing(bufferedImage, hints)

                        if (scannedResult != null) {
                            isScanningActive = false
                            withContext(Dispatchers.Main) {
                                onResult(scannedResult)
                            }
                            break
                        }
                    }
                    delay(20.milliseconds) // ~20 FPS scan rate
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    webcamError = e.message ?: "Failed to open webcam."
                }
            } finally {
                try {
                    activeWebcam?.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Camera Controls Row
        if (webcams.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dropdown camera picker
                Box {
                    OutlinedButton(
                        onClick = { isDropdownExpanded = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Videocam,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = webcams.getOrNull(selectedWebcamIndex)?.name ?: "Camera",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        webcams.forEachIndexed { index, webcam ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        webcam.name,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                onClick = {
                                    selectedWebcamIndex = index
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Torch / Light Toggle Button
                    IconButton(
                        onClick = { isTorchOn = !isTorchOn }
                    ) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Toggle Light / Torch",
                            tint = if (isTorchOn) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Resolution toggle button
                    val activeCam = webcams.getOrNull(selectedWebcamIndex)
                    if (activeCam != null && activeCam.viewSizes.size > 1) {
                        TextButton(
                            onClick = {
                                selectedResolutionIndex =
                                    (selectedResolutionIndex + 1) % activeCam.viewSizes.size
                            }
                        ) {
                            val currentSize = activeCam.viewSizes.getOrNull(selectedResolutionIndex)
                            Text(
                                text = if (currentSize != null) "${currentSize.width}x${currentSize.height}" else "Resolution",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }

        // Camera Preview Box with Viewfinder Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val frame = currentFrame
            if (frame != null) {
                Image(
                    bitmap = frame,
                    contentDescription = "Webcam Live Stream",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Viewfinder Reticle Frame & Animated Scanner Line Overlay
                WebcamViewfinderOverlay()
            } else if (webcamError != null) {
                Text(
                    text = webcamError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Starting camera stream...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun KScanView(onResult: (ScannedBarcode) -> Unit, onClose: () -> Unit) {
    val scannerController = remember { ScannerController() }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        ScannerView(
            modifier = Modifier.fillMaxSize(),
            codeTypes = listOf(
                KScanBarcodeFormat.FORMAT_EAN_13,
                KScanBarcodeFormat.FORMAT_EAN_8,
                KScanBarcodeFormat.FORMAT_UPC_A,
                KScanBarcodeFormat.FORMAT_CODE_128,
                KScanBarcodeFormat.FORMAT_QR_CODE,
                KScanBarcodeFormat.FORMAT_DATA_MATRIX
            ),
            scannerController = scannerController,
            result = { result ->
                if (result is KScanBarcodeResult.OnSuccess) {
                    val rawData = result.barcode.data.trim()
                    if (rawData.length >= 3) {
                        onResult(
                            ScannedBarcode(
                                rawValue = rawData,
                                format = result.barcode.format,
                                timestamp = Clock.System.now().toEpochMilliseconds()
                            )
                        )
                    }
                }
            }
        )

        IconButton(
            onClick = {
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
            "Scan barcode / QR",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
        )
    }
}

@Composable
private fun WebcamViewfinderOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "LaserLine")
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ScanY"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val boxWidth = width * 0.75f
        val boxHeight = height * 0.65f
        val left = (width - boxWidth) / 2
        val top = (height - boxHeight) / 2
        val right = left + boxWidth
        val bottom = top + boxHeight

        // Draw semi-transparent dark mask around target region
        val maskColor = Color.Black.copy(alpha = 0.45f)
        drawRect(color = maskColor, topLeft = Offset(0f, 0f), size = Size(width, top))
        drawRect(
            color = maskColor,
            topLeft = Offset(0f, bottom),
            size = Size(width, height - bottom)
        )
        drawRect(color = maskColor, topLeft = Offset(0f, top), size = Size(left, boxHeight))
        drawRect(
            color = maskColor,
            topLeft = Offset(right, top),
            size = Size(width - right, boxHeight)
        )

        // Draw target bounding box corner brackets
        val strokeWidth = 4.dp.toPx()
        val cornerLength = 24.dp.toPx()
        val cornerColor = Color(0xFF00E676) // Bright green reticle color

        // Top-Left corner
        drawLine(cornerColor, Offset(left, top), Offset(left + cornerLength, top), strokeWidth)
        drawLine(cornerColor, Offset(left, top), Offset(left, top + cornerLength), strokeWidth)

        // Top-Right corner
        drawLine(cornerColor, Offset(right, top), Offset(right - cornerLength, top), strokeWidth)
        drawLine(cornerColor, Offset(right, top), Offset(right, top + cornerLength), strokeWidth)

        // Bottom-Left corner
        drawLine(
            cornerColor,
            Offset(left, bottom),
            Offset(left + cornerLength, bottom),
            strokeWidth
        )
        drawLine(
            cornerColor,
            Offset(left, bottom),
            Offset(left, bottom - cornerLength),
            strokeWidth
        )

        // Bottom-Right corner
        drawLine(
            cornerColor,
            Offset(right, bottom),
            Offset(right - cornerLength, bottom),
            strokeWidth
        )
        drawLine(
            cornerColor,
            Offset(right, bottom),
            Offset(right, bottom - cornerLength),
            strokeWidth
        )

        // Draw animated laser scanning line
        val lineY = top + (boxHeight * scanLineY)
        drawLine(
            color = Color.Red.copy(alpha = 0.85f),
            start = Offset(left + 8.dp.toPx(), lineY),
            end = Offset(right - 8.dp.toPx(), lineY),
            strokeWidth = 2.dp.toPx()
        )
    }
}

private fun decodeFrameWithZxing(
    image: BufferedImage,
    hints: Map<DecodeHintType, Any>
): ScannedBarcode? {
    // Convert source frame to clean 8-bit grayscale for ZXing luminance calculation
    val grayImage = toGrayscaleImage(image)
    val source = BufferedImageLuminanceSource(grayImage)

    fun isValidResult(result: Result?, cleanValue: String): Boolean {
        if (result == null || cleanValue.isBlank()) return false
        // Filter out false-positive single/double character glitches from Code 39 / Codabar / Code 93
        val isShortNoise = (result.barcodeFormat == BarcodeFormat.CODE_39 ||
                            result.barcodeFormat == BarcodeFormat.CODABAR ||
                            result.barcodeFormat == BarcodeFormat.CODE_93) && cleanValue.length < 3
        return !isShortNoise
    }

    // Try HybridBinarizer first
    try {
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val reader = MultiFormatReader().apply { setHints(hints) }
        val result = reader.decodeWithState(bitmap)
        val cleanValue = result?.text?.trim() ?: ""
        if (isValidResult(result, cleanValue)) {
            Napier.d(
                tag = "JvmBarcodeScanner",
                message = "Decoded barcode: '$cleanValue' (Format: ${result.barcodeFormat.name})"
            )
            return ScannedBarcode(
                rawValue = cleanValue,
                format = result.barcodeFormat.name,
                timestamp = System.currentTimeMillis()
            )
        }
    } catch (_: Exception) {
    }

    // Try GlobalHistogramBinarizer for glossy or low-light product packaging
    try {
        val bitmap = BinaryBitmap(GlobalHistogramBinarizer(source))
        val reader = MultiFormatReader().apply { setHints(hints) }
        val result = reader.decodeWithState(bitmap)
        val cleanValue = result?.text?.trim() ?: ""
        if (isValidResult(result, cleanValue)) {
            Napier.d(
                tag = "JvmBarcodeScanner",
                message = "Decoded barcode (GlobalHistogram): '$cleanValue' (Format: ${result.barcodeFormat.name})"
            )
            return ScannedBarcode(
                rawValue = cleanValue,
                format = result.barcodeFormat.name,
                timestamp = System.currentTimeMillis()
            )
        }
    } catch (_: Exception) {
    }

    return null
}

private fun toGrayscaleImage(source: BufferedImage): BufferedImage {
    return try {
        val gray = BufferedImage(source.width, source.height, BufferedImage.TYPE_BYTE_GRAY)
        val g = gray.createGraphics()
        g.drawImage(source, 0, 0, null)
        g.dispose()
        gray
    } catch (_: Exception) {
        source
    }
}

private fun scaleImageForZxing(source: BufferedImage, factor: Double): BufferedImage {
    return try {
        val targetWidth = (source.width * factor).toInt()
        val targetHeight = (source.height * factor).toInt()
        val scaled = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_BYTE_GRAY)
        val g = scaled.createGraphics()
        g.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC
        )
        g.drawImage(source, 0, 0, targetWidth, targetHeight, null)
        g.dispose()
        scaled
    } catch (_: Exception) {
        source
    }
}

actual fun createBarcodeScannerService(): BarcodeScannerService = JvmBarcodeScannerService()
