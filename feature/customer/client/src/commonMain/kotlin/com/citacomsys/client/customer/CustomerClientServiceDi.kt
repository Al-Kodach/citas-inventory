package com.citacomsys.client.customer

import com.citacomsys.client.customer.data.ClientCustomerRepository
import com.citacomsys.client.customer.data.CustomerDatabaseManager
import com.citacomsys.client.customer.data.SqlDelightCustomerLocalDao
import com.citacomsys.client.customer.data.remote.KtorCustomerService
import com.citacomsys.client.customer.mesh.CustomerHubGateway
import com.citacomsys.client.customer.sync.CustomerSyncHandler
import com.citacomsys.client.customer.ui.screen.ledger.CustomerDetailsViewModel
import com.citacomsys.client.customer.ui.screen.list.CustomerListViewModel
import com.citacomsys.client.customer.ui.screen.register.CustomerRegistrationViewModel
import com.citacomsys.core.mesh.gateway.HubServiceGateway
import com.citacomsys.core.sync.handler.EntitySyncHandler
import com.citacomsys.module.api.customer.CustomerRepository
import com.citacomsys.module.api.customer.CustomerService
import com.citacomsys.module.api.customer.dao.CustomerLocalDao
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val customerClientServiceDi = module {
    viewModelOf(::CustomerRegistrationViewModel)
    viewModelOf(::CustomerListViewModel)
    viewModelOf(::CustomerDetailsViewModel)
}

val customerClientInfraServiceDi = module {
    single<CustomerService> { KtorCustomerService(get()) }

    single {
        CustomerSyncHandler(
            service = get(),
            localDao = get(),
            sessionManager = get()
        )
    } bind EntitySyncHandler::class

    singleOf(::CustomerDatabaseManager)

    single<CustomerLocalDao> {
        SqlDelightCustomerLocalDao(get(), get())
    }

    singleOf(::CustomerHubGateway) bind HubServiceGateway::class
    singleOf(::ClientCustomerRepository) bind CustomerRepository::class
}
