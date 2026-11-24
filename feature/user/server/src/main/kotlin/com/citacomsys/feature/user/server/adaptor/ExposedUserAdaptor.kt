package com.citacomsys.feature.user.server.adaptor

import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.core.persistence.query.queryPage
import com.citacomsys.core.persistence.table.tenant.Workspaces
import com.citacomsys.core.persistence.table.user.UserSessions
import com.citacomsys.core.persistence.table.user.UserWorkspaceBindings
import com.citacomsys.core.persistence.table.user.Users
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.inventory.api.user.UserPort
import com.citacomsys.inventory.api.user.model.BindUserToWorkspaceRequest
import com.citacomsys.inventory.api.user.model.SaveTenantUserRequest
import com.citacomsys.inventory.api.user.model.UnbindUserFromWorkspaceRequest
import com.citacomsys.types.ChangeListResponse
import com.citacomsys.types.DefaultCapabilities
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import com.citacomsys.types.PhoneNumber
import com.citacomsys.types.SessionItemDto
import com.citacomsys.types.User
import com.citacomsys.types.UserCapability
import com.citacomsys.types.UserListItem
import com.citacomsys.types.UserStatus
import com.citacomsys.types.UserWorkspaceBinding
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Instant

internal class ExposedUserAdaptor : UserPort {

    private fun parseCapabilities(raw: String?): Set<UserCapability> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.split(",")
            .mapNotNull { name -> runCatching { UserCapability.valueOf(name.trim()) }.getOrNull() }
            .toSet()
    }

    private fun formatCapabilities(caps: Set<UserCapability>): String {
        return caps.joinToString(",") { it.name }
    }

    override suspend fun updateBranch(userId: String, branchId: String): Unit = dbQuery {
        val userUuid = userId.toUuid()
        Users.update({ Users.id eq userUuid }) {
            it[updatedAt] = Clock.System.now()
        }
    }

    override suspend fun updateOrganization(userId: String, organizationId: String): Unit =
        dbQuery {
            val userUuid = userId.toUuid()
            val orgUuid = organizationId.toUuid()
            Users.update({ Users.id eq userUuid }) {
                it[this.organizationId] = orgUuid
                it[updatedAt] = Clock.System.now()
            }
        }

    override suspend fun findById(id: String): Result<User> = dbQuery {
        runCatching {
            Users.selectAll()
                .where { Users.id eq id.toUuid() }
                .singleOrNull()?.toUser()
                ?: throw NoSuchElementException("User not found with id: $id")
        }
    }

    override suspend fun save(request: SaveTenantUserRequest, organizationId: String): User =
        dbQuery {
            val userUuid = request.id.toUuid()

            val existing = Users.selectAll()
                .where { Users.id eq userUuid }
                .singleOrNull()

            val capsToSave = if (request.capabilities.isNotEmpty()) {
                request.capabilities
            } else {
                DefaultCapabilities.forRole(request.role)
            }
            val capsStr = formatCapabilities(capsToSave)

            if (existing != null) {
                Users.update({ Users.id eq userUuid }) {
                    it[name] = request.name
                    it[username] = request.username
                    it[phone] = request.phone.value
                    it[email] = request.email
                    it[role] = request.role
                    it[capabilities] = capsStr
                    it[userType] = request.type
                    it[updatedAt] = Clock.System.now()
                }
            } else {
                Users.upsert {
                    it[id] = userUuid
                    it[this.organizationId] = organizationId.toUuid()
                    it[name] = request.name
                    it[username] = request.username
                    it[phone] = request.phone.value
                    it[email] = request.email
                    it[role] = request.role
                    it[capabilities] = capsStr
                    it[status] = UserStatus.ACTIVE
                    it[userType] = request.type
                    it[mfaEnabled] = false
                    it[passwordHash] = request.password ?: ""
                    it[createdAt] = request.createdAt
                    it[updatedAt] = request.createdAt
                }
            }

            Users.selectAll().where { Users.id eq userUuid }.single().toUser()
        }

    override suspend fun save(user: User): User = dbQuery {
        val userUuid = user.id.toUuid()

        val existing = Users.selectAll()
            .where { Users.id eq userUuid }
            .singleOrNull()

        val capsStr = formatCapabilities(user.capabilities)

        if (existing != null) {
            Users.update({ Users.id eq userUuid }) {
                it[name] = user.name
                it[username] = user.username
                it[phone] = user.phone.value
                it[email] = user.email
                it[role] = user.role
                it[capabilities] = capsStr
                it[status] = user.status
                it[userType] = user.userType
                it[mfaEnabled] = user.mfaEnabled
                it[passwordHash] = user.passwordHash
                it[updatedAt] = Clock.System.now()
            }
        } else {
            Users.upsert {
                it[id] = userUuid
                it[organizationId] = user.organizationId.toUuid()
                it[name] = user.name
                it[username] = user.username
                it[phone] = user.phone.value
                it[email] = user.email
                it[role] = user.role
                it[capabilities] = capsStr
                it[status] = user.status
                it[userType] = user.userType
                it[mfaEnabled] = user.mfaEnabled
                it[passwordHash] = user.passwordHash
                it[createdAt] = user.createdAt
                it[updatedAt] = user.createdAt
            }
        }

        Users.selectAll().where { Users.id eq userUuid }.single().toUser()
    }

    override suspend fun getChangesSince(
        since: Instant?,
        organizationId: String
    ): ChangeListResponse<User, String> = dbQuery {
        val orgUuid = organizationId.toUuid()
        val query = if (since != null) {
            Users.selectAll().where {
                (Users.organizationId eq orgUuid) and
                        (Users.updatedAt greater since)
            }
        } else {
            Users.selectAll().where { Users.organizationId eq orgUuid }
        }

        val updated = query.map { it.toUser() }
        ChangeListResponse(updated = updated, deleted = emptyList())
    }

    override suspend fun findByPhone(phone: String): Result<User> = dbQuery {
        runCatching {
            Users.selectAll()
                .where { Users.phone eq PhoneNumber(phone).normalized }
                .singleOrNull()?.toUser()
                ?: throw NoSuchElementException("User not found with phone: $phone")
        }
    }

    override suspend fun findByUsername(identity: String): Result<User> = dbQuery {
        runCatching {
            Users.selectAll()
                .where { Users.username eq identity }
                .singleOrNull()?.toUser()
                ?: throw NoSuchElementException("User not found with username: $identity")
        }
    }

    override suspend fun findByIdentity(identity: String): Result<User> = dbQuery {
        runCatching {
            val phoneNormalized =
                runCatching { PhoneNumber(identity).normalized }.getOrDefault(identity)
            val condition: Op<Boolean> =
                (Users.username eq identity) or (Users.phone eq phoneNormalized) or (Users.email eq identity)
            Users.selectAll()
                .where { condition }
                .singleOrNull()?.toUser()
                ?: throw NoSuchElementException("User not found with identity: $identity")
        }
    }

    override suspend fun markPhoneNumberAsVerified(userId: String): Unit = dbQuery {
        Users.update({ Users.id eq userId.toUuid() }) {
            it[isPhoneVerified] = true
            it[updatedAt] = Clock.System.now()
        }
    }

    override suspend fun findOwner(organizationId: String): User? = dbQuery {
        Users.selectAll()
            .where {
                (Users.organizationId eq organizationId.toUuid()) and
                        (Users.role eq "OWNER")
            }
            .singleOrNull()?.toUser()
    }

    override suspend fun suspendUser(userId: String, reason: String): Result<User> = dbQuery {
        runCatching {
            val userUuid = userId.toUuid()
            Users.update({ Users.id eq userUuid }) {
                it[status] = UserStatus.SUSPENDED
                it[suspendedReason] = reason
                it[updatedAt] = Clock.System.now()
            }
            Users.selectAll().where { Users.id eq userUuid }.single().toUser()
        }
    }

    override suspend fun activateUser(userId: String): Result<User> = dbQuery {
        runCatching {
            val userUuid = userId.toUuid()
            Users.update({ Users.id eq userUuid }) {
                it[status] = UserStatus.ACTIVE
                it[suspendedReason] = null
                it[updatedAt] = Clock.System.now()
            }
            Users.selectAll().where { Users.id eq userUuid }.single().toUser()
        }
    }

    override suspend fun updatePassword(
        userId: String,
        newPasswordHash: String,
        forceChange: Boolean
    ): Boolean = dbQuery {
        val count = Users.update({ Users.id eq userId.toUuid() }) {
            it[passwordHash] = newPasswordHash
            it[updatedAt] = Clock.System.now()
        }
        count > 0
    }

    override suspend fun updateStatus(status: UserStatus, userId: String): Unit = dbQuery {
        Users.update({ Users.id eq userId.toUuid() }) {
            it[this.status] = status
            it[updatedAt] = Clock.System.now()
        }
    }

    override suspend fun delete(userId: String, actorId: String): Result<Unit> = dbQuery {
        runCatching {
            val userUuid = userId.toUuid()
            Users.deleteWhere { Users.id eq userUuid }
            UserWorkspaceBindings.deleteWhere { UserWorkspaceBindings.userId eq userUuid }
            Unit
        }
    }

    override suspend fun updateMfaEnabled(id: String, bool: Boolean): Unit = dbQuery {
        Users.update({ Users.id eq id.toUuid() }) {
            it[mfaEnabled] = bool
            it[updatedAt] = Clock.System.now()
        }
    }

    override suspend fun findSuspendedUsers(organizationId: String): List<User> = dbQuery {
        Users.selectAll()
            .where {
                (Users.organizationId eq organizationId.toUuid()) and
                        (Users.status eq UserStatus.SUSPENDED)
            }
            .map { it.toUser() }
    }

    override suspend fun getAdminEmails(organizationId: String): List<String> = dbQuery {
        Users.selectAll()
            .where {
                (Users.organizationId eq organizationId.toUuid()) and
                        ((Users.role eq "OWNER") or (Users.role eq "ADMIN"))
            }
            .mapNotNull { it[Users.email] }
    }

    override suspend fun fetchPaged(
        query: PageQuery,
        organizationId: String
    ): PageResult<UserListItem> {
        return Users.queryPage(
            pageQuery = query,
            searchColumns = listOf(Users.name, Users.username, Users.phone),
            filterBuilder = {
                val orgUuid = organizationId.toUuid()
                where { Users.organizationId eq orgUuid }
            },
            defaultSort = Users.createdAt,
            mapper = { row ->
                val emailVal = row[Users.email]
                val emailMaskedVal = emailVal?.let { email ->
                    val parts = email.split("@")
                    if (parts.size == 2 && parts[0].length > 2) {
                        parts[0].take(2) + "***@" + parts[1]
                    } else email
                }
                UserListItem(
                    id = row[Users.id].toString(),
                    name = row[Users.name],
                    username = row[Users.username],
                    phoneMasked = PhoneNumber(row[Users.phone]).mask,
                    emailMasked = emailMaskedVal,
                    status = row[Users.status],
                    role = row[Users.role],
                    createdAt = row[Users.createdAt].toString()
                )
            }
        )
    }

    override suspend fun getActiveSessions(organizationId: String): List<SessionItemDto> = dbQuery {
        UserSessions
            .innerJoin(Users)
            .selectAll()
            .where {
                (UserSessions.organizationId eq organizationId.toUuid()) and
                        (UserSessions.isRevoked eq false)
            }.map { row ->
                SessionItemDto(
                    sessionId = row[UserSessions.id].toString(),
                    username = row[Users.username],
                    deviceName = row[UserSessions.deviceId],
                    createdAt = row[UserSessions.createdAt],
                    ipAddr = row[UserSessions.lastIp] ?: "127.0.0.1",
                    platform = row[UserSessions.platform] ?: "Desktop"
                )
            }
    }

    override suspend fun getBindingsForUserId(
        userId: String,
        organizationId: String
    ): List<UserWorkspaceBinding> {
        return UserWorkspaceBindings
            .innerJoin(Users)
            .innerJoin(Workspaces)
            .selectAll()
            .where {
                (UserWorkspaceBindings.userId eq userId.toUuid()) and
                        (Users.organizationId eq organizationId.toUuid()) and
                        (UserWorkspaceBindings.isActive eq true)
            }
            .map { it.toUserWorkspaceBinding() }
    }

    override suspend fun bindUserToWorkspace(
        request: BindUserToWorkspaceRequest,
        organizationId: String
    ): Result<Unit> = dbQuery {
        runCatching {
            val userUuid = request.userId.toUuid()
            val wsUuid = request.workspaceId.toUuid()
            val branchUuid = request.branchId?.takeIf { it.isNotBlank() }?.toUuid()
            val bindingId = UUID.randomUUID()
            val now = Clock.System.now()

            val existing = UserWorkspaceBindings.selectAll()
                .where { (UserWorkspaceBindings.userId eq userUuid) and (UserWorkspaceBindings.workspaceId eq wsUuid) }
                .singleOrNull()

            val capsToSave = if (request.useOrgCapabilities) {
                val userRow = Users.selectAll().where { Users.id eq userUuid }.singleOrNull()
                val userCaps =
                    userRow?.let { parseCapabilities(it[Users.capabilities]) } ?: emptySet()
                if (userCaps.isNotEmpty()) userCaps else DefaultCapabilities.forRole(request.role)
            } else {
                if (request.capabilities.isNotEmpty()) request.capabilities else DefaultCapabilities.forRole(request.role)
            }
            val capsStr = formatCapabilities(capsToSave)

            if (existing != null) {
                UserWorkspaceBindings.update({ (UserWorkspaceBindings.userId eq userUuid) and (UserWorkspaceBindings.workspaceId eq wsUuid) }) {
                    it[role] = request.role
                    it[capabilities] = capsStr
                    it[branchId] = branchUuid
                    it[isActive] = true
                    it[updatedAt] = now
                }
            } else {
                UserWorkspaceBindings.insert {
                    it[id] = bindingId
                    it[userId] = userUuid
                    it[workspaceId] = wsUuid
                    it[branchId] = branchUuid
                    it[role] = request.role
                    it[capabilities] = capsStr
                    it[isActive] = true
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
            Unit
        }
    }

    override suspend fun unbindUserFromWorkspace(
        request: UnbindUserFromWorkspaceRequest,
        organizationId: String
    ): Result<Unit> = dbQuery {
        runCatching {
            val userUuid = request.userId.toUuid()
            val wsUuid = request.workspaceId.toUuid()

            UserWorkspaceBindings.update({ (UserWorkspaceBindings.userId eq userUuid) and (UserWorkspaceBindings.workspaceId eq wsUuid) }) {
                it[isActive] = false
                it[updatedAt] = Clock.System.now()
            }
            Unit
        }
    }

    private fun ResultRow.toUser(): User {
        return User(
            id = this[Users.id].toString(),
            organizationId = this[Users.organizationId].toString(),
            name = this[Users.name],
            username = this[Users.username],
            role = this[Users.role],
            capabilities = parseCapabilities(this[Users.capabilities]),
            userType = this[Users.userType],
            email = this[Users.email],
            passwordHash = this[Users.passwordHash],
            status = this[Users.status],
            createdAt = this[Users.createdAt],
            updatedAt = this[Users.updatedAt],
            mfaEnabled = this[Users.mfaEnabled],
            phone = PhoneNumber(this[Users.phone]),
            isPhoneVerified = this[Users.isPhoneVerified],
            suspendedReason = this[Users.suspendedReason],
            deletedAt = this[Users.deletedAt]
        )
    }

    private fun ResultRow.toUserWorkspaceBinding(): UserWorkspaceBinding {
        return UserWorkspaceBinding(
            id = this[UserWorkspaceBindings.id].toString(),
            userId = this[UserWorkspaceBindings.userId].toString(),
            workspaceId = this[UserWorkspaceBindings.workspaceId].toString(),
            branchId = this[UserWorkspaceBindings.branchId]?.toString(),
            role = this[UserWorkspaceBindings.role],
            workspaceName = this[Workspaces.name],
            branchName = this[Workspaces.name],
            capabilities = parseCapabilities(this[UserWorkspaceBindings.capabilities]),
            isActive = this[UserWorkspaceBindings.isActive],
            createdAt = this[UserWorkspaceBindings.createdAt],
            updatedAt = this[UserWorkspaceBindings.updatedAt]
        )
    }
}
