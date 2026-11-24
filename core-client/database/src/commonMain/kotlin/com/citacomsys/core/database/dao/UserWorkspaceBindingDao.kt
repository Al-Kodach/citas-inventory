package com.citacomsys.core.database.dao

import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.database.UserWorkspaceBindingEntity
import com.citacomsys.shared.util.JsonObjAdaptor
import com.citacomsys.types.UserCapability
import com.citacomsys.types.UserWorkspaceBinding
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

interface UserWorkspaceBindingDao {
    suspend fun saveAll(bindings: List<UserWorkspaceBinding>)
    fun getBindingsForUser(userId: String): Flow<List<UserWorkspaceBinding>>

    companion object {
        fun asEntity(binding: UserWorkspaceBinding): UserWorkspaceBindingEntity =
            UserWorkspaceBindingEntity(
                id = binding.id,
                user_id = binding.userId,
                workspace_id = binding.workspaceId,
                workspace_name = binding.workspaceName,
                branch_id = binding.branchId,
                branch_name = binding.branchName,
                role = binding.role,
                capabilities = JsonObjAdaptor.encode<Set<UserCapability>>(binding.capabilities),
                is_active = binding.isActive,
                created_at = binding.createdAt,
                updated_at = binding.updatedAt
            )

        fun asDomain(
            id: String,
            user_id: String,
            workspace_id: String,
            workspace_name: String,
            branch_id: String?,
            branch_name: String?,
            role: String,
            capabilities: String,
            is_active: Boolean,
            created_at: Instant,
            updated_at: Instant
        ): UserWorkspaceBinding =
            UserWorkspaceBinding(
                id = id,
                userId = user_id,
                workspaceId = workspace_id,
                workspaceName = workspace_name,
                branchId = branch_id,
                branchName = branch_name,
                role = role,
                capabilities = JsonObjAdaptor.decode<Set<UserCapability>>(capabilities)
                    ?: emptySet(),
                isActive = is_active,
                createdAt = created_at,
                updatedAt = updated_at
            )
    }
}

internal class SqlDelightUserWorkspaceBindingDao(
    private val manager: InventoryDatabaseManager
) : UserWorkspaceBindingDao {

    override suspend fun saveAll(bindings: List<UserWorkspaceBinding>) {
        val db = manager.getDatabase()
        db.transaction {
            bindings.forEach { binding ->
                db.userWorkspaceBindingQueries.insertOrReplace(
                    UserWorkspaceBindingDao.asEntity(binding)
                )
            }
        }
    }

    override fun getBindingsForUser(userId: String): Flow<List<UserWorkspaceBinding>> {
        return manager.observesList { db ->
            db.userWorkspaceBindingQueries.selectByUser(userId, ::mapToDomain)
        }
    }

    private fun mapToDomain(
        id: String,
        user_id: String,
        workspace_id: String,
        workspace_name: String,
        branch_id: String?,
        branch_name: String?,
        role: String,
        capabilities: String,
        is_active: Boolean,
        created_at: Instant,
        updated_at: Instant
    ): UserWorkspaceBinding {
        return UserWorkspaceBindingDao.asDomain(
            id = id,
            user_id = user_id,
            workspace_id = workspace_id,
            workspace_name = workspace_name,
            branch_id = branch_id,
            branch_name = branch_name,
            role = role,
            capabilities = capabilities,
            is_active = is_active,
            created_at = created_at,
            updated_at = updated_at
        )
    }
}
