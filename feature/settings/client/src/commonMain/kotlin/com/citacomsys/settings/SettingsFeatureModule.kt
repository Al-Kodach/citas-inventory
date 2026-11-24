package com.citacomsys.settings

import com.citacomsys.feature.settings.api.UserSecurityApi
import com.citacomsys.settings.data.remote.api.KtorUserSecurityApi
import com.citacomsys.settings.ui.mesh.MeshDiagnosticsViewModel
import com.citacomsys.settings.ui.mesh.MeshDiscoveryViewModel
import com.citacomsys.settings.ui.mesh.MeshNodePairingCenterViewModel
import com.citacomsys.settings.ui.screen.profile.user.UserChangePasswordViewModel
import com.citacomsys.settings.ui.screen.profile.user.UserProfileViewModel
import com.citacomsys.settings.ui.screen.settings.SettingsViewModel
import com.citacomsys.settings.ui.screen.software.SoftwareInfoViewModel
import com.citacomsys.settings.ui.sync.SyncPolicyViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val settingsFeatureModule = module {
    single<UserSecurityApi> { KtorUserSecurityApi(get()) }
    singleOf(::SettingsViewModel)
    viewModelOf(::UserProfileViewModel)
    viewModelOf(::UserChangePasswordViewModel)
    viewModelOf(::MeshDiagnosticsViewModel)
    viewModelOf(::MeshDiscoveryViewModel)
    viewModelOf(::MeshNodePairingCenterViewModel)
    viewModelOf(::SyncPolicyViewModel)
    viewModelOf(::SoftwareInfoViewModel)
}
