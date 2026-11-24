package com.citacomsys.auth.session

import com.citacomsys.api.auth.SessionMetadata
import com.citacomsys.api.auth.service.SessionService
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.inventory.api.user.RoleRepository
import com.citacomsys.inventory.api.user.UserRepository
import com.citacomsys.types.AuthTokens
import com.citacomsys.types.AuthenticatedSession
import com.citacomsys.types.DefaultCapabilities
import com.citacomsys.types.User
import com.citacomsys.types.UserCapability
import com.citacomsys.types.UserType

class AuthenticationOrchestrator(
    private val sessionService: SessionService,
    private val userRepositor: UserRepository,
    private val roleRepo: RoleRepository
) {

    suspend fun createAuthenticatedSession(
        user: User,
        workspaceId: String?,
        metadata: SessionMetadata,
    ): AuthenticatedSession = dbQuery {

        val tokens = sessionService.create(
            user = user,
            workspaceId = workspaceId,
            metadata = metadata
        )

        val resolvedUserCapabilities = resolveRoleWithCapabilities(
            user.userType,
            user.role,
            user.organizationId
        )

        val bindings = userRepositor.getBindingsForUserId(user.id, user.organizationId)
            .map {
                it.copy(
                    capabilities = resolveRoleWithCapabilities(
                        user.userType,
                        it.role,
                        user.organizationId
                    )
                )
            }

        AuthenticatedSession(
            AuthTokens(
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken
            ),
            user = user.copy(capabilities = resolvedUserCapabilities),
            workspaceBindings = bindings
        )
    }

    private suspend fun resolveRoleWithCapabilities(
        userType: UserType,
        role: String,
        orgId: String
    ): Set<UserCapability> {
        val roleKey = when (userType) {
            UserType.SYSTEM -> if (role.startsWith("PLATFORM_")) role else "PLATFORM_$role"
            UserType.PLATFORM -> if (role.startsWith("PLATFORM_")) role else "PLATFORM_$role"
            UserType.TENANT -> if (role.startsWith("TENANT_")) role else "TENANT_$role"
        }

        val capsFromDb = roleRepo.findCapabilities(orgId, roleKey)
        if (capsFromDb.isNotEmpty()) return capsFromDb

        return DefaultCapabilities.forRole(roleKey)
    }
}
