package com.citacomsys.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
@SerialName("UserWorkspaceBinding")
data class UserWorkspaceBinding(
    val id: String,
    val userId: String,
    val workspaceId: String,
    val workspaceName: String,
    val branchId: String? = null,
    val branchName: String? = null,
    val role: String,
    val capabilities: Set<UserCapability> = emptySet(),
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant
)
