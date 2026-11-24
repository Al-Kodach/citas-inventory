package com.citacomsys.core.components.navigationsuite

import androidx.compose.ui.tooling.preview.Preview

/**
 * Adaptive Previews based on WindowSizeClass breakpoints (Light & Dark modes):
 * Compact: < 600dp
 * Medium: 600dp - 839dp
 * Expanded: 840dp - 1199dp
 * Large: 1200dp - 1599dp
 * Extra Large: >= 1600dp
 */

@Preview(
    name = "Phone (Light)",
    group = "Adaptive-Light",
    widthDp = 360,
    heightDp = 800,
    showBackground = true,
)
annotation class PhoneLightPreview

@Preview(
    name = "Phone (Dark)",
    group = "Adaptive-Dark",
    widthDp = 360,
    heightDp = 800,
    showBackground = true,
    uiMode = 32,
)
annotation class PhoneDarkPreview

@Preview(
    name = "Tablet (Light)",
    group = "Adaptive-Light",
    showBackground = true,
    widthDp = 700,
    heightDp = 900
)
annotation class TabletLightPreview

@Preview(
    name = "Tablet (Dark)",
    group = "Adaptive-Dark",
    widthDp = 700,
    showBackground = true,
    heightDp = 900,
    uiMode = 32,
)
annotation class TabletDarkPreview

@Preview(
    name = "Desktop (Light)",
    group = "Adaptive-Light",
    showBackground = true,
    widthDp = 1000,
    heightDp = 800
)
annotation class DesktopLightPreview

@Preview(
    name = "Desktop (Dark)",
    group = "Adaptive-Dark",
    widthDp = 1000,
    showBackground = true,
    heightDp = 800,
    uiMode = 32,
)
annotation class DesktopDarkPreview

@Preview(
    name = "Workstation (Light)",
    group = "Adaptive-Light",
    showBackground = true,
    widthDp = 1300,
    heightDp = 1000
)
annotation class LargeLightPreview

@Preview(
    name = "Workstation (Dark)",
    group = "Adaptive-Dark",
    widthDp = 1300,
    showBackground = true,
    heightDp = 1000,
    uiMode = 32
)
annotation class LargeDarkPreview

@Preview(
    name = "Ultrawide (Light)",
    group = "Adaptive-Light",
    showBackground = true,
    widthDp = 1700,
    heightDp = 1000
)
annotation class ExtraLargeLightPreview

@Preview(
    name = "Ultrawide (Dark)",
    group = "Adaptive-Dark",
    showBackground = true,
    widthDp = 1700,
    heightDp = 1000,
    uiMode = 32,
)
annotation class ExtraLargeDarkPreview

@PhoneLightPreview
@PhoneDarkPreview
@TabletLightPreview
@TabletDarkPreview
@DesktopLightPreview
@DesktopDarkPreview
@LargeLightPreview
@LargeDarkPreview
@ExtraLargeLightPreview
@ExtraLargeDarkPreview
annotation class AllAdaptivePreviews
