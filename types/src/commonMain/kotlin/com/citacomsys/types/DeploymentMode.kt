package com.citacomsys.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

//@SerialName("DeploymentMode")
@Serializable
enum class DeploymentMode {
    /** Standard Cloud Storage & SaaS Service Mode. */
    CLOUD_SYNCED,
    /** Local Edge / Offline Storage & Service Mode (Powered by Industrial Mesh). */
    OFFLINE_LOCAL;

    val isOffline: Boolean
        get() = this == OFFLINE_LOCAL

    val isCloudSynced: Boolean
        get() = this == CLOUD_SYNCED
}
