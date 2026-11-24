package com.citacomsys.feature.branding.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.citacomsys.core.branding.BrandingConfigScreen as CoreBrandingConfigScreen
import com.citacomsys.types.BrandingConfig

@Composable
fun BrandingConfigScreen(
    modifier: Modifier = Modifier,
    defaultName: String = "",
    currentBranding: BrandingConfig?,
    onSave: (BrandingConfig?) -> Unit,
) {
    CoreBrandingConfigScreen(
        modifier = modifier,
        defaultName = defaultName,
        currentBranding = currentBranding,
        onSave = onSave
    )
}
