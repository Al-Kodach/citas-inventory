package com.citacomsys.api.organization.model

import com.citacomsys.kernel.model.OrganizationRuntimeConfig
import com.citacomsys.types.AuthTokens
import com.citacomsys.types.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("WorkstationRecoveryRequest")
data class WorkstationRecoveryRequest(
    val businessCode: String,
    val hardwareId: String,
    val recoveryCode: String,
    val publicKey: String
)

@Serializable
@SerialName("WorkstationRecoveryResult")
data class WorkstationRecoveryResult(
    val config: OrganizationRuntimeConfig,
    val tokens: AuthTokens,
    val user: User
)
