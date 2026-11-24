package com.citacomsys.core.storage

import com.typesafe.config.Config
import io.ktor.server.application.Application
import mu.KotlinLogging
import org.koin.core.context.loadKoinModules
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

private val logger = KotlinLogging.logger("StorageModule")

val storageModule: Module = module {
    single<StorageConfig> {
        loadStorageConfig(get())
    }
    // For local testing, compile utilizing the filesystem engine target
    single<ArtifactStorageProvider> {
        val r2AccessKey = System.getenv("R2_ACCESS_KEY")
        val r2SecretKey = System.getenv("R2_SECRET_KEY")
        val r2Endpoint = System.getenv("R2_ENDPOINT")
        val r2Bucket = System.getenv("R2_BUCKET_NAME")
        val cdnBaseUrl = System.getenv("CDN_BASE_URL") ?: "http://localhost:8080/static/builds"

        if (!r2AccessKey.isNullOrBlank() && !r2SecretKey.isNullOrBlank() && !r2Endpoint.isNullOrBlank() && !r2Bucket.isNullOrBlank()) {
            logger.info("Initializing Cloudflare R2 Artifact Storage Provider...")
            R2StorageProvider(
                accessKey = r2AccessKey,
                secretKey = r2SecretKey,
                endpoint = r2Endpoint,
                bucketName = r2Bucket,
                publicCdnBaseUrl = cdnBaseUrl
            )
        } else {
            logger.warn("Cloudflare R2 credentials missing. Falling back to Local Filesystem storage.")
            LocalFilesystemStorageProvider(
                baseStorageDirectoryPath = "./build-artifacts-vault",
                publicCdnBaseUrl = cdnBaseUrl
            )
        }
    }
}

private fun loadStorageConfig(config: Config): StorageConfig {
    val storageConf = config.getConfig("app.storage")
    val baseDir = File(storageConf.getString("base_dir"))

    return StorageConfig(
        baseDir = baseDir,
        uploadsDir = File(baseDir, storageConf.getString("uploads_subdir")),
        backupsDir = File(baseDir, storageConf.getString("backups_subdir")),
        logsDir = File(baseDir, storageConf.getString("logs_subdir")),
    )
}