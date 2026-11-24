package com.citacomsys.organization

import com.citacomsys.api.organization.OrganizationRepository
import com.citacomsys.api.organization.OrganizationService
import com.citacomsys.organization.infra.gateway.organizationOnboardRoute
import com.citacomsys.organization.infra.gateway.organizationRoute
import com.citacomsys.organization.infra.repository.ExposedOrganizationRepository
import com.citacomsys.organization.service.OrganizationReportService
import com.citacomsys.organization.service.ServerOrganizationService
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import org.koin.core.context.loadKoinModules
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val serverOrganizationServiceModule = module {
    singleOf(::ExposedOrganizationRepository) bind OrganizationRepository::class
    singleOf(::ServerOrganizationService) bind OrganizationService::class
    singleOf(::OrganizationReportService)
}

fun Application.configureOrganizationService() {
    loadKoinModules(serverOrganizationServiceModule)

    routing {
        organizationRoute()
        organizationOnboardRoute()
    }
}
