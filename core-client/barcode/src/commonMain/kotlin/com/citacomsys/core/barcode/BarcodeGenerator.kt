package com.citacomsys.core.barcode

import androidx.compose.ui.graphics.ImageBitmap

expect fun generateQrBitmap(
    content: String,
    width: Int = 512,
    height: Int = 512
): ImageBitmap