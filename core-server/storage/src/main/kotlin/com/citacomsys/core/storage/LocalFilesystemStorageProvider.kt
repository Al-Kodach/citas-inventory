package com.citacomsys.core.storage


import java.io.File
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalFilesystemStorageProvider(
    private val baseStorageDirectoryPath: String, // e.g., "/var/www/citas/storage/builds" or "C:\\CitasStorage\\Builds"
    private val publicCdnBaseUrl: String          // e.g., "http://localhost:8080/static/builds"
) : ArtifactStorageProvider {

    override suspend fun storeArtifact(
        appFlavor: String,
        fileName: String,
        inputStream: InputStream
    ): String = withContext(Dispatchers.IO) {
        // Create a safe path structure based on the application flavor category
        val flavorFolder = File(baseStorageDirectoryPath, appFlavor.lowercase()).apply {
            if (!exists()) mkdirs()
        }

        // Generate a unique, deduplicated timestamp filename to prevent asset naming conflicts
        val uniqueFileName = "${System.currentTimeMillis()}_$fileName"
        val targetFile = File(flavorFolder, uniqueFileName)

        // Stream data blocks directly onto the hard drive to prevent server RAM block fragmentation
        targetFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }

        //  Return the public URL mapping path token for downstream DB injection loops
        return@withContext "$publicCdnBaseUrl/${appFlavor.lowercase()}/$uniqueFileName"
    }
}
