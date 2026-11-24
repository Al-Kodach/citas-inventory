package com.citacomsys.feature.auth

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.citacomsys.core.spi.FeatureNavigationPlugin
import com.citacomsys.core.spi.CoreFeature
import com.citacomsys.feature.auth.di.authFeatureModule
import com.citacomsys.core.spi.Navigator
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.Koin

class AuthCoreFeature : CoreFeature {
    override val id: String = "core.auth"
    
    // Auth doesn't have a navigation plugin in this design, it's a "gate" feature
    override val navigationPlugin: FeatureNavigationPlugin = object : FeatureNavigationPlugin {
        override val id: String = "auth"

        override val serializersModule = SerializersModule {}

        override fun EntryProviderScope<NavKey>.registerEntries(
            navigator: Navigator,
            isCompact: Boolean
        ) {}
    }

    override fun activate(koin: Koin) {
        koin.loadModules(listOf(authFeatureModule))
    }

    override fun deactivateService(koin: Koin) {}
}
