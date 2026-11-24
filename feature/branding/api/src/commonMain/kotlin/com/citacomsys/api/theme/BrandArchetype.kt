package com.citacomsys.api.theme

import androidx.compose.ui.graphics.Color

data class BrandArchetype(

    val id: String,

    val name: String,

    val description: String,

    val primary: Color,

    val secondary: Color,

    val tertiary: Color,

    val error: Color
)