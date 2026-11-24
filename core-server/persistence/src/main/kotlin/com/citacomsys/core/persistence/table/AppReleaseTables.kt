package com.citacomsys.core.persistence.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp

object AppReleasesTable : Table("app_releases") {
    val id = long("id").autoIncrement()
    val appId = varchar("app_id", 50)
    val appName = varchar("app_name", 100)
    val version = varchar("version", 30)
    val channel = varchar("channel", 20).default("STABLE")
    val changelog = text("changelog").nullable()
    val isLatest = bool("is_latest").default(true)
    val isPublished = bool("is_published").default(true)
    val isActiveProductionRelease = bool("is_active_production_release").default(true)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(id)
}

object ReleaseArtifactsTable : Table("release_artifacts") {
    val id = long("id").autoIncrement()
    val releaseId = reference("release_id", AppReleasesTable.id, onDelete = ReferenceOption.CASCADE)
    val platform = varchar("platform", 50)
    val architecture = varchar("architecture", 50)
    val packageType = varchar("package_type", 30)
    val fileName = varchar("file_name", 150)
    val downloadUrl = text("download_url")
    val fileSizeMb = double("file_size_mb").default(0.0)
    val sha256Checksum = varchar("sha256_checksum", 64).nullable()
    val downloadCount = integer("download_count").default(0)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(id)
}

object AppDownloadsTable : Table("app_downloads") {
    val id = long("id").autoIncrement()
    val artifactId = reference("artifact_id", ReleaseArtifactsTable.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val appId = varchar("app_id", 50)
    val platform = varchar("platform", 50)
    val architecture = varchar("architecture", 50).nullable()
    val appVersion = varchar("app_version", 30)
    val ipAddress = varchar("ip_address", 45).nullable()
    val userAgent = text("user_agent").nullable()
    val downloadedAt = timestamp("downloaded_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(id)
}
