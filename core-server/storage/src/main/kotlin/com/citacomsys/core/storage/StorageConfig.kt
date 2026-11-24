package com.citacomsys.core.storage

import java.io.File

data class StorageConfig(
    val baseDir: File,
    val uploadsDir: File,
    val backupsDir: File,
    val logsDir: File,
)