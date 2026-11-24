package com.citacomsys.core.domain.result

import java.util.UUID

object SystemActors {
   const val SYSTEM_ID = "00000000-0000-0000-0000-000000000000"

    // One ID for everything system-related
    val SYSTEM_workspace_id: UUID = UUID.fromString(SYSTEM_ID)
    val SYSTEM_USER_ID: UUID = UUID.fromString(SYSTEM_ID)

    const val SYSTEM_NAME = "SYSTEM_ACTOR"
}