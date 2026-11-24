package com.citacomsys.core.domain.security

import com.citacomsys.types.UserCapability
import kotlinx.coroutines.flow.StateFlow

interface UserCapabilityManager {

    val capabilities: StateFlow<Set<UserCapability>>

    fun has(capability: UserCapability): Boolean

    fun hasAll(caps: Set<UserCapability>): Boolean
}