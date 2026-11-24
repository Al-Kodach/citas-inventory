package com.citacomsys.core.security

import com.citacomsys.core.security.crypto.JvmSignatureProvider
import com.citacomsys.core.security.crypto.SignatureProvider
import com.citacomsys.core.security.hashing.JvmBCryptPasswordHasher
import com.citacomsys.shared.security.PasswordHasher
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<PasswordHasher> { JvmBCryptPasswordHasher() }
    single<SignatureProvider> {
        JvmSignatureProvider(
            get(named("vault"))
        )
    }
}