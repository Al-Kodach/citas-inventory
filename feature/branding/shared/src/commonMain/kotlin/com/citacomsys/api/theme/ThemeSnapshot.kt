package com.citacomsys.api.theme

import kotlinx.serialization.Serializable

@Serializable
data class ThemeSnapshot(
    val id: String,
    val branding: BrandingConfig,
    val version: Long,
    val updatedBy: String,
    val updatedAt: String,
)
