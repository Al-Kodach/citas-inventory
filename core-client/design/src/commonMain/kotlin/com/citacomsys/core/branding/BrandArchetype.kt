package com.citacomsys.core.branding

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.design.ThemeManager

data class BrandArchetype(
    val id: String = "",
    val name: String,
    val description: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val error: Color = Color(0xFFDE350B),
) {
    fun toColorScheme(isDark: Boolean = false): ColorScheme {
        return ThemeManager.fromColors(
            primary = primary,
            secondary = secondary,
            tertiary = tertiary,
            error = error,
            isDark = isDark
        )
    }

    companion object {
        fun fromColors(
            name: String = "Custom Archetype",
            description: String = "Custom archetype created from selected colors",
            primary: Color,
            secondary: Color = primary,
            tertiary: Color = primary,
            error: Color = Color(0xFFDE350B),
        ): BrandArchetype {
            return BrandArchetype(
                id = "custom_${primary.hashCode()}",
                name = name,
                description = description,
                primary = primary,
                secondary = secondary,
                tertiary = tertiary,
                error = error,
            )
        }
    }
}

object BrandArchetypes {
    val ClinicalEmerald = BrandArchetype(
        id = "clinical_emerald",
        name = "Clinical Emerald",
        description = "Healthcare, Pharmacy & Medical",
        primary = Color(0xFF00875A),   // Vivid Emerald Green
        secondary = Color(0xFF00B8D9), // Electric Cyan
        tertiary = Color(0xFF36B37E),  // Bright Mint
        error = Color(0xFFDE350B),      // Bright Red
    )

    val CorporateSapphire = BrandArchetype(
        id = "corporate_sapphire",
        name = "Corporate Sapphire",
        description = "Enterprise & Corporate Finance",
        primary = Color(0xFF0052CC),   // Royal Sapphire Blue
        secondary = Color(0xFF2684FF), // Bright Sky Blue
        tertiary = Color(0xFF00B8D9),  // Electric Aqua
        error = Color(0xFFFF5630),      // Coral Red
    )

    val RetailEmber = BrandArchetype(
        id = "retail_ember",
        name = "Retail Ember",
        description = "Modern Commerce & Retail",
        primary = Color(0xFFFF5630),   // Vivid Scarlet Crimson
        secondary = Color(0xFFFFAB00), // Bright Amber Gold
        tertiary = Color(0xFF36B37E),  // Vivid Mint
        error = Color(0xFFBF2600),      // Deep Red
    )

    val CyberNeon = BrandArchetype(
        id = "cyber_neon",
        name = "Cyber Neon",
        description = "Tech-Forward & Modern SaaS",
        primary = Color(0xFF6554C0),   // Electric Indigo
        secondary = Color(0xFF00C7E6), // Bright Cyan Aqua
        tertiary = Color(0xFF8777D9),  // Vivid Lavender
        error = Color(0xFFFF5630),      // Vivid Coral
    )

    val ExecutiveGold = BrandArchetype(
        id = "executive_gold",
        name = "Executive Gold",
        description = "Premium & High-End Luxury",
        primary = Color(0xFF172B4D),   // Deep Midnight Carbon
        secondary = Color(0xFFFFAB00), // Vibrant Gold
        tertiary = Color(0xFF36B37E),  // Emerald Accent
        error = Color(0xFFDE350B),      // Vivid Red
    )

    val EcoMeadow = BrandArchetype(
        id = "eco_meadow",
        name = "Eco Meadow",
        description = "Sustainable & Natural Wellness",
        primary = Color(0xFF36B37E),   // Vivid Leaf Green
        secondary = Color(0xFF57D9A3), // Bright Mint
        tertiary = Color(0xFFFFAB00),  // Warm Sun Gold
        error = Color(0xFFDE350B),      // Alert Red
    )

    val IndustrialTitanium = BrandArchetype(
        id = "industrial_titanium",
        name = "Industrial Titanium",
        description = "Automotive & Heavy Industry",
        primary = Color(0xFF253858),   // Slate Titanium
        secondary = Color(0xFFFF9900), // High-Vis Industrial Orange
        tertiary = Color(0xFF4C6B8B),  // Steel Blue
        error = Color(0xFFDE350B),      // Alert Red
    )

    val All = listOf(
        ClinicalEmerald,
        CorporateSapphire,
        RetailEmber,
        CyberNeon,
        ExecutiveGold,
        EcoMeadow,
        IndustrialTitanium,
    )
}
