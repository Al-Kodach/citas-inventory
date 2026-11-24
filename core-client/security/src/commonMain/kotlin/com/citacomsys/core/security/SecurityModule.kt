package com.citacomsys.core.security

import com.citacomsys.core.domain.storage.TokenStore
import com.citacomsys.core.domain.security.ActionConfirmationManager
import com.citacomsys.core.domain.security.WorkspaceResourceQuotaGuard
import com.citacomsys.core.security.sudo.DefaultACTnManager
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

expect fun platformModule(): Module

val sharedSecurityModule = module {
    includes(platformModule())
    singleOf(::DefaultACTnManager) {
        bind<ActionConfirmationManager>()
    }
    single<TokenStore> {
        TokenStoreImpl(
            scope = get(),
            appConfig = get(),
            vault = get(named("vault"))
        )
    }
}

val tenantSecurityModule = module {
    includes(sharedSecurityModule)
    single<OfflineAuthenticator> {
        OfflineAuthenticatorImpl(
            vault = get(named("vault")),
            prefs = get(),
            passwordHasher = get()
        )
    }

    singleOf(::DefaultWorkspaceResourceQuotaGuard) {
        bind<WorkspaceResourceQuotaGuard>()
    }
}