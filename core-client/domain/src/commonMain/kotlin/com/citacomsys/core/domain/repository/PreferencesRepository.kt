package com.citacomsys.core.domain.repository

import com.citacomsys.types.BrandingConfig
import kotlinx.coroutines.flow.StateFlow

interface PreferencesRepository {
    val isDarkTheme: StateFlow<Boolean?>
    val userFontScale: StateFlow<Int>
    val useOrgBranding: StateFlow<Boolean>
    val usePlatformBranding: StateFlow<Boolean>
    val activeBranding: StateFlow<BrandingConfig?>

    fun toggleTheme()
    fun nextFontScale()
    fun setFontScale(scale: Int)
    fun setUseOrgBranding(useOrg: Boolean)
    fun setUsePlatformBranding(usePlatform: Boolean)
    fun setActiveBranding(branding: BrandingConfig?)
    fun setLastEventSeq(seq: Long)
    fun getLastEventSeq(): Long
}
