package com.citacomsys.backup.ui

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.download

actual suspend fun FileKit.saveFile(
    baseName: String,
    extension: String,
    bytes: ByteArray
) {
    FileKit.download(
        bytes = bytes,
        fileName = "$baseName.$extension"
    )
}

