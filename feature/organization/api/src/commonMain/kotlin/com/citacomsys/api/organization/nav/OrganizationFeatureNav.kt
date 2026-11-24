package com.citacomsys.api.organization.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
sealed interface OrganizationFeatureNav : NavKey

@Serializable
data object OrganizationHubNav : OrganizationFeatureNav

@Serializable
data object WorkspaceListNav : OrganizationFeatureNav

@Serializable
data object OrganizationProfileNav : OrganizationFeatureNav

@Serializable
data class BranchManagementNav(val workspaceId: String = "") : OrganizationFeatureNav

@Serializable
data object OrganizationMonitoringNav : OrganizationFeatureNav

@Serializable
data object OrganizationSettingsNav : OrganizationFeatureNav

@Serializable
data object AuditNav : OrganizationFeatureNav

@Serializable
data object OrganizationReportsNav : OrganizationFeatureNav

@Serializable
data object OrganizationSiemAuditNav : OrganizationFeatureNav

@Serializable
data class WorkspaceDetailsNav(val workspaceId: String) : OrganizationFeatureNav

val organizationSerializersModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(OrganizationHubNav::class, OrganizationHubNav.serializer())
        subclass(WorkspaceListNav::class, WorkspaceListNav.serializer())
        subclass(OrganizationProfileNav::class, OrganizationProfileNav.serializer())
        subclass(BranchManagementNav::class, BranchManagementNav.serializer())
        subclass(OrganizationMonitoringNav::class, OrganizationMonitoringNav.serializer())
        subclass(OrganizationSettingsNav::class, OrganizationSettingsNav.serializer())
        subclass(AuditNav::class, AuditNav.serializer())
        subclass(OrganizationReportsNav::class, OrganizationReportsNav.serializer())
        subclass(OrganizationSiemAuditNav::class, OrganizationSiemAuditNav.serializer())
        subclass(WorkspaceDetailsNav::class, WorkspaceDetailsNav.serializer())
    }
}
