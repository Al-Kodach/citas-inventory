package com.citacomsys.core.security

import com.citacomsys.core.security.crypto.AndroidSignatureProvider
import com.citacomsys.core.security.crypto.SignatureProvider
import com.citacomsys.core.security.hashing.AndroidBCryptPasswordHasher
import com.citacomsys.shared.security.PasswordHasher
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<PasswordHasher> { AndroidBCryptPasswordHasher() }
    single<SignatureProvider> { AndroidSignatureProvider() }
}