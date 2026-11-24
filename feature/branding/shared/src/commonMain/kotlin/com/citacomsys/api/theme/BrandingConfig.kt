package com.citacomsys.api.theme

import kotlinx.serialization.Serializable

@Serializable
data class BrandingConfig(
    val appName: String,
    val primaryColor: Long,
    val accentColor: Long,
    val tertiaryColor: Long,
    val errorColor: Long,

    val logoUrl: String? = null,
    val iconUrl: String? = null,
    val faviconUrl: String? = null,

    val themeMode: ThemeMode = ThemeMode.SYSTEM,

    val typography: TypographyStyle = TypographyStyle.MODERN,
    val cornerStyle: CornerStyle = CornerStyle.ROUNDED,
    val glassEnabled: Boolean = true,
    val highContrast: Boolean = true,

    val updatedAt: String
)