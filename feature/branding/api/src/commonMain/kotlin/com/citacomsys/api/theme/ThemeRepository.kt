package com.citacomsys.api.theme

import kotlinx.coroutines.flow.Flow

interface ThemeRepository {

    fun observeBranding(): Flow<BrandingConfig?>

    suspend fun getBranding(): BrandingConfig?

    suspend fun saveBranding(
        branding: BrandingConfig
    )

    suspend fun synchronize()
}