package com.citacomsys.types

import kotlinx.serialization.Serializable

@Serializable
enum class ModuleCategory {
    CORE,           // Inventory, Sales, Purchases
    PEOPLE,         // Users, Customers
    ENGAGEMENT,     // SMS, Alerts, Reminders
    INTELLIGENCE,   // Reports, Analytics
    ENTERPRISE      // SIEM, Cross-branch
}

@Serializable
enum class ProductModule(val category: ModuleCategory) {
    // --- CORE ---
    INVENTORY(ModuleCategory.CORE),
    SALES(ModuleCategory.CORE),
    PURCHASES(ModuleCategory.CORE),

    // --- PEOPLE ---
    USER_MANAGEMENT(ModuleCategory.PEOPLE),
    CUSTOMERS(ModuleCategory.PEOPLE),

    // --- ENGAGEMENT ---
    AUTOMATED_ALERTS(ModuleCategory.ENGAGEMENT),
    CUSTOMER_CARE_SMS(ModuleCategory.ENGAGEMENT),

    // --- INTELLIGENCE ---
    REPORTS(ModuleCategory.INTELLIGENCE),

    // --- ENTERPRISE ---
    CROSS_BRANCH_SIEM_AUDIT(ModuleCategory.ENTERPRISE)
}

@Serializable
enum class ProductType(val isActive: Boolean = false) {
    PHARMACY(true),
    RETAIL(true),
    RESTAURANT,
    AUTOMOTIVE,
    AGRICULTURE,
    INVENTORY; // Admin and system info

    companion object {
        /**
         * Safely resolves a ProductType from a string, defaulting to RETAIL if unknown.
         */
        fun safeValueOf(value: String?): ProductType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: RETAIL
        }
    }
}