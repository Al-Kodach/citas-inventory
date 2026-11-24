package com.citacomsys.inventory.admin.app.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object AdminDashboardNavKey: NavKey
@Serializable data object AdminSettingsNavKey: NavKey
@Serializable data object AdminUserProfileNav: NavKey
@Serializable data object AdminSystemModuleNavKey: NavKey

@Serializable data class AdminDeviceMetadataNav(val deviceId: String) : NavKey