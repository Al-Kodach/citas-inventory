package com.citacomsys.inventory.home.server

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val homeModule = module {
    singleOf(::DashboardService)
}