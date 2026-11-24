package com.citacomsys.device.di

import org.koin.dsl.module

val deviceServiceModule = module {
    includes(platformModule())
}