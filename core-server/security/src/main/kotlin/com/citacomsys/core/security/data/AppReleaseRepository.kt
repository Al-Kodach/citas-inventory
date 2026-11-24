package com.citacomsys.core.security.data

import com.citacomsys.admin.api.security.PublishReleasePayload
import com.citacomsys.core.persistence.table.AppReleasesTable
import com.citacomsys.core.persistence.table.ReleaseArtifactsTable
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.feature.api.telemetry.PrivateBinaryAssetDto
import com.citacomsys.feature.api.telemetry.PrivateUpdateResponse
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID
import kotlin.time.Clock

class AppReleaseRepository {

    suspend fun getLatestApplicableRelease(flavor: String): PrivateUpdateResponse? = dbQuery {
        val latestReleaseRow = AppReleasesTable
            .selectAll()
            .where {
                (AppReleasesTable.appId eq flavor) and
                        (AppReleasesTable.isActiveProductionRelease eq true)
            }
            .orderBy(AppReleasesTable.createdAt to SortOrder.DESC)
            .limit(1)
            .firstOrNull() ?: return@dbQuery null

        val releaseId = latestReleaseRow[AppReleasesTable.id]

        val associatedBinaries = ReleaseArtifactsTable
            .selectAll()
            .where { ReleaseArtifactsTable.releaseId eq releaseId }
            .map { binaryRow ->
                PrivateBinaryAssetDto(
                    fileName = binaryRow[ReleaseArtifactsTable.fileName],
                    downloadUrl = binaryRow[ReleaseArtifactsTable.downloadUrl],
                    sizeInBytes = (binaryRow[ReleaseArtifactsTable.fileSizeMb] * 1024 * 1024).toLong()
                )
            }

        PrivateUpdateResponse(
            version = latestReleaseRow[AppReleasesTable.version],
            changelog = latestReleaseRow[AppReleasesTable.changelog] ?: "",
            publishedAtString = latestReleaseRow[AppReleasesTable.createdAt].toString(),
            isMandatoryForceUpdate = false,
            binaries = associatedBinaries
        )
    }

    suspend fun publishNewRelease(payload: PublishReleasePayload): UUID = dbQuery {
        val newReleaseId = UUID.randomUUID()

        AppReleasesTable.update({
            (AppReleasesTable.appId eq payload.appId) and
                    (AppReleasesTable.isActiveProductionRelease eq true)
        }) {
            it[isActiveProductionRelease] = false
        }

        val numericId = AppReleasesTable.insert {
            it[appId] = payload.appId
            it[appName] = payload.appName
            it[version] = payload.version
            it[channel] = payload.channel
            it[changelog] = payload.changelog
            it[isLatest] = payload.isLatest
            it[isPublished] = payload.isPublished
            it[isActiveProductionRelease] = true
            it[createdAt] = Clock.System.now()
        } get AppReleasesTable.id

        payload.binaries.forEach { binary ->
            ReleaseArtifactsTable.insert {
                it[releaseId] = numericId
                it[platform] = binary.platform.uppercase()
                it[architecture] = binary.architecture.uppercase()
                it[packageType] = binary.packageType.uppercase()
                it[fileName] = binary.fileName
                it[downloadUrl] = binary.downloadUrl
                it[fileSizeMb] = if (binary.fileSizeMb > 0.0) binary.fileSizeMb else (binary.sizeInBytes.toDouble() / (1024 * 1024))
                it[sha256Checksum] = binary.sha256Checksum
            }
        }

        return@dbQuery newReleaseId
    }

    suspend fun getAllRegisteredReleases(): List<PrivateUpdateResponse> = dbQuery {
        AppReleasesTable
            .selectAll()
            .orderBy(AppReleasesTable.createdAt to SortOrder.DESC)
            .map { releaseRow ->
                val releaseId = releaseRow[AppReleasesTable.id]

                val binaries = ReleaseArtifactsTable
                    .selectAll()
                    .where { ReleaseArtifactsTable.releaseId eq releaseId }
                    .map { binaryRow ->
                        PrivateBinaryAssetDto(
                            fileName = binaryRow[ReleaseArtifactsTable.fileName],
                            downloadUrl = binaryRow[ReleaseArtifactsTable.downloadUrl],
                            sizeInBytes = (binaryRow[ReleaseArtifactsTable.fileSizeMb] * 1024 * 1024).toLong()
                        )
                    }

                PrivateUpdateResponse(
                    version = releaseRow[AppReleasesTable.version],
                    changelog = releaseRow[AppReleasesTable.changelog] ?: "",
                    publishedAtString = releaseRow[AppReleasesTable.createdAt].toString(),
                    isMandatoryForceUpdate = false,
                    binaries = binaries
                )
            }
    }
}
