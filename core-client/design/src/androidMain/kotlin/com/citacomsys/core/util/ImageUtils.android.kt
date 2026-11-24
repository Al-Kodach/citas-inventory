package com.citacomsys.core.util

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import io.github.aakira.napier.Napier
import java.io.ByteArrayOutputStream

actual fun ByteArray.toImageBitmap(): ImageBitmap? {
    return try {
        this.decodeToImageBitmap()
    } catch (e: Exception) {
        Napier.e("Failed to decode image: ${e.message}")
        null
    }
}

actual fun ImageBitmap.toByteArray(): ByteArray {
    val bitmap = this.asAndroidBitmap()
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}