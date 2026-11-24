package com.citacomsys.types

enum class SessionType {
    // The app is connected to a cloud workspace.
    CLOUD_SAAS,

    // The app is running in a standalone, offline-only mode.
    STANDALONE_OFFLINE;

    val isSubscribed: Boolean
        get() = this == CLOUD_SAAS
}

