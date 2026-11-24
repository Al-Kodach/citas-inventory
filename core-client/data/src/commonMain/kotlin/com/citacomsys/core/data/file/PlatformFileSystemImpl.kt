package com.citacomsys.core.data.file

import com.citacomsys.core.domain.file.PlatformFileSystem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.Path
import kotlinx.coroutines.withContext

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class PlatformFileSupport {
    fun getTempDir(): String
    fun getInternalDir(): String
    fun getDocumentsDir(): String
    fun getUserHomeDir(): String
    fun getDatabaseFile(name: String): String
    val hostFileSystem: FileSystem
}


internal class PlatformFileSystemImpl(
    private val support: PlatformFileSupport,
    private val ioDispatcher: CoroutineDispatcher
) : PlatformFileSystem {

    private val fs: FileSystem = support.hostFileSystem

    // FIXED: Points to the support properties directly instead of recursing
    override fun getInternalDir(): Path = support.getInternalDir().toPath()
    override fun getPublicDir(): Path = support.getDocumentsDir().toPath()
    override fun getTempDir(): Path = support.getTempDir().toPath()

    override fun getDatabaseDirPath(): Path = getInternalDir()
    override fun getDatabaseFile(name: String): Path = support.getDatabaseFile(name).toPath()

    override suspend fun exists(path: Path): Boolean = withContext(ioDispatcher) {
        fs.exists(path)
    }

    override suspend fun delete(path: Path) = withContext(ioDispatcher) {
        fs.delete(path, mustExist = false)
    }

    override suspend fun deleteRecursively(path: Path) {
        withContext(ioDispatcher) {
            fs.deleteRecursively(path)
        }
    }

    override suspend fun readBytes(path: Path): ByteArray = withContext(ioDispatcher) {
        fs.read(path) { readByteArray() }
    }

    override suspend fun writeBytes(path: Path, bytes: ByteArray): Unit =
        withContext(ioDispatcher) {
            path.parent?.let { fs.createDirectories(it) }
            fs.write(path) { write(bytes) }
        }

    override suspend fun readText(path: Path): String = withContext(ioDispatcher) {
        fs.read(path) { readUtf8() }
    }

    override suspend fun getFileSize(path: Path): Long = withContext(ioDispatcher) {
        try {
            fs.metadata(path).size ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    override suspend fun createTempDirectory(prefix: String): Path {
        val root = getTempDir()
        val targetDir = root / prefix
        fs.createDirectories(targetDir)
        return targetDir
    }

    override suspend fun createInternalFile(suggestedName: String, extension: String): Path =
        withContext(ioDispatcher) {
            val root = getInternalDir()
            // Use Okio path resolution matching the target OS format (e.g. \ for Win, / for Unix)
            val sanitizedExtension = if (extension.startsWith(".")) extension else ".$extension"
            val targetFile = root / "$suggestedName$sanitizedExtension"

            targetFile.parent?.let { fs.createDirectories(it) }
            targetFile
        }
}

