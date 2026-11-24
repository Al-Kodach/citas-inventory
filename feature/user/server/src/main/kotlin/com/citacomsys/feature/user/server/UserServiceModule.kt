package com.citacomsys.feature.user.server

import com.citacomsys.feature.user.server.adaptor.ExposedPlatformAdminRepository
import com.citacomsys.feature.user.server.adaptor.ExposedRoleRepository
import com.citacomsys.feature.user.server.adaptor.ExposedUserAdaptor
import com.citacomsys.feature.user.server.service.UserService
import com.citacomsys.inventory.api.user.PlatformAdminRepository
import com.citacomsys.inventory.api.user.RoleRepository
import com.citacomsys.inventory.api.user.UserPort
import com.citacomsys.inventory.api.user.UserRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val userServiceModule = module {
    single<ExposedUserAdaptor> { ExposedUserAdaptor() }
    single<UserPort> { get<ExposedUserAdaptor>() }
    single<RoleRepository> { ExposedRoleRepository() }
    single<PlatformAdminRepository> { ExposedPlatformAdminRepository() }

    singleOf(::UserService)

    single<UserRepository> { get<UserService>() }

    singleOf(::UserController)
}
