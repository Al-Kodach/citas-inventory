package com.citacomsys.core.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.github.aakira.napier.Napier
import org.jetbrains.skia.Image

actual fun ByteArray.toImageBitmap(): ImageBitmap? {
    return try {
        Image.makeFromEncoded(this).toComposeImageBitmap()
    } catch (e: Exception) {
        Napier.e("Failed to decode image: ${e.message}")
        null
    }
}

