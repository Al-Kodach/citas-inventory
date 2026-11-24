package com.citacomsys.theme.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.citacomsys.feature.branding.ui.BrandingPreviewCard

@Composable
fun BrandingPreviewCard(
    appName: String,
    darkState: MutableState<Boolean>,
    primary: Color,
    accent: Color,
    tertiary: Color,
    error: Color = Color(0xFFDE350B),
    modifier: Modifier = Modifier,
) {
    BrandingPreviewCard(
        appName = appName,
        darkState = darkState,
        primary = primary,
        accent = accent,
        tertiary = tertiary,
        error = error,
        modifier = modifier,
    )
}
