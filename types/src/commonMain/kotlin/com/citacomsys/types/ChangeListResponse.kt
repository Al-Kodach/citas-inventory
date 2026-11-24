package com.citacomsys.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("ChangeListResponse")
data class ChangeListResponse<T, ID>(
    val updated: List<T>,
    val deleted: List<ID>
)

@Serializable
data class MeshHop(
    val nodeId: String,
    val nodeName: String,
    val replayedAt: String,
    val channelMode: String // "SATELLITE" or "HUB"
)

@Serializable
data class SyncMetadata(
    val wsId: String,
    val brId: String,
    val actorId: String,
    val actorName: String,
    val createdAt: String,
    /** The sequential path this data travelled through your physical network */
    val routingChain: List<MeshHop> = emptyList()
)