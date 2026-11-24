package com.citacomsys.inventory.admin

import com.citacomsys.core.domain.security.UserCapabilityManager
import com.citacomsys.inventory.admin.AdminConstant.APP_USER
import com.citacomsys.types.User
import com.citacomsys.types.UserCapability
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.asMutableStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class AdminUserCapabilityManager(
    kSafe: KSafe,
    scope: CoroutineScope
): UserCapabilityManager {
    private val activeUser by kSafe.asMutableStateFlow<User?>(null, scope, APP_USER)

    override val capabilities: StateFlow<Set<UserCapability>> = activeUser.map { user ->
        user?.capabilities?.toSet() ?: emptySet()
    }.stateIn(
        scope = CoroutineScope(Dispatchers.Default),
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    override fun has(capability: UserCapability): Boolean {
        return capabilities.value.contains(capability)
    }

    override fun hasAll(caps: Set<UserCapability>): Boolean {
        if (caps.isEmpty()) return true
        return capabilities.value.containsAll(caps)
    }
}
