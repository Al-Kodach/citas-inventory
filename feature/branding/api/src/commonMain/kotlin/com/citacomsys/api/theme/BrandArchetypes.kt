package com.citacomsys.api.theme

import androidx.compose.ui.graphics.Color

object BrandArchetypes {

    val Clinical = BrandArchetype(
        id = "clinical",
        name = "Clinical",
        description = "Healthcare and Pharmacy",
        primary = Color(0xFF2E7D32),
        secondary = Color(0xFF4CAF50),
        tertiary = Color(0xFF26A69A),
        error = Color(0xFFB3261E)
    )

    val Corporate = BrandArchetype(
        id = "corporate",
        name = "Corporate",
        description = "Enterprise",
        primary = Color(0xFF1565C0),
        secondary = Color(0xFF5C6BC0),
        tertiary = Color(0xFF26C6DA),
        error = Color(0xFFD32F2F)
    )

    val Retail = BrandArchetype(
        id = "retail",
        name = "Retail",
        description = "Modern retail",
        primary = Color(0xFFFF6F00),
        secondary = Color(0xFFFFB300),
        tertiary = Color(0xFF00ACC1),
        error = Color(0xFFC62828)
    )

    val Executive = BrandArchetype(
        id = "executive",
        name = "Executive",
        description = "Luxury",
        primary = Color(0xFF512DA8),
        secondary = Color(0xFF7E57C2),
        tertiary = Color(0xFF3949AB),
        error = Color(0xFFC62828)
    )

    val All = listOf(
        Clinical,
        Corporate,
        Retail,
        Executive
    )

    fun fromColors(
        primary: Color,
        secondary: Color = primary,
        tertiary: Color = primary,
        error: Color = Color(0xFFD32F2F),
        name: String = "Custom Archetype",
        description: String = "Created from selected color set"
    ): BrandArchetype {
        return BrandArchetype(
            id = "custom_${primary.hashCode()}",
            name = name,
            description = description,
            primary = primary,
            secondary = secondary,
            tertiary = tertiary,
            error = error
        )
    }
}
