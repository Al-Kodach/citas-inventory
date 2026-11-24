package com.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.citacomsys.types.BrandingConfig
import com.materialkolor.PaletteStyle

@Composable
fun rememberLightColors(
    seedColor: Color = BrandSeedColor,
    style: PaletteStyle = PaletteStyle.Vibrant
): ColorScheme = remember(seedColor, style) {
    if (seedColor == BrandSeedColor && style == PaletteStyle.Vibrant) {
        LightColors
    } else {
        createBrandColorScheme(seedColor = seedColor, isDark = false, style = style)
    }
}

@Composable
fun rememberDarkColors(
    seedColor: Color = BrandSeedColor,
    style: PaletteStyle = PaletteStyle.Vibrant
): ColorScheme = remember(seedColor, style) {
    if (seedColor == BrandSeedColor && style == PaletteStyle.Vibrant) {
        DarkColors
    } else {
        createBrandColorScheme(seedColor = seedColor, isDark = true, style = style)
    }
}

val BrandGradient = Brush.linearGradient(
    colors = listOf(
        BrandPrimaryRed,
        BrandSolarYellow,
        BrandLeafGreen,
        BrandBrightBlue,
        BrandPurple
    )
)

@Composable
fun AppTheme(
    isDarkTheme: Boolean? = null,
    fontScale: Int = 1,
    seedColor: Color = BrandSeedColor,
    colorScheme: ColorScheme? = null,
    brandColors: ColorScheme? = null,
    branding: Any? = null,
    content: @Composable () -> Unit
) {
    val isDark = isDarkTheme ?: isSystemInDarkTheme()

    val primaryColor = (branding as? BrandingConfig)?.primaryColor?.let {
        Color(it.toInt())
    } ?: seedColor

    val effectiveColorScheme = colorScheme
        ?: (branding as? BrandingConfig)?.let {
            ThemeManager.fromBranding(it, isDark = isDark)
        } ?: brandColors

        ?: if (isDark) {
            rememberDarkColors(seedColor = primaryColor)
        } else {
            rememberLightColors(seedColor = primaryColor)
        }

    MaterialTheme(
        colorScheme = effectiveColorScheme,
        typography = AppTypography(fontScale = fontScale)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = effectiveColorScheme.background,
            contentColor = effectiveColorScheme.onBackground,
            content = content
        )
    }
}
