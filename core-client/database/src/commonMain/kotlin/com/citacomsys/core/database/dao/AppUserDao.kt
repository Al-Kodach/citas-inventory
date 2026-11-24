package com.citacomsys.core.database.dao

import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.database.AppUserEntity
import com.citacomsys.database.UserListItem
import com.citacomsys.shared.util.JsonObjAdaptor
import com.citacomsys.types.User
import com.citacomsys.types.UserCapability
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Clock
import kotlin.time.Instant

interface AppUserDao : BaseDao<AppUserEntity, UserListItem, String> {
    suspend fun save(entity: AppUserEntity)
    suspend fun suspend(id: String, reason: String)
    suspend fun activate(id: String)
    suspend fun delete(id: String)
    suspend fun getMaxVersionTimestamp(): Instant?
    suspend fun countActiveUsers(): Int

    companion object {
        fun asEntity(user: User): AppUserEntity =
            AppUserEntity(
                id = user.id,
                name = user.name,
                username = user.username,
                email_masked = user.email?.let {
                    val parts = it.split("@")
                    if (parts.size == 2 && parts[0].length > 2) parts[0].take(2) + "***@" + parts[1] else "***@org.com"
                },
                phone_masked = user.phone.value.let {
                    if (it.length > 4) it.take(3) + "****" + it.takeLast(2) else "****"
                },
                organization_id = user.organizationId,
                role = user.role,
                user_type = user.userType,
                status = user.status,
                suspendReason = user.suspendedReason,
                capabilities = JsonObjAdaptor.encode<Set<UserCapability>>(user.capabilities),
                created_at = user.createdAt,
                last_updated_at = user.updatedAt
            )
    }
}

internal class SqlAppUserDao(
    private val manager: InventoryDatabaseManager
) : AppUserDao {

    override fun findById(id: String): Flow<AppUserEntity?> {
        return manager.observeSingle { it.usersQueries.selectById(id) }
    }

    override suspend fun count(): Flow<Long?> {
        return flowOf(0)
    }

    override suspend fun save(entity: AppUserEntity) {
        val db = manager.getDatabase()
        db.transaction {
            db.usersQueries.insertOrReplace(entity)
        }
    }

    override fun observeAll(): Flow<List<UserListItem>> =
        manager.observesList { it.usersQueries.selectAll() }

    override fun search(query: String): Flow<List<UserListItem>> =
        manager.observesList { it.usersQueries.search(query) }

    override suspend fun getMaxVersionTimestamp(): Instant? {
        val db = manager.getDatabase()
        return db.transactionWithResult {
            db.usersQueries.getMaxUpdatedAt().executeAsOneOrNull()?.MAX
        }
    }

    override suspend fun delete(id: String) {
        val db = manager.getDatabase()
        db.transaction {
            db.usersQueries.deleteById(id)
        }
    }

    override suspend fun suspend(id: String, reason: String) {
        val db = manager.getDatabase()
        db.transaction {
            db.usersQueries.suspend(reason, Clock.System.now(), id)
        }
    }

    override suspend fun activate(id: String) {
        val db = manager.getDatabase()
        db.transaction {
            db.usersQueries.activate(Clock.System.now(), id)
        }
    }

    override suspend fun countActiveUsers(): Int {
        val db = manager.getDatabase()
        return db.transactionWithResult {
            db.usersQueries.countActiveUsers().executeAsOneOrNull()?.toInt() ?: 0
        }
    }
}
