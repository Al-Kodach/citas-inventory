package com.citacomsys.backup.fileUtil

import com.citacomsys.backup.api.fileUtil.BackupArchiveService
import okio.Path
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal class BackupArchiveServiceImpl : BackupArchiveService {

    override suspend fun createArchive(sourceDirectory: Path, outputFile: Path) {
        val sourceFolder = File(sourceDirectory.toString())
        val zipFile = File(outputFile.toString())

        ZipOutputStream(FileOutputStream(zipFile)).use { zipStream ->
            sourceFolder.walkTopDown().forEach { file ->
                if (file.isFile) {
                    // Calculate relative path inside the zip file
                    val relativePath = file.absolutePath.substring(sourceFolder.absolutePath.length + 1)
                    val zipEntry = ZipEntry(relativePath)
                    zipStream.putNextEntry(zipEntry)

                    FileInputStream(file).use { fileStream ->
                        fileStream.copyTo(zipStream)
                    }
                    zipStream.closeEntry()
                }
            }
        }
    }

    override suspend fun extractArchive(archivePath: Path, outputDirectory: Path) {
        val zipFile = File(archivePath.toString())
        val outputFolder = File(outputDirectory.toString())

        if (!outputFolder.exists()) outputFolder.mkdirs()

        ZipInputStream(FileInputStream(zipFile)).use { zipStream ->
            var entry: ZipEntry? = zipStream.nextEntry
            while (entry != null) {
                val newFile = File(outputFolder, entry.name)

                // Prevent Zip Slip vulnerability (directory traversal attacks)
                if (!newFile.canonicalPath.startsWith(outputFolder.canonicalPath)) {
                    throw SecurityException("Malicious zip entry detected outside staging scope: ${entry.name}")
                }

                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    // Ensure structural parent folders exist
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fileStream ->
                        zipStream.copyTo(fileStream)
                    }
                }
                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }
        }
    }
}
