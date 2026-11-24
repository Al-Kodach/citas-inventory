package com.citacomsys.client.support.di

import com.citacomsys.client.support.dispatchers.DispatchersProvider
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformSupportModule(): Module = module {
    single { DispatchersProvider() }
}