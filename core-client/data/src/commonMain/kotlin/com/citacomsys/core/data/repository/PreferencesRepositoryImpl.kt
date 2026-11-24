package com.citacomsys.core.data.repository

import com.citacomsys.core.domain.model.LastSyncMetadata
import com.citacomsys.core.domain.repository.PreferencesRepository
import com.citacomsys.shared.AppConfig
import com.citacomsys.types.BrandingConfig
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.asMutableStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal class PreferencesRepositoryImpl(
    appConfig: AppConfig,
    private val prefs: KSafe,
    scope: CoroutineScope
) : PreferencesRepository {
    private val isDark = appConfig.prefsPrefix + "_is_dark_theme"
    private val syncMeta = "${appConfig.prefsPrefix}_last_sync_metadata"
    private val fontKey = "${appConfig.prefsPrefix}_font_scale"
    private val lastSeqKey = "${appConfig.prefsPrefix}_lastSeq"

    private val isDarkMode by prefs.asMutableStateFlow<Boolean?>(null, scope, isDark)
    override val isDarkTheme: StateFlow<Boolean?> = isDarkMode.asStateFlow()

    override val userFontScale = prefs.getFlow(fontKey, 1)
        .stateIn(scope, SharingStarted.Eagerly, 1)

    override fun toggleTheme() {
        isDarkMode.value = isDarkMode.value != true
    }

    override val useOrgBranding: StateFlow<Boolean> = prefs.getFlow("use_org_branding", false)
        .stateIn(scope, SharingStarted.Eagerly, false)
    override val usePlatformBranding: StateFlow<Boolean> = prefs.getFlow("use_platform_branding", false)
        .stateIn(scope, SharingStarted.Eagerly, false)

    private val _activeBranding = MutableStateFlow<BrandingConfig?>(null)
    override val activeBranding: StateFlow<BrandingConfig?> = _activeBranding.asStateFlow()

    override fun setActiveBranding(branding: BrandingConfig?) {
        _activeBranding.value = branding
    }

    override fun setUseOrgBranding(useOrg: Boolean) {
        prefs.putDirect("use_org_branding", useOrg)
    }

    override fun setUsePlatformBranding(usePlatform: Boolean) {
        prefs.putDirect("use_platform_branding", usePlatform)
    }

    override fun nextFontScale() {
        val current = userFontScale.value
        val next = if (current >= 4) 1 else current + 1
        prefs.putDirect(fontKey, next)
    }

    override fun setFontScale(scale: Int) {
        prefs.putDirect(fontKey, scale)
    }

    override fun setLastEventSeq(seq: Long) {
        prefs.putDirect(lastSeqKey, seq)
    }

    override fun getLastEventSeq(): Long {
        return prefs.getDirect(lastSeqKey, 0L)
    }
}
