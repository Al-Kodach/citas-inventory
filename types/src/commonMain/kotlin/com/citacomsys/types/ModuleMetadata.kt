package com.citacomsys.types

import kotlinx.serialization.Serializable

@Serializable
data class ModuleFeature(
    val title: String,
    val description: String,
    val isPremium: Boolean = false
)

fun ProductModule.displayName(): String = when (this) {
    ProductModule.INVENTORY -> "Inventory Management"
    ProductModule.SALES -> "Point of Sale (POS)"
    ProductModule.PURCHASES -> "Procurement & Logistics"
    ProductModule.USER_MANAGEMENT -> "Team Control"
    ProductModule.AUTOMATED_ALERTS -> "Security & Status Alerts"
    ProductModule.CUSTOMER_CARE_SMS -> "Customer Engagement & Reminders"
    ProductModule.CUSTOMERS -> "CRM & Customer History"
    ProductModule.CROSS_BRANCH_SIEM_AUDIT -> "Enterprise SIEM & Audit"
    ProductModule.REPORTS -> "Standard Reporting"
}

fun ProductModule.asFeatures(): List<ModuleFeature> = when (this) {
    ProductModule.INVENTORY -> listOf(
        ModuleFeature(
            "Real-time Inventory Ledger",
            "Track every single unit moving in or out of your store with absolute precision."
        ),
        ModuleFeature(
            "Automated Reorder Intelligence",
            "Smart alerts notify you before you run out of stock, ensuring business continuity."
        ),
        ModuleFeature(
            "Expiry Date Guardian",
            "Proactively identify batches nearing expiry. Save thousands by returning stock to suppliers early.",
            isPremium = true
        ),
        ModuleFeature(
            "Batch & SKU Management",
            "Organize products by manufacturer, strength, and variants. Full barcode scanning support."
        )
    )

    ProductModule.SALES -> listOf(
        ModuleFeature(
            "High-Speed Checkout",
            "A point-of-sale interface built for speed. Process transactions and print receipts effortlessly."
        ),
        ModuleFeature(
            "[Upcoming] Omni-Channel Payments",
            "Record Cash, Cards, and Mobile Money (Momo) payments with integrated reconciliation. (Direct processing coming soon)"
        ),
        ModuleFeature(
            "Cart Park & Resume",
            "Park a current sale to attend to an emergency and resume exactly where you left off."
        ),
        ModuleFeature(
            "Digital SMS Receipts",
            "Modernize your business. Send professional digital summaries directly to your customer's phone (SMS credit required).",
            isPremium = true
        )
    )

    ProductModule.PURCHASES -> listOf(
        ModuleFeature(
            "[Upcoming] Supplier Relationship Manager",
            "Maintain a full database of your suppliers, their lead times, and outstanding balances."
        ),
        ModuleFeature(
            "[Upcoming] Purchase Order Automation",
            "Generate professional POs based on low-stock alerts with one-click stock updates."
        ),
        ModuleFeature(
            "[Upcoming] Cost Price Variance Tracking",
            "Monitor how your purchase costs change over time to keep your margins profitable."
        )
    )

    ProductModule.AUTOMATED_ALERTS -> listOf(
        ModuleFeature(
            "Internal Security Alerts",
            "Receive instant SMS notifications for high-value sales, stock adjustments, or login attempts (SMS credit required).",
            isPremium = true
        )
    )
    
    ProductModule.CUSTOMER_CARE_SMS -> listOf(
        ModuleFeature(
            "Automated Reminders",
            "Identify recurring customers due for a visit and text them automatically (SMS credit required).",
            isPremium = true
        ),
        ModuleFeature(
            "Order Ready Alerts",
            "Notify customers instantly when their orders are ready for pickup (SMS credit required).",
            isPremium = true
        ),
        ModuleFeature(
            "Marketing Broadcasts",
            "Send bulk announcements about new arrivals or seasonal discounts (SMS credit required).",
            isPremium = true
        )
    )

    ProductModule.USER_MANAGEMENT -> listOf(
        ModuleFeature(
            "Role-Based Access Control",
            "Manage staff permissions with industry-specific generic roles (Manager, Operator)."
        )
    )

    ProductModule.CUSTOMERS -> listOf(
        ModuleFeature(
            "Unified Customer Profiles",
            "Maintain comprehensive records of your clients including contact details and purchase history."
        ),
        ModuleFeature(
            "Loyalty & Consent",
            "Track customer consent for notifications and manage communication preferences."
        )
    )

    ProductModule.REPORTS -> listOf(
        ModuleFeature(
            "Operational Reporting",
            "Standard daily, weekly, and monthly reports on sales, inventory, and staff performance."
        ),
        ModuleFeature(
            "Predictive Analytics",
            "Deep-dive into business performance with revenue forecasts and trend analysis (Pro/Enterprise Only).",
            isPremium = true
        )
    )

    ProductModule.CROSS_BRANCH_SIEM_AUDIT -> listOf(
        ModuleFeature(
            "Enterprise Security Operations",
            "Monitor all branch operations in real-time with centralized security event logging.",
            isPremium = true
        )
    )
}
