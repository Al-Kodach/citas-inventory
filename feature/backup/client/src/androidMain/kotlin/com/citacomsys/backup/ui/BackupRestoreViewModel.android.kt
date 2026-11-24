package com.citacomsys.backup.ui

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.write

actual suspend fun FileKit.saveFile(
    baseName: String,
    extension: String,
    bytes: ByteArray
) {

    val fileSaver = openFileSaver(
        suggestedName = baseName,
        defaultExtension = extension,
        dialogSettings = FileKitDialogSettings.createDefault()
    )

    fileSaver?.write(bytes)
}