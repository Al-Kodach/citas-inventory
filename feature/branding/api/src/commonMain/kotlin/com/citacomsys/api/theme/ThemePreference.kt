package com.citacomsys.api.theme

import kotlinx.serialization.Serializable

@Serializable
data class ThemePreference(

    val followSystem: Boolean = true,

    val darkMode: Boolean = false,

    val reduceTransparency: Boolean = false,

    val highContrast: Boolean = false,

    val dynamicColors: Boolean = true
)