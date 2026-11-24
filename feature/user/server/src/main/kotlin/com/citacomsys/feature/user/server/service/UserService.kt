package com.citacomsys.feature.user.server.service

import com.citacomsys.core.logging.logApplicationEvent
import com.citacomsys.core.logging.logIdentityAccessEvent
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.inventory.api.user.PlatformAdminRepository
import com.citacomsys.inventory.api.user.RoleRepository
import com.citacomsys.inventory.api.user.UserPort
import com.citacomsys.inventory.api.user.model.BindUserToWorkspaceRequest
import com.citacomsys.inventory.api.user.model.SaveTenantUserRequest
import com.citacomsys.inventory.api.user.model.UnbindUserFromWorkspaceRequest
import com.citacomsys.shared.security.PasswordHasher
import com.citacomsys.types.ChangeListResponse
import com.citacomsys.types.DefaultCapabilities
import com.citacomsys.types.EntityType
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import com.citacomsys.types.SessionItemDto
import com.citacomsys.types.User
import com.citacomsys.types.UserCapability
import com.citacomsys.types.UserItem
import com.citacomsys.types.UserStatus
import com.citacomsys.types.UserType
import com.citacomsys.types.UserWorkspaceBinding
import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import mu.KotlinLogging
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.time.Instant

internal class UserService(
    private val repository: UserPort,
    private val passwordHasher: PasswordHasher,
    private val adminRepository: PlatformAdminRepository,
    private val auditor: TelemetryAuditor
) : UserPort {
    override suspend fun updateBranch(userId: String, branchId: String) {
        repository.updateBranch(userId, branchId)
        idCache.synchronous().invalidate(userId)
    }

    override suspend fun updateOrganization(userId: String, organizationId: String) {
        repository.updateOrganization(userId, organizationId)
        idCache.synchronous().invalidate(userId)
    }
    private val logger = KotlinLogging.logger { }

    private val cacheScope = CoroutineScope(Dispatchers.IO.limitedParallelism(4))
    private val idCache: AsyncCache<String, User> = Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .maximumSize(1000)
        .buildAsync()

    private fun resolveUserWithCapabilities(user: User): User {
        if (user.userType == UserType.SYSTEM) {
            return user.copy(capabilities = UserCapability.entries.toSet())
        }

        if (user.capabilities.isNotEmpty()) {
            return user
        }

        val resolvedCaps = DefaultCapabilities.forRole(user.role)
        return user.copy(capabilities = resolvedCaps)
    }

    override suspend fun findById(id: String): Result<User> = runCatching {
        idCache.getOrLoadSuspend(cacheScope, id) { key ->
            val user = repository.findById(key).getOrNull() ?:
            adminRepository.findById(id).getOrNull()?.toUser()
            ?: throw NoSuchElementException("User not found")
            resolveUserWithCapabilities(user)
        }
    }

    override suspend fun save(
        request: SaveTenantUserRequest,
        organizationId: String,
    ): User {
        val existing = repository.findById(request.id).getOrNull()

        val passwordHash = if (existing == null) {
            val rawPassword = request.password
                ?: throw IllegalArgumentException("Password required for new staff registration.")
            passwordHasher.hash(rawPassword)
        } else {
            existing.passwordHash
        }

        val userCaps = if (request.capabilities.isNotEmpty()) {
            request.capabilities
        } else {
            DefaultCapabilities.forRole(request.role)
        }

        val user = User(
            id = request.id,
            organizationId = organizationId,
            name = request.name,
            username = request.username,
            phone = request.phone,
            email = request.email,
            role = request.role,
            capabilities = userCaps,
            status = existing?.status ?: UserStatus.ACTIVE,
            userType = request.type,
            passwordHash = passwordHash,
            isPhoneVerified = existing?.isPhoneVerified ?: false,
            mfaEnabled = existing?.mfaEnabled ?: false,
            suspendedReason = existing?.suspendedReason,
            createdAt = request.createdAt,
            updatedAt = request.createdAt,
        )

        val savedUser = repository.save(user)

        // Automatically bind user to workspace and branch when specified in SaveTenantUserRequest
        if (!request.wsId.isNullOrBlank()) {
            runCatching {
                repository.bindUserToWorkspace(
                    BindUserToWorkspaceRequest(
                        userId = savedUser.id,
                        workspaceId = request.wsId!!,
                        role = request.role,
                        capabilities = userCaps,
                        useOrgCapabilities = true,
                        branchId = request.branchId
                    ),
                    organizationId = organizationId
                )
                logger.info { "User ${savedUser.id} automatically bound to workspace ${request.wsId} (branch: ${request.branchId})" }
            }.onFailure { e ->
                logger.warn(e) { "Failed to bind user ${savedUser.id} to workspace ${request.wsId}: ${e.message}" }
            }
        }

        return resolveUserWithCapabilities(savedUser).also {
            idCache.synchronous().invalidate(it.id)
            idCache.synchronous().invalidate(it.phone.value)

            auditor.logApplicationEvent(
                action = if (existing == null) "USER_CREATED" else "USER_UPDATED",
                entityType = EntityType.USER.name,
                entityId = it.id,
                summary = "User ${it.name} was ${if (existing == null) "created" else "updated"}",
                level = TelemetryLevel.INFO,
                organizationIdOverride = organizationId
            )
        }
    }

    override suspend fun getChangesSince(
        since: Instant?,
        organizationId: String
    ): ChangeListResponse<User, String> {
        val changes = repository.getChangesSince(since, organizationId)
        val resolvedUpdated = changes.updated.map { resolveUserWithCapabilities(it) }
        return changes.copy(updated = resolvedUpdated)
    }

    override suspend fun save(user: User): User {
        return resolveUserWithCapabilities(repository.save(user)).also {
            idCache.synchronous().invalidate(it.id)
            idCache.synchronous().invalidate(it.phone.value)
        }
    }


    override suspend fun findByPhone(phone: String): Result<User> = runCatching {
        repository.findByPhone(phone).getOrThrow().let { user ->
            val resolvedUser = resolveUserWithCapabilities(user)
            val future = CompletableFuture.completedFuture(resolvedUser)
            idCache.put(resolvedUser.id, future)
            resolvedUser
        }
    }

    override suspend fun findByUsername(identity: String): Result<User> = runCatching {
        val user = repository.findByUsername(identity).getOrThrow()
        resolveUserWithCapabilities(user)
    }

    override suspend fun findByIdentity(identity: String): Result<User> = runCatching {
        val user = repository.findByIdentity(identity).getOrThrow()
        resolveUserWithCapabilities(user)
    }

    override suspend fun markPhoneNumberAsVerified(userId: String) {
        repository.markPhoneNumberAsVerified(userId)
        idCache.synchronous().invalidate(userId)
    }

    override suspend fun findOwner(organizationId: String): User? {
        val owner = repository.findOwner(organizationId) ?: return null
        return resolveUserWithCapabilities(owner)
    }

    override suspend fun suspendUser(
        userId: String,
        reason: String
    ): Result<User> {
        val result = repository.suspendUser(userId, reason)
        return result.map { resolveUserWithCapabilities(it) }.onSuccess {
            val future = CompletableFuture.completedFuture(it)
            idCache.put(it.id, future)
            idCache.put(it.phone.value, future)

            auditor.logApplicationEvent(
                action = "USER_SUSPENDED",
                entityType = EntityType.USER.name,
                entityId = it.id,
                summary = "User ${it.name} suspended. Reason: $reason",
                level = TelemetryLevel.WARNING,
                organizationIdOverride = it.organizationId
            )
        }
    }

    override suspend fun activateUser(userId: String): Result<User> {
        val result = repository.activateUser(userId)
        return result.map { resolveUserWithCapabilities(it) }.onSuccess {
            val future = CompletableFuture.completedFuture(it)
            idCache.put(it.id, future)
            idCache.put(it.phone.value, future)

            auditor.logApplicationEvent(
                action = "USER_ACTIVATED",
                entityType = EntityType.USER.name,
                entityId = it.id,
                summary = "User ${it.name} activated",
                level = TelemetryLevel.INFO,
                organizationIdOverride = it.organizationId
            )
        }
    }

    override suspend fun updatePassword(
        userId: String,
        newPasswordHash: String,
        forceChange: Boolean
    ): Boolean {
        val user = findById(userId).getOrThrow()

        val success = repository.updatePassword(
            user.id,
            newPasswordHash,
            forceChange
        )

        if (success) {
            idCache.synchronous().invalidate(userId)
            idCache.synchronous().invalidate(user.phone.value)

            auditor.logIdentityAccessEvent(
                userId = userId,
                action = "PASSWORD_CHANGED",
                outcome = "SUCCESS",
                level = TelemetryLevel.INFO
            )
            return true
        }

        auditor.logIdentityAccessEvent(
            userId = userId,
            action = "PASSWORD_CHANGED",
            outcome = "FAILURE",
            level = TelemetryLevel.ERROR
        )

        error("Failed to update password")
    }

    override suspend fun updateStatus(
        status: UserStatus,
        userId: String
    ) {
        repository.updateStatus(status, userId)
        idCache.synchronous().invalidate(userId)
    }

    override suspend fun delete(userId: String, actorId: String): Result<Unit> = runCatching {
        repository.delete(userId, actorId).onSuccess {
            idCache.synchronous().invalidate(userId)

            auditor.logApplicationEvent(
                action = "USER_DELETED",
                entityType = EntityType.USER.name,
                entityId = userId,
                summary = "User $userId deleted by $actorId",
                level = TelemetryLevel.WARNING
            )
        }.getOrThrow()
    }

    override suspend fun updateMfaEnabled(id: String, bool: Boolean) {
        repository.updateMfaEnabled(id, bool)
        idCache.synchronous().invalidate(id)

        auditor.logApplicationEvent(
            action = if (bool) "MFA_ENABLED" else "MFA_DISABLED",
            entityType = EntityType.USER.name,
            entityId = id,
            summary = "MFA ${if (bool) "enabled" else "disabled"} for user $id",
            level = TelemetryLevel.INFO
        )
    }

    override suspend fun findSuspendedUsers(organizationId: String): List<User> {
        return repository.findSuspendedUsers(organizationId).map { resolveUserWithCapabilities(it) }
    }

    override suspend fun getAdminEmails(organizationId: String): List<String> {
        return repository.getAdminEmails(organizationId)
    }

    override suspend fun fetchPaged(
        query: PageQuery,
        organizationId: String
    ): PageResult<UserItem> {
        return repository.fetchPaged(query, organizationId)
    }

    override suspend fun getActiveSessions(organizationId: String): List<SessionItemDto> {
        return repository.getActiveSessions(organizationId)
    }

    override suspend fun getBindingsForUserId(
        userId: String,
        organizationId: String
    ): List<UserWorkspaceBinding> {
        return repository.getBindingsForUserId(userId, organizationId)
    }

    override suspend fun bindUserToWorkspace(
        request: BindUserToWorkspaceRequest,
        organizationId: String
    ): Result<Unit> = runCatching {
        repository.bindUserToWorkspace(request, organizationId).getOrThrow()
        idCache.synchronous().invalidate(request.userId)

        auditor.logApplicationEvent(
            action = "USER_WORKSPACE_BOUND",
            entityType = EntityType.USER.name,
            entityId = request.userId,
            summary = "User ${request.userId} bound to workspace ${request.workspaceId} with role ${request.role}",
            level = TelemetryLevel.INFO,
            organizationIdOverride = organizationId
        )
    }

    override suspend fun unbindUserFromWorkspace(
        request: UnbindUserFromWorkspaceRequest,
        organizationId: String
    ): Result<Unit> = runCatching {
        repository.unbindUserFromWorkspace(request, organizationId).getOrThrow()
        idCache.synchronous().invalidate(request.userId)
        auditor.logApplicationEvent(
            action = "USER_WORKSPACE_UNBOUND",
            entityType = EntityType.USER.name,
            entityId = request.userId,
            summary = "User ${request.userId} unbound from workspace ${request.workspaceId}",
            level = TelemetryLevel.INFO,
            organizationIdOverride = organizationId
        )
    }
}

/**
 * A suspending, atomic get-or-load function for Caffeine's AsyncCache.
 */
suspend fun <K : Any, V> AsyncCache<K, V>.getOrLoadSuspend(
    scope: CoroutineScope,
    key: K,
    loader: suspend (K) -> V
): V {
    val callerContext = currentCoroutineContext()
    val future = this.get(key) { k, _ ->
        scope.future(callerContext) {
            dbQuery {
                loader(k)
            }
        }
    }
    return future.await()
}
