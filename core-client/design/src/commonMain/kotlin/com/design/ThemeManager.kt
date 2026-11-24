package com.design

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.citacomsys.types.BrandingConfig
import com.materialkolor.Contrast
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

object ThemeManager {

    /**
     * Converts a [BrandingConfig] into a Compose [ColorScheme] using MaterialKolor dynamic color generation.
     */
    fun toColorScheme(
        branding: BrandingConfig?,
        isDark: Boolean = false,
        style: PaletteStyle = PaletteStyle.Vibrant,
        defaultScheme: ColorScheme = if (isDark) DarkColors else LightColors
    ): ColorScheme {
        if (branding == null) return defaultScheme

        val primary = Color(branding.primaryColor.toInt())
        val secondary = branding.accentColor?.let { Color(it.toInt()) }
        val tertiary = branding.tertiaryColor?.let { Color(it.toInt()) }
        val error = branding.errorColor?.let { Color(it.toInt()) }

        val effectiveStyle = branding.paletteStyle?.let { name ->
            runCatching { PaletteStyle.valueOf(name) }.getOrNull()
        } ?: style

        return dynamicColorScheme(
            primary = primary,
            secondary = secondary,
            tertiary = tertiary,
            error = error,
            isDark = isDark,
            isAmoled = false,
            style = effectiveStyle,
            contrastLevel = Contrast.Default.value
        )
    }

    fun fromBranding(
        branding: BrandingConfig?,
        isDark: Boolean = false,
        style: PaletteStyle = PaletteStyle.Vibrant,
        defaultScheme: ColorScheme = if (isDark) DarkColors else LightColors
    ): ColorScheme = toColorScheme(branding, isDark, style, defaultScheme)

    /**
     * Creates a Compose [ColorScheme] directly from individual seed colors using MaterialKolor.
     */
    fun fromColors(
        primary: Color,
        secondary: Color? = null,
        tertiary: Color? = null,
        error: Color? = null,
        isDark: Boolean = false,
        style: PaletteStyle = PaletteStyle.Vibrant
    ): ColorScheme {
        return dynamicColorScheme(
            primary = primary,
            secondary = secondary,
            tertiary = tertiary,
            error = error,
            isDark = isDark,
            isAmoled = false,
            style = style
        )
    }

    /**
     * Calculates luminance to determine if a background color is light or dark for high contrast text.
     */
    fun isColorLight(color: Color): Boolean {
        val luminance = (0.2126 * color.red + 0.7152 * color.green + 0.0722 * color.blue)
        return luminance > 0.5
    }
}
