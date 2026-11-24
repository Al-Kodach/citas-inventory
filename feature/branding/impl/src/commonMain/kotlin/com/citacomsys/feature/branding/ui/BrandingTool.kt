package com.citacomsys.feature.branding.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.Color
import com.citacomsys.core.branding.BrandingTool as SharedBrandingTool

/**
 * Delegating wrapper for the shared [SharedBrandingTool] component from `core-client:design`.
 */
@Composable
fun BrandingTool(
    appName: String = "",
    darkState: MutableState<Boolean>,
    primaryColor: Color,
    accentColor: Color,
    tertiaryColor: Color,
    errorColor: Color,
    onPrimaryChange: (Color) -> Unit,
    onAccentChange: (Color) -> Unit,
    onTertiaryChange: (Color) -> Unit,
    onErrorChange: (Color) -> Unit,
) {
    SharedBrandingTool(
        appName = appName,
        darkState = darkState,
        primaryColor = primaryColor,
        accentColor = accentColor,
        tertiaryColor = tertiaryColor,
        errorColor = errorColor,
        onPrimaryChange = onPrimaryChange,
        onAccentChange = onAccentChange,
        onTertiaryChange = onTertiaryChange,
        onErrorChange = onErrorChange,
    )
}
