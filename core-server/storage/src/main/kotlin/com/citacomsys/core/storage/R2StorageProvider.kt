package com.citacomsys.core.storage

import java.io.InputStream
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger("R2StorageProvider")

class R2StorageProvider(
    accessKey: String,
    secretKey: String,
    endpoint: String,
    private val bucketName: String,
    private val publicCdnBaseUrl: String
) : ArtifactStorageProvider {

    private val s3Client = S3Client.builder()
        .endpointOverride(URI.create(endpoint))
        .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
        .region(Region.US_EAST_1)
        .build()

    override suspend fun storeArtifact(
        appFlavor: String,
        fileName: String,
        inputStream: InputStream
    ): String = withContext(Dispatchers.IO) {
        val uniqueFileName = "${System.currentTimeMillis()}_$fileName"
        val key = "${appFlavor.lowercase()}/$uniqueFileName"

        try {
            val bytes = inputStream.readAllBytes()
            val putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("application/octet-stream")
                .build()

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes))
            
            val finalUrl = "$publicCdnBaseUrl/$key"
            logger.info { "Successfully uploaded artifact to R2: $finalUrl" }
            finalUrl
        } catch (e: Exception) {
            logger.error(e) { "Failed to upload artifact to Cloudflare R2: $key" }
            throw e
        }
    }
}
