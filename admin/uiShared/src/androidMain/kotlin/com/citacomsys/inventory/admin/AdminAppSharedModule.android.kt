package com.citacomsys.inventory.admin

import com.citacomsys.core.data.di.getViolationDescription
import com.citacomsys.core.data.file.PlatformFileSupport
import com.citacomsys.shared.event.EventBus
import com.citacomsys.shared.event.SecurityViolationEvent
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeMemoryPolicy
import eu.anifantakis.lib.ksafe.KSafeSecurityPolicy
import eu.anifantakis.lib.ksafe.SecurityAction
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

actual fun platformModule(): Module = module {
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
                    // Publish to EventBus instead of calling logger directly.
                    // This avoids circular dependencies during DI startup.
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