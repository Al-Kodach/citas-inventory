package com.citacomsys.api.theme

import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}