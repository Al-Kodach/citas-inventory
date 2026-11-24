package com.citacomsys.retail

import com.citacomsys.core.persistence.plugin.transactionManager
import com.citacomsys.core.security.plugin.Authorize
import com.citacomsys.core.security.plugin.SecurityScope
import com.citacomsys.core.security.plugin.resolvedWorkspaceId
import com.citacomsys.retail.api.model.RetailProduct
import com.citacomsys.retail.service.RetailProductService
import com.citacomsys.types.PageQuery
import com.citacomsys.types.UserCapability
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import org.koin.java.KoinJavaComponent.inject

fun Route.retailInventory() {
    val service: RetailProductService by inject(RetailProductService::class.java)

    route("api/v1/inventory/retail") {
        authenticate("access-auth", "device-auth") {
            install(Authorize) {
                scope = SecurityScope.WORKSPACE
                requireCapabilities = setOf(UserCapability.MANAGE_INVENTORY)
            }

            get("/products") {
                val workspaceId =
                    call.request.queryParameters["activeWorkspaceId"] ?: call.resolvedWorkspaceId
                val query = call.request.queryParameters

                val pageQuery = PageQuery(
                    page = query["page"]?.toIntOrNull() ?: 1,
                    size = query["size"]?.toIntOrNull() ?: 50,
                    search = query["search"]
                )
                val result = call.transactionManager.runReadOnly {
                    service.fetch(workspaceId, pageQuery)
                }
                call.respond(result)
            }

            post("products/query") {
                val workspaceId =
                    call.request.queryParameters["activeWorkspaceId"] ?: call.resolvedWorkspaceId
                val pageQuery = call.receive<PageQuery>()
                val result = call.transactionManager.runReadOnly {
                    service.fetch(workspaceId, pageQuery)
                }
                call.respond(result)
            }

            get("/products/{id}") {
                val id = call.parameters["id"]!!
                val workspaceId =
                    call.request.queryParameters["activeWorkspaceId"] ?: call.resolvedWorkspaceId
                val result = call.transactionManager.runReadOnly {
                    service.getById(id, workspaceId) ?: error("Product not found")
                }
                call.respond(result)
            }

            post("/product") {
                val product = call.receive<RetailProduct>()
                val wsId = call.resolvedWorkspaceId

                call.transactionManager.runInTransaction {
                    service.saveProduct(product, wsId)
                }

                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
