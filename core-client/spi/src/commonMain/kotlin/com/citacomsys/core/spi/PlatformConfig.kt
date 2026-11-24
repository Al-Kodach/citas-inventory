package com.citacomsys.core.spi

import com.citacomsys.shared.AppConfig

/**
 * Advanced configuration for the platform shell.
 */
interface PlatformConfig : AppConfig {
    /**
     * If true, the platform will bypass certain checks for demo purposes.
     */
    val isDemoMode: Boolean get() = false

    /**
     * If true, the platform will skip onboarding if organization data is pre-seeded.
     */
    val skipOnboarding: Boolean get() = false
    
    /**
     * The target environment (e.g., "dev", "prod").
     */
    val environment: String get() = "prod"
}
