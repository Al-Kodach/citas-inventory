package com.citacomsys.feature.support.server.infra.gateway

import com.citacomsys.feature.support.api.AddMessageRequest
import com.citacomsys.feature.support.api.CreateTicketRequest
import com.citacomsys.feature.support.api.TicketStatus
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.supportRoutes() {
    val controller = SupportController()

    route("/api/support") {
        get("/tickets") {
            val workspaceId = call.request.queryParameters["activeWorkspaceId"] ?: "default-workspace"
            val tickets = controller.getTickets(workspaceId)
            call.respond(tickets)
        }

        get("/tickets/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val ticket = controller.getTicket(id)
            if (ticket != null) {
                call.respond(ticket)
            } else {
                call.respond(io.ktor.http.HttpStatusCode.NotFound)
            }
        }

        post("/tickets") {
            val request = call.receive<CreateTicketRequest>()
            val created = controller.createTicket(request)
            call.respond(created)
        }

        post("/tickets/message") {
            val request = call.receive<AddMessageRequest>()
            val message = controller.addMessage(request)
            call.respond(message)
        }

        patch("/tickets/{id}/status") {
            val id = call.parameters["id"] ?: return@patch call.respond(io.ktor.http.HttpStatusCode.BadRequest)
            val statusParam = call.request.queryParameters["status"] ?: "OPEN"
            val status = TicketStatus.valueOf(statusParam)
            val updated = controller.updateStatus(id, status)
            call.respond(updated)
        }
    }
}
