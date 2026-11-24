package com.citacomsys.api.theme

import com.citacomsys.types.BrandingConfig
import kotlinx.coroutines.flow.StateFlow

interface ThemeService {

    val isDarkTheme: StateFlow<Boolean?>

    suspend fun load()

    suspend fun applyBranding(branding: BrandingConfig)

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun refresh()
}