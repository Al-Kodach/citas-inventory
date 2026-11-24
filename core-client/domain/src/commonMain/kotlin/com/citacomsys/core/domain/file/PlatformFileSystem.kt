package com.citacomsys.core.domain.file

import okio.Path

interface PlatformFileSystem {
    fun getInternalDir(): Path
    fun getPublicDir(): Path
    fun getTempDir(): Path

    fun getDatabaseDirPath(): Path
    fun getDatabaseFile(name: String): Path

    suspend fun exists(path: Path): Boolean
    suspend fun delete(path: Path)
    suspend fun deleteRecursively(path: Path)
    suspend fun readBytes(path: Path): ByteArray
    suspend fun writeBytes(path: Path, bytes: ByteArray)
    suspend fun readText(path: Path): String
    suspend fun getFileSize(path: Path): Long
    suspend fun createInternalFile(suggestedName: String, extension: String): Path
    suspend fun createTempDirectory(prefix: String): Path
}
