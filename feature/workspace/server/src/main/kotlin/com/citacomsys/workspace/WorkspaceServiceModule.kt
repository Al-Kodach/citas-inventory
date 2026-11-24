package com.citacomsys.workspace

import com.citacomsys.workspace.infra.gateway.branchRoute
import com.citacomsys.workspace.infra.gateway.workspaceProvisioningRoute
import com.citacomsys.workspace.infra.gateway.workspaceRoute
import com.citacomsys.workspace.infra.workspaceServiceModule
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import org.koin.core.context.loadKoinModules

fun Application.configureWorkspaceModule() {
    loadKoinModules(listOf(workspaceServiceModule))

    routing {
        workspaceRoute()
        workspaceProvisioningRoute()
        branchRoute()
    }
}
