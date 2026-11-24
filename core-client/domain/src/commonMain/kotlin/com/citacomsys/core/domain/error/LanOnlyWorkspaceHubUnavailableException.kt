package com.citacomsys.core.domain.error

import okio.IOException

class LanOnlyWorkspaceHubUnavailableException(
    val workspaceId: String = "",
    message: String = "Workspace [$workspaceId] is LAN-only and Mother Hub is unreachable."
) : IOException(message)
