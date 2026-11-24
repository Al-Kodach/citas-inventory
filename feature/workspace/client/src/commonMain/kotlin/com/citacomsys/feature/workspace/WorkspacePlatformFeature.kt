package com.citacomsys.feature.workspace

import com.citacomsys.core.spi.FeatureNavigationPlugin
import com.citacomsys.core.spi.CoreFeature
import com.citacomsys.feature.workspace.di.workspaceFeatureModule
import org.koin.core.Koin

class WorkspaceGateFeature : CoreFeature {
    override val id: String = "core.workspace.gate"
    override val navigationPlugin: FeatureNavigationPlugin = WorkspaceGateNavigationPlugin()

    override fun activate(koin: Koin) {
        koin.loadModules(listOf(workspaceFeatureModule))
    }

    override fun deactivateService(koin: Koin) {
        koin.unloadModules(listOf(workspaceFeatureModule))
    }
}

class WorkspaceActiveFeature : CoreFeature {
    override val id: String = "core.workspace.active"
    override val navigationPlugin: FeatureNavigationPlugin = WorkspaceActiveNavigationPlugin()

    override fun activate(koin: Koin) {
        koin.loadModules(listOf(workspaceFeatureModule))
    }

    override fun deactivateService(koin: Koin) {
        koin.unloadModules(listOf(workspaceFeatureModule))
    }
}
