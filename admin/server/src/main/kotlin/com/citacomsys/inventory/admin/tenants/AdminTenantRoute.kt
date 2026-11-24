package com.citacomsys.inventory.admin.tenants

import com.citacomsys.admin.api.tenant.AdminPreRegisterRequest
import com.citacomsys.admin.api.tenant.OrganizationDetails
import com.citacomsys.core.persistence.plugin.readTransaction
import com.citacomsys.core.persistence.plugin.writeTransaction
import com.citacomsys.core.security.plugin.Authorize
import com.citacomsys.types.PageQuery
import com.citacomsys.types.UserCapability
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

internal fun Route.adminTenantRoute(
    tenantService: AdminTenantService
) {
    route("/tenants") {
        install(Authorize) {
            requireCapabilities = setOf(UserCapability.MANAGE_TENANTS)
        }

        authenticate("admin-access-auth") {
            post {
                val query = call.receive<PageQuery>()
                val orgs = call.readTransaction {
                    tenantService.fetchOrganizations(query)
                }
                call.respond(orgs)
            }

            post("pre-register") {
                val request = call.receive<AdminPreRegisterRequest>()
                val result = call.writeTransaction {
                    tenantService.preRegisterOrganization(request)
                }
                call.respond(result)
            }

            route("{orgId}") {
                get {
                    val id = call.parameters["orgId"] ?: error("Organization ID param missing")
                    val detail: OrganizationDetails =
                        call.readTransaction { tenantService.getOrganizationDetails(id) }
                    call.respond(detail)
                }

                post("suspend") {
                    val id = call.parameters["orgId"] ?: error("Organization ID param missing")
                    val reason =
                        call.request.queryParameters["reason"] ?: error("Reason param missing")
                    val result = call.writeTransaction {
                        tenantService.suspendOrganization(id, reason).getOrThrow()
                    }
                    call.respond(result)
                }

                post("activate") {
                    val id = call.parameters["orgId"] ?: error("Organization ID param missing")
                    val result =
                        call.writeTransaction { tenantService.activateOrganization(id).getOrThrow() }
                    call.respond(result)
                }

                route("/workspaces") {
                    get {
                        val id = call.parameters["orgId"] ?: error("Organization ID param missing")
                    }

                    route("{activeWorkspaceId}") {
                        get {
                            val id = call.parameters["activeWorkspaceId"]
                                ?: error("Workspace ID param missing")
                            val detail =
                                call.readTransaction { tenantService.getWorkspaceDetails(id) }
                            call.respond(detail)
                        }

                        post("suspend") {
                            val id = call.parameters["activeWorkspaceId"]
                                ?: error("Workspace ID param missing")
                            val reason =
                                call.request.queryParameters["reason"]
                                    ?: error("Reason param missing")
                            val result =
                                call.writeTransaction {
                                    tenantService.suspendWorkspace(id, reason).getOrThrow()
                                }
                            call.respond(result)
                        }

                        post("activate") {
                            val id = call.parameters["activeWorkspaceId"]
                                ?: error("Workspace ID param missing")
                            val result =
                                call.writeTransaction {
                                    tenantService.activateWorkspace(id).getOrThrow()
                                }
                            call.respond(result)
                        }

                        delete {
                            val id = call.parameters["activeWorkspaceId"]
                                ?: error("Workspace ID param missing")
                            val result =
                                call.writeTransaction {
                                    tenantService.deleteWorkspace(id).getOrThrow()
                                }
                            call.respond(result)
                        }
                    }
                }
            }
        }
    }
}
