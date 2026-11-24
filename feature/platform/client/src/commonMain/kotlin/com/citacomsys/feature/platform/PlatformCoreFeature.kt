package com.citacomsys.feature.platform

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.citacomsys.core.spi.FeatureNavigationPlugin
import com.citacomsys.core.spi.CoreFeature
import com.citacomsys.feature.platform.di.platformFeatureModule
import com.citacomsys.core.spi.Navigator
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.Koin

class PlatformCoreFeature : CoreFeature {
    override val id: String = "platform.module"

    override val navigationPlugin: FeatureNavigationPlugin = object : FeatureNavigationPlugin {
        override val id = "platform.module"

        override val serializersModule = SerializersModule {}

        override fun EntryProviderScope<NavKey>.registerEntries(
            navigator: Navigator,
            isCompact: Boolean
        ) {}
    }

    override fun activate(koin: Koin) {
        koin.loadModules(listOf(platformFeatureModule))
    }
}
