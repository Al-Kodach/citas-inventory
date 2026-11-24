package com.citacomsys.api.workspace.branch

import com.citacomsys.types.Address
import com.citacomsys.types.generateUUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant

@Serializable
@SerialName("Branch")
data class Branch(
    val id: String = generateUUID(),
    val workspaceId: String,
    val name: String,
    val locationCode: String,
    val address: Address? = null,
    val contactPhone: String? = null,
    val email: String? = null,
    val website: String? = null,
    val isActive: Boolean = true,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
)


