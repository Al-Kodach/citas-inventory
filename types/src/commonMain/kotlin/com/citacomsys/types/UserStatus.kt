package com.citacomsys.types

enum class UserStatus {
    ACTIVE,
    SUSPENDED,
    DELETED,
    REQUIRED_PHONE_VERIFICATION
}

/**
 * Generic Tiered Roles for Tenant Workspaces.
 * These align with the business kernel and are mapped to business-specific 
 * terms in the UI layer.
 */
enum class TenantUserRole {
    OWNER,      // Root authority for the entire workspace
    MANAGER,    // Operational leader with inventory/staff management
    ADMIN,
    OPERATOR,   // Front-line execution (Sales, Inventory view)
    SECURITY;    // Audit and compliance focus

    /**
     * Resolves the human-readable alias for a role based on the business context.
     */
    fun displayName(type: ProductType): String = when (this) {
        OWNER -> "Business Owner"
        MANAGER -> "Business Manager"
        ADMIN -> when(type) {
            else -> "Store Manager"
        }
        OPERATOR -> when (type) {
            ProductType.PHARMACY -> "Pharmacist / Dispenser"
            ProductType.RETAIL -> "Sales Attendant"
            ProductType.AGRICULTURE -> "Inventory Clerk"
            else -> "Operations Staff"
        }
        SECURITY -> "Compliance Officer"
    }
}

enum class PlatformAdminRole {
    SUPER_ADMIN,
    POLICY_ADMIN,
    DEVELOPER,
    SUPPORT,
    READ_ONLY,
    SECURITY
}
