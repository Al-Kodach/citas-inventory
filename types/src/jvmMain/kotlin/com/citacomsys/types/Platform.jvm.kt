package com.citacomsys.types

import com.citacomsys.types.PlatformUUID
import java.util.UUID

actual fun generateUUID(): PlatformUUID = UUID.randomUUID().toString()