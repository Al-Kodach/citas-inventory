package com.citacomsys.client.customer.ui

import com.citacomsys.client.customer.customerClientServiceDi
import com.citacomsys.client.customer.data.ClientCustomerRepository
import com.citacomsys.client.customer.sync.CustomerSyncHandler
import com.citacomsys.core.spi.CoreFeature
import com.citacomsys.core.domain.repository.GlobalSearchRepository
import com.citacomsys.core.sync.engine.EntitySyncEngine
import com.citacomsys.core.spi.FeatureNavigationPlugin
import org.koin.core.Koin

class CustomerCoreFeature : CoreFeature {
    override val id: String = "core.customerState"

    override val navigationPlugin: FeatureNavigationPlugin = CustomerFeatureNavigationPlugin()

    override fun activate(koin: Koin) {
        koin.loadModules(listOf(customerClientServiceDi))
        
        try {
            koin.get<GlobalSearchRepository>()
                .registerProvider(koin.get<ClientCustomerRepository>())
        } catch (_: Throwable) {}
    }

    override fun deactivateService(koin: Koin) {
        koin.unloadModules(listOf(customerClientServiceDi))
    }
}
