package com.design

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

/**
 * First-Class Brand Seed Color for CitasInventorySaaS.
 * A sophisticated deep ocean teal sapphire seed that yields a crisp, high-contrast, professional M3 color palette.
 */
val BrandSeedColor: Color = Color(0xFF59C5DD)
val BrandSecondarySeedColor: Color = Color(0xFF4A6267)
val BrandTertiarySeedColor: Color = Color(0xFF535E7D)

/**
 * Default Light ColorScheme generated via MaterialKolor using [BrandSeedColor] with Fidelity style.
 */
val LightColors: ColorScheme = dynamicColorScheme(
    seedColor = BrandSeedColor,
    isDark = false,
    isAmoled = false,
    style = PaletteStyle.Fidelity
)

/**
 * Default Dark ColorScheme generated via MaterialKolor using [BrandSeedColor] with Fidelity style.
 */
val DarkColors: ColorScheme = dynamicColorScheme(
    seedColor = BrandSeedColor,
    isDark = true,
    isAmoled = false,
    style = PaletteStyle.Fidelity
)

/**
 * Generates a dynamic M3 [ColorScheme] from a seed color using MaterialKolor with crisp Fidelity palette matching.
 */
fun createBrandColorScheme(
    seedColor: Color = BrandSeedColor,
    isDark: Boolean = false,
    style: PaletteStyle = PaletteStyle.Fidelity,
    isAmoled: Boolean = false,
    contrastLevel: Double = 0.0
): ColorScheme = dynamicColorScheme(
    seedColor = seedColor,
    isDark = isDark,
    isAmoled = isAmoled,
    style = style,
    contrastLevel = contrastLevel
)

/**
 * Generates a dynamic M3 [ColorScheme] with separate seed colors using MaterialKolor.
 */
fun createBrandColorScheme(
    primary: Color,
    secondary: Color? = null,
    tertiary: Color? = null,
    error: Color? = null,
    isDark: Boolean = false,
    style: PaletteStyle = PaletteStyle.Fidelity,
    isAmoled: Boolean = false,
    contrastLevel: Double = 0.0
): ColorScheme = dynamicColorScheme(
    primary = primary,
    secondary = secondary,
    tertiary = tertiary,
    error = error,
    isDark = isDark,
    isAmoled = isAmoled,
    style = style,
    contrastLevel = contrastLevel
)

// Brand Palette Aliases
val BrandPrimaryRed = Color(0xFFE1001A)
val BrandDeepBlue = Color(0xFF002D62)
val BrandBrightBlue = Color(0xFF009EE0)
val BrandLeafGreen = Color(0xFF48A23F)
val BrandSolarYellow = Color(0xFFFFC20E)
val BrandPurple = Color(0xFF6F2C91)
