package com.citacomsys.core.barcode

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter

actual fun generateQrBitmap(
    content: String,
    width: Int,
    height: Int
): ImageBitmap {
    val writer = QRCodeWriter()
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints)
    val bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix)
    return bufferedImage.toComposeImageBitmap()
}