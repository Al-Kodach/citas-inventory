package com.citacomsys.core.storage

import java.io.InputStream

interface ArtifactStorageProvider {
    /**
     * Persists a binary package stream onto the storage system.
     * @return The absolute, public-facing HTTP download URL path string.
     */
    suspend fun storeArtifact(
        appFlavor: String,
        fileName: String,
        inputStream: InputStream
    ): String
}
