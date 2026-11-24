package com.citacomsys.feature.branding.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.citacomsys.core.branding.BrandingPreviewCard as SharedBrandingPreviewCard

/**
 * Delegating wrapper for the shared [SharedBrandingPreviewCard] component from `core-client:design`.
 */
@Composable
fun BrandingPreviewCard(
    appName: String,
    darkState: MutableState<Boolean>,
    primary: Color,
    accent: Color,
    tertiary: Color,
    error: Color = Color(0xFFD32F2F),
    modifier: Modifier = Modifier,
) {
    SharedBrandingPreviewCard(
        appName = appName,
        darkState = darkState,
        primary = primary,
        accent = accent,
        tertiary = tertiary,
        error = error,
        modifier = modifier,
    )
}
