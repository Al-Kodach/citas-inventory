package com.citacomsys.core.data.di

import com.citacomsys.core.data.file.PlatformFileSupport
import com.citacomsys.core.data.worker.DefaultPlatformWorkManagerEngine
import com.citacomsys.core.domain.file.PlatformFileSystem
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
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import java.io.File

@OptIn(DelicateCoroutinesApi::class)
actual fun platformModule(): Module = module {
    single {
        val baseDir = get<PlatformFileSystem>().getInternalDir()

        KSafe(
            fileName = "inv_prefs",
            baseDir = baseDir.toFile(),
            memoryPolicy = KSafeMemoryPolicy.PLAIN_TEXT
        )
    }

    single(named("vault")) {
        val baseDir = get<PlatformFileSystem>().getInternalDir()
        KSafe(
            fileName = "inv_vault",
            memoryPolicy = KSafeMemoryPolicy.LAZY_PLAIN_TEXT,
            baseDir = baseDir.toFile(),
            securityPolicy = KSafeSecurityPolicy(
                rootedDevice = SecurityAction.BLOCK,
                debuggerAttached = SecurityAction.BLOCK,
                debugBuild = SecurityAction.WARN,
                emulator = SecurityAction.WARN,
                onViolation = { violation ->
                    // Publish to EventBus instead of calling logger directly.
                    // This avoids circular dependencies and StackOverflow during DI startup.
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
        PlatformFileSupport(get())
    }

    single<PlatformWorkManagerEngine> {
        DefaultPlatformWorkManagerEngine(
            connectivityService = get(),
            scope = get()
        )
    } bind StartupTask::class
}
