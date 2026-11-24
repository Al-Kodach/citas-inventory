package com.citacomsys.module.server.customer

import com.citacomsys.module.api.customer.CustomerService
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

val customerServiceModuleDi = module {
    single<CustomerService> { CustomerServiceImpl() }
}

fun Application.configureCustomerServiceModule() {
    loadKoinModules(customerServiceModuleDi)

    routing {
        customerRoutes()
    }
}