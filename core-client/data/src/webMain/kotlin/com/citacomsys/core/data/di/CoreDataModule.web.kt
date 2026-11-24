package com.citacomsys.core.data.di

import com.citacomsys.core.data.file.PlatformFileSupport
import com.citacomsys.core.data.worker.DefaultPlatformWorkManagerEngine
import com.citacomsys.core.spi.PlatformWorkManagerEngine
import com.citacomsys.core.spi.StartupTask
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeMemoryPolicy
import eu.anifantakis.lib.ksafe.KSafeSecurityPolicy
import eu.anifantakis.lib.ksafe.SecurityAction
import io.github.aakira.napier.Napier
import io.ktor.client.utils.clientDispatcher
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single {
        KSafe(
            fileName = "prefs",
            memoryPolicy = KSafeMemoryPolicy.PLAIN_TEXT
        )
    }

    single(named("vault")) {
        KSafe(
            fileName = "vault",
            securityPolicy = KSafeSecurityPolicy(
                rootedDevice = SecurityAction.BLOCK,      // IGNORE, WARNING, or BLOCK
                debuggerAttached = SecurityAction.BLOCK,
                debugBuild = SecurityAction.WARN,
                emulator = SecurityAction.IGNORE,
                onViolation = { violation ->
                    Napier.e("Security: ${violation.name}")
                }
            )
        )
    }

    single {
        PlatformFileSupport()
    }

    single<PlatformWorkManagerEngine> {
        DefaultPlatformWorkManagerEngine(
            connectivityService = get(),
            scope = get()
        )
    } bind StartupTask::class
}

@OptIn(InternalAPI::class)
@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual fun ioDispatcher(): CoroutineDispatcher = Dispatchers.clientDispatcher(3, "IO")

actual fun defaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
