package com.citacomsys.types

object DefaultCapabilities {

    fun forRole(roleStr: String): Set<UserCapability> {
        val cleanRole = roleStr.uppercase()
            .removePrefix("PLATFORM_")
            .removePrefix("TENANT_")

        val platformRole = runCatching { PlatformAdminRole.valueOf(cleanRole) }.getOrNull()
        if (platformRole != null) return forRole(platformRole)

        val tenantRole = runCatching { TenantUserRole.valueOf(cleanRole) }.getOrNull()
        if (tenantRole != null) return forRole(tenantRole)

        return emptySet()
    }

    fun forRole(role: TenantUserRole): Set<UserCapability> = when (role) {
        TenantUserRole.OWNER -> UserCapability.entries.toSet()
//        - setOf(UserCapability.PROCESS_SALES) // Testing

        TenantUserRole.MANAGER -> setOf(
            UserCapability.MANAGE_USERS,
            UserCapability.VIEW_USERS,
            UserCapability.MANAGE_INVENTORY,
            UserCapability.VIEW_INVENTORY,
            UserCapability.MANAGE_INVENTORY_PRICES,
            UserCapability.VIEW_REPORTS,
            UserCapability.VIEW_ADVANCED_ANALYTICS,
            UserCapability.VIEW_CUSTOMERS,
            UserCapability.MANAGE_CUSTOMERS,
            UserCapability.VIEW_BRANCHES,
            UserCapability.MANAGE_BRANCHES,
            UserCapability.PROCESS_SALES,
            UserCapability.VIEW_SALES,
        )

        TenantUserRole.ADMIN -> setOf(
            UserCapability.MANAGE_INVENTORY,
            UserCapability.VIEW_INVENTORY,
            UserCapability.MANAGE_INVENTORY_PRICES,
            UserCapability.PROCESS_SALES,
            UserCapability.VIEW_POS,
            UserCapability.VIEW_REPORTS,
            UserCapability.VIEW_ADVANCED_ANALYTICS,
            UserCapability.VIEW_CUSTOMERS,
            UserCapability.MANAGE_CUSTOMERS,
            UserCapability.VIEW_BRANCHES,
            UserCapability.MANAGE_BRANCHES,
            UserCapability.VIEW_SALES,
            UserCapability.VIEW_USERS,
        )

        TenantUserRole.OPERATOR -> setOf(
            UserCapability.PROCESS_SALES,
            UserCapability.VIEW_INVENTORY,
            UserCapability.VIEW_REPORTS,
            UserCapability.VIEW_CUSTOMERS,
            UserCapability.MANAGE_CUSTOMERS,
            UserCapability.VIEW_SALES
        )

        TenantUserRole.SECURITY -> setOf(
            UserCapability.VIEW_REPORTS,
            UserCapability.VIEW_SECURITY_AUDITS,
            UserCapability.VIEW_DEVICES
        )
    }

    fun forRole(role: PlatformAdminRole): Set<UserCapability> = when (role) {
        PlatformAdminRole.SUPER_ADMIN -> UserCapability.entries.toSet()

        PlatformAdminRole.POLICY_ADMIN -> setOf(
            UserCapability.MANAGE_SETTINGS,
            UserCapability.ACCESS_SYSTEM_SETTINGS,
            UserCapability.VIEW_SECURITY_AUDITS,
            UserCapability.VIEW_SIEM
        )

        PlatformAdminRole.DEVELOPER -> setOf(
            UserCapability.VIEW_PACKAGE,
            UserCapability.VIEW_TENANTS,
            UserCapability.VIEW_USERS,
            UserCapability.VIEW_SECURITY_AUDITS,
            UserCapability.VIEW_SIEM,
            UserCapability.MANAGE_SYSTEM_INFRASTRUCTURE
        )

        PlatformAdminRole.SUPPORT -> setOf(
            UserCapability.VIEW_PACKAGE,
            UserCapability.VIEW_TENANTS,
            UserCapability.VIEW_DEVICES,
            UserCapability.VIEW_USERS,
            UserCapability.MANAGE_USERS,
            UserCapability.VIEW_REPORTS,
            UserCapability.VIEW_CUSTOMERS
        )

        PlatformAdminRole.SECURITY -> setOf(
            UserCapability.VIEW_SECURITY_AUDITS,
            UserCapability.VIEW_SIEM,
            UserCapability.MANAGE_DEVICES,
            UserCapability.ACCESS_SYSTEM_SETTINGS
        )

        PlatformAdminRole.READ_ONLY -> setOf(
            UserCapability.VIEW_PACKAGE,
            UserCapability.VIEW_TENANTS,
            UserCapability.VIEW_USERS,
            UserCapability.VIEW_INVENTORY,
            UserCapability.VIEW_REPORTS,
        )
    }
}
