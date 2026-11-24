package com.citacomsys.core.database.di

import com.citacomsys.core.database.DriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module {
    return module {
        single<DriverFactory> { DriverFactory() }
    }
}