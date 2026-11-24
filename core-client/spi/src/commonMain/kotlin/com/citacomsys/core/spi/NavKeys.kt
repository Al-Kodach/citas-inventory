package com.citacomsys.core.spi

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Root navigation key for the entire platform.
 * Using Explicit SerialNames to prevent discriminator collisions across modules.
 */

// --- CORE MODULES (Shared across Admin and Pharmacy) ---
@Serializable
data object UserProfileNav : NavKey

@Serializable
data object SyncManagerNav: NavKey

@Serializable
data object TelemetryNav: NavKey

@Serializable
data object TenantSecurityNav: NavKey


@Serializable
data object BackupNav : NavKey

@Serializable
data object InventoryNav : NavKey


// --- STANDALONE / UNGROUPED ---

@SerialName("standalone.staff")
@Serializable
data object StaffNav : NavKey

@SerialName("standalone.staff.form")
@Serializable
data class UserFormNav(val userId: String? = null) : NavKey

@SerialName("standalone.staff.details")
@Serializable
data class UserDetailNav(val userId: String) : NavKey

@SerialName("standalone.staff.workspace.binding")
@Serializable
data object UserWorkspaceBindingNav : NavKey

@SerialName("standalone.mesh.diagnostics")
@Serializable
data object MeshDiagnosticsNav : NavKey

@SerialName("standalone.mesh.discovery")
@Serializable
data object MeshDiscoveryNav : NavKey

@SerialName("standalone.mesh.pairing_center")
@Serializable
data object MeshNodePairingCenterNav : NavKey

@SerialName("standalone.sync.policy")
@Serializable
data object SyncPolicyNav : NavKey

@SerialName("standalone.legal")
@Serializable
data object LegalNav : NavKey

@SerialName("standalone.software.info")
@Serializable
data object SoftwareInfoNav : NavKey
