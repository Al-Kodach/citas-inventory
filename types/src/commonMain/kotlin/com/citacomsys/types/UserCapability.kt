package com.citacomsys.types

import kotlinx.serialization.Serializable

@Serializable
enum class UserCapability {
    // Platform Level Management
    PACKAGE_MANAGEMENT,
    VIEW_PACKAGE,
    MANAGE_TENANTS,
    VIEW_TENANTS,
    VIEW_ORGANIZATIONS,
    MANAGE_ORGANIZATIONS,
    MANAGE_WORKSPACES,

    // Endpoints / Assets (Split for Support vs Security)
    VIEW_DEVICES,    // For Support/Security visibility
    MANAGE_DEVICES,  // For Admin/Security destructive actions (Revoke/Rename)

    // Identity & Access
    VIEW_USERS,
    MANAGE_USERS,
    VIEW_SALES,

    // Inventory Management
    MANAGE_INVENTORY,
    VIEW_INVENTORY,
    MANAGE_INVENTORY_PRICES, // Permission to change Selling Prices
    CORRECT_STOCK_COSTS,     // Permission to adjust landing costs/discrepancies

    // Sales & Operations
    PROCESS_SALES,
    VIEW_POS,

    // Finance & Subscriptions
    MANAGE_BILLING, // Access to invoices, payments, and account balance

    // Security & Compliance (SOC)
    VIEW_REPORTS,         // General operational reports
    VIEW_ADVANCED_ANALYTICS, // Premium trend tracking and forecasting
    VIEW_SECURITY_AUDITS, // Access to raw audit logs
    VIEW_SIEM,           // Access to Security Hub, Threat Feed, and Request Tracing

    // System Configuration
    MANAGE_SETTINGS,        // General app/workspace settings
    ACCESS_SYSTEM_SETTINGS, // Global platform/security policies
    MANAGE_SYSTEM_INFRASTRUCTURE,

    // Customer Care
    VIEW_CUSTOMERS,
    MANAGE_CUSTOMERS,

    // Branch Management
    VIEW_BRANCHES,
    MANAGE_BRANCHES;

    /**
     * Determines if this capability can be assigned to a standard Tenant Role.
     */
    fun isTenantApplicable(): Boolean = when (this) {
        PACKAGE_MANAGEMENT,
        VIEW_PACKAGE,
        MANAGE_TENANTS,
        VIEW_TENANTS,
        ACCESS_SYSTEM_SETTINGS,
        MANAGE_SYSTEM_INFRASTRUCTURE -> false
        else -> true
    }
}
