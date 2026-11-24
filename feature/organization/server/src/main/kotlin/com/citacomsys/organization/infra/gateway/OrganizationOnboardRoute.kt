package com.citacomsys.organization.infra.gateway

import com.citacomsys.api.organization.OrganizationService
import com.citacomsys.api.organization.model.OrganizationRegistrationRequest
import com.citacomsys.api.organization.model.WorkstationRecoveryRequest
import com.citacomsys.core.persistence.plugin.writeTransaction
import com.citacomsys.core.security.toAuditRequestContext
import com.citacomsys.feature.api.telemetry.TelemetryRequestContextElement
import io.ktor.server.auth.authenticate
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.withContext
import org.koin.ktor.ext.inject

fun Route.organizationOnboardRoute() {
    val service: OrganizationService by inject()

    route("api/v1/organization") {
        authenticate("device-access-auth") {
            post("/register") {
                val request = call.receive<OrganizationRegistrationRequest>()
                val fingerprint = call.request.headers["X-Hardware-Fingerprint"]
                val auditContext = call.toAuditRequestContext()

                val result = withContext(TelemetryRequestContextElement(auditContext)) {
                    call.writeTransaction {
                        service.registerOrganization(
                            request
                        )
                    }
                }
                call.respond(result)
            }

            post("/verify") {
                val request = call.receive<Map<String, String>>()
                val ownerId = request["ownerId"] ?: error("Missing ownerId")
                val code = request["code"] ?: error("Missing code")
                val fingerprint = call.request.headers["X-Hardware-Fingerprint"]
                val puk = call.request.headers["X-Workstation-Puk"]

                val auditContext = call.toAuditRequestContext()

                val result = withContext(TelemetryRequestContextElement(auditContext)) {
                    call.writeTransaction {
                        service.verifyOwner(
                            ownerId,
                            code,
                            fingerprint,
                            puk
                        )
                    }
                }

                call.respond(result)
            }

            post("/workstation/recover") {
                val request = call.receive<WorkstationRecoveryRequest>()
                val auditContext = call.toAuditRequestContext()
                val result = withContext(TelemetryRequestContextElement(auditContext)) {
                    call.writeTransaction { service.recoverWorkstation(request) }
                }
                call.respond(result)
            }
        }
    }
}
