package com.citacomsys.user.di

import com.citacomsys.user.data.ClientUserRepository
import com.citacomsys.user.data.ClientUserRepositoryImpl
import com.citacomsys.user.data.remote.api.UserApiService
import com.citacomsys.user.data.remote.api.impl.KtorUserApiService
import com.citacomsys.user.ui.ActiveUsersViewModel
import com.citacomsys.user.ui.screen.TenantUserListViewModel
import com.citacomsys.user.ui.screen.UserDetailViewModel
import com.citacomsys.user.ui.screen.UserFormViewModel
import com.citacomsys.user.ui.screen.UserWorkspaceBindingViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val userModule = module {
    single<UserApiService> { KtorUserApiService(get()) }

    singleOf(::ClientUserRepositoryImpl) {
        bind<ClientUserRepository>()
    }

    viewModelOf(::TenantUserListViewModel)
    viewModelOf(::UserFormViewModel)
    viewModelOf(::UserDetailViewModel)
    viewModelOf(::UserWorkspaceBindingViewModel)
    viewModelOf(::ActiveUsersViewModel)
}
