package com.citacomsys.backup.api.fileUtil

import okio.Path

interface BackupCryptoService {
    suspend fun encrypt(inputPath: Path, outputPath: Path, password: String)
    suspend fun decrypt(inputPath: Path, outputPath: Path, password: String)
    suspend fun checksum(path: Path): String
}

interface BackupArchiveService {
    suspend fun createArchive(sourceDirectory: Path, outputFile: Path)
    suspend fun extractArchive(archivePath: Path, outputDirectory: Path)
}
