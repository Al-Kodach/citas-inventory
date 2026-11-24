package com.citacomsys.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant

/** UI-driven customization for the workspace.
 * If App name is null, we use org/workspace name
 **/

@Serializable
@SerialName("BrandingConfig")
data class BrandingConfig(
    val appName: String,
    val logoUrl: String? = null,

    // Core Brand Colors
    val primaryColor: Long,
    val accentColor: Long? = null,
    val tertiaryColor: Long? = null,

    // UI Semantics
    val errorColor: Long? = null,
    val defaultDarkMode: Boolean = false,
    val paletteStyle: String? = null,
    val updatedAt: Instant = Clock.System.now()
)
