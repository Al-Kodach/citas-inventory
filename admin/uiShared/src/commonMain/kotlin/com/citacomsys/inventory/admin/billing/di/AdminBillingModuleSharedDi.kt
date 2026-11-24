package com.citacomsys.inventory.admin.billing.di

import com.citacomsys.admin.api.billing.AdminBillingApiService
import com.citacomsys.inventory.admin.billing.remote.KtorAdminBillingServiceApi
import com.citacomsys.inventory.admin.billing.ui.AdminBillingDetailViewModel
import com.citacomsys.inventory.admin.billing.ui.AdminBillingViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val adminBillingSharedModule = module {
    single<AdminBillingApiService> { KtorAdminBillingServiceApi(get()) }
    viewModelOf(::AdminBillingViewModel)
    viewModelOf(::AdminBillingDetailViewModel)
}