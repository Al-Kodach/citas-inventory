package com.citacomsys.feature.platform.gateway

import com.citacomsys.api.platform.repository.ProductPackageRepository
import com.citacomsys.api.platform.model.ProductPackageDetails
import com.citacomsys.api.platform.remote.ProductPackageService
import com.citacomsys.core.persistence.plugin.transactionManager
import com.citacomsys.core.security.plugin.Authorize
import com.citacomsys.core.security.plugin.userPrincipal
import com.citacomsys.core.security.plugin.resolvedWorkspaceId
import com.citacomsys.core.security.token.jwt.UserPrincipal
import com.citacomsys.types.UserCapability
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.packageRoute() {
    val repository: ProductPackageRepository by inject()
    val service: ProductPackageService by inject()

    authenticate("access-auth") {
        route("/packages") {
            get {
                val packages = call.transactionManager.runReadOnly {
                    repository.getAllPublic()
                }
                call.respond(packages)
            }

            get("/available") {
                val workspaceId = call.resolvedWorkspaceId

                val packages = call.transactionManager.runReadOnly {
                    service.getAvailableUpgrades(workspaceId)
                }
                call.respond(packages)
            }

            post("/save") {
                call.transactionManager.runInTransaction {
                    authorized(call)
                    val request = call.receive<ProductPackageDetails>()
                    repository.save(request).getOrThrow()
                }
                call.respond(Unit)
            }
        }
    }
}

private fun Route.authorized(call: ApplicationCall): UserPrincipal {
    install(Authorize) {
        requireCapabilities = setOf(UserCapability.PACKAGE_MANAGEMENT)
    }
    return call.userPrincipal()
}
