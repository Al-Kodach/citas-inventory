package com.citacomsys.shared.util

import com.citacomsys.types.PlatformAdminRole
import com.citacomsys.types.ProductType
import com.citacomsys.types.TenantUserRole

/**
 * Safely converts a raw role string into a human-readable display ID.
 * Handles both Tenant and Platform roles to prevent crashes during list rendering.
 */
fun String.toRoleDisplayName(businessType: ProductType): String {

    // Check if it's a standard Tenant Role
    try {
        val normalized = this.removePrefix("TENANT_")
        return TenantUserRole.valueOf(normalized).displayName(businessType)
    } catch (_: Exception) {}

    // Check if it's a Platform Admin Role
    try {
        val normalized = this.removePrefix("PLATFORM_")
        return PlatformAdminRole.valueOf(normalized).name.replace("_", " ").lowercase()
            .replaceFirstChar { it.uppercase() }
    } catch (_: Exception) {}

    // Fallback to raw string formatted nicely
    return this.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
}
