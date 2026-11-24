package com.citacomsys.inventory.admin.routes

import com.citacomsys.admin.api.security.AdminBinaryAssetInput
import com.citacomsys.admin.api.security.PublishReleasePayload
import com.citacomsys.core.logging.logIdentityAccessEvent
import com.citacomsys.core.security.data.AppReleaseRepository
import com.citacomsys.core.security.plugin.Authorize
import com.citacomsys.core.security.toAuditRequestContext
import com.citacomsys.core.storage.ArtifactStorageProvider
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.types.UserCapability
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID


fun Route.configureAdminReleaseManagementEndpoints(
    releaseRepository: AppReleaseRepository,
    storageProvider: ArtifactStorageProvider,
    auditor: TelemetryAuditor
) {
    route("/app-releases") {

        // SECURITY BOUNDARY: Restrict path explicitly to System Admins
        install(Authorize) {
            requireSubscription = false // Platform core context
            requireCapabilities = setOf(UserCapability.MANAGE_SYSTEM_INFRASTRUCTURE)
        }

        get("/history") {
            val historyFeed = releaseRepository.getAllRegisteredReleases()
            call.respond(HttpStatusCode.OK, historyFeed)
        }

//        post("/publish") {
//            val auditContext = call.toAuditRequestContext()
////            val operatorId = auditContext.userId ?: "SYSTEM_OPERATOR"
//
//            var version = ""
//            var productType = ""
//            var changelog = ""
//            var isMandatory = false
//            val binaryInputs = mutableListOf<AdminBinaryAssetInput>()
//
//            try {
//                val multipartData = call.receiveMultipart(formFieldLimit = -1L)
//
//                multipartData.forEachPart { part ->
//                    when (part) {
//                        is PartData.FormItem -> {
//                            when (part.name) {
//                                "version" -> version = part.value
//                                "productType", "appFlavor" -> productType = part.value
//                                "changelog" -> changelog = part.value
//                                "isMandatory" -> isMandatory = part.value.toBoolean()
//                            }
//                        }
//
//                        is PartData.FileItem -> {
//                            val fileName = part.originalFileName ?: "build_${UUID.randomUUID()}.bin"
//                            val osTarget = part.headers["X-OS-Target"] ?: "WINDOWS"
//
//                            // STREAMS DIRECTLY TO STORAGE PROVIDER AGNOSTICALLY
//                            val publicAssetUrl = storageProvider.storeArtifact(
//                                appFlavor = productType,
//                                fileName = fileName,
//                                inputStream = part.streamProvider()
//                            )
//
//                            // Measure the file size post-write contextually if needed or fetch via header
//                            binaryInputs.add(
//                                AdminBinaryAssetInput(
//                                    fileName = fileName,
//                                    downloadUrl = publicAssetUrl, // Holds the live http://localhost link!
//                                    sizeInBytes = part.headers[HttpHeaders.ContentLength]?.toLongOrNull()
//                                        ?: 0L,
//                                    osTarget = osTarget
//                                )
//                            )
//                        }
//
//                        else -> {}
//                    }
//                    part.release()
//                }
//
//                if (version.isBlank() || productType.isBlank() || binaryInputs.isEmpty()) {
//                    call.respond(HttpStatusCode.BadRequest, "Missing release properties.")
//                    return@post
//                }
//
//                val targetReleaseId = releaseRepository.publishNewRelease(
//                    PublishReleasePayload(
//                        version,
//                        productType,
//                        changelog,
//                        isMandatory,
//                        binaryInputs
//                    )
//                )
//
//                // Log the high-level infrastructure mutation to the global SIEM engine
//                auditor.logIdentityAccessEvent(
//                    action = "PLATFORM_BUILD_PUBLISHED",
//                    outcome = "SUCCESS",
//                    failureReason = "Version: v$version on track [$productType]. Multipart binary stream ingestion completed.",
//                    level = TelemetryLevel.CRITICAL
//                )
//
//                call.respond(
//                    HttpStatusCode.Created,
//                    mapOf("status" to "deployed", "releaseId" to targetReleaseId.toString())
//                )
//            } catch (e: Exception) {
//                call.respond(
//                    HttpStatusCode.InternalServerError,
//                    "Stream processing exception: ${e.message}"
//                )
//            }
//        }

    }
}
