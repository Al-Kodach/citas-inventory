package com.citacomsys.shared

import com.citacomsys.types.ProductType

interface AppConfig {
    val appPrefix: String get() = "TNT"        // e.g., "ADM" or "TNT"
    val prefsPrefix: String get() = "tenant"        // e.g., "admin" or "tenant_client"
    val apiSecret: String get() = ""
}
