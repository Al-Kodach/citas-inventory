package com.citacomsys.feature.organization

import com.citacomsys.core.spi.FeatureNavigationPlugin
import com.citacomsys.core.spi.CoreFeature
import com.citacomsys.feature.organization.di.organizationFeatureModule
import org.koin.core.Koin

class OrganizationCoreFeature : CoreFeature {
    override val id: String = "organization.feature"

    override val navigationPlugin: FeatureNavigationPlugin = OrganizationNavigationPlugin()

    override fun activate(koin: Koin) {
        koin.loadModules(listOf(organizationFeatureModule))
    }

    override fun deactivateService(koin: Koin) {
        koin.unloadModules(listOf(organizationFeatureModule))
    }
}
