package com.citacomsys.core.data.di

import com.citacomsys.core.data.file.PlatformFileSupport
import com.citacomsys.core.data.worker.AndroidPlatformWorkManagerEngine
import com.citacomsys.core.spi.PlatformWorkManagerEngine
import com.citacomsys.core.spi.StartupTask
import com.citacomsys.shared.event.EventBus
import com.citacomsys.shared.event.SecurityViolationEvent
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeMemoryPolicy
import eu.anifantakis.lib.ksafe.KSafeSecurityPolicy
import eu.anifantakis.lib.ksafe.SecurityAction
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

@OptIn(DelicateCoroutinesApi::class)
actual fun platformModule(): Module = module {
    single<PlatformWorkManagerEngine> {
        AndroidPlatformWorkManagerEngine(androidApplication())
    } bind StartupTask::class

    single {
        KSafe(
            context = androidApplication(),
            fileName = "prefs",
            memoryPolicy = KSafeMemoryPolicy.PLAIN_TEXT
        )
    }

    single(named("vault")) {
        KSafe(
            context = androidApplication(),
            fileName = "vault",

            securityPolicy = KSafeSecurityPolicy(
                rootedDevice = SecurityAction.BLOCK,      // IGNORE, WARNING, or BLOCK
                debuggerAttached = SecurityAction.BLOCK,
                debugBuild = SecurityAction.WARN,
                emulator = SecurityAction.IGNORE,
                onViolation = { violation ->
                    GlobalScope.launch {
                        EventBus.publish(
                            SecurityViolationEvent(
                                violation = violation.name,
                                reason = getViolationDescription(violation)
                            )
                        )
                    }
                }
            )
        )
    }

    single {
        PlatformFileSupport(
            context = androidApplication(),
            appConfig = get()
        )
    }
}
