package com.citacomsys.core.database.dao

import com.citacomsys.core.database.CustomerDatabaseManager
import com.citacomsys.customer.database.UserEntity
import com.citacomsys.shared.util.JsonObjAdaptor
import com.citacomsys.types.PhoneNumber
import com.citacomsys.types.User
import com.citacomsys.types.UserCapability
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

interface UserDao {
    fun findById(id: String): Flow<UserEntity?>
    fun findByPhone(phone: String): Flow<UserEntity?>
    suspend fun save(entity: UserEntity)
    suspend fun delete(id: String)
    suspend fun getMaxVersionTimestamp(): Instant?

    companion object {
        fun asEntity(user: User): UserEntity =
            UserEntity(
                id = user.id,
                phone = user.phone.value,
                email = user.email,
                username = user.username,
                user_type = user.userType,
                organization_id = user.organizationId,
                mfa_enabled = user.mfaEnabled,
                is_phone_verified = user.isPhoneVerified,
                role = user.role,
                capabilities = JsonObjAdaptor.encode<Set<UserCapability>>(user.capabilities),
                status = user.status,
                suspendReason = user.suspendedReason,
                created_at = user.createdAt,
                deleted_at = user.deletedAt,
                last_updated_at = user.updatedAt
            )

        fun asDomain(entity: UserEntity) = User(
            id = entity.id,
            organizationId = entity.organization_id,
            name = entity.username,
            username = entity.username,
            email = entity.email,
            phone = PhoneNumber(entity.phone),
            role = entity.role,
            userType = entity.user_type,
            status = entity.status,
            mfaEnabled = entity.mfa_enabled,
            createdAt = entity.created_at,
            updatedAt = entity.last_updated_at,
            isPhoneVerified = entity.is_phone_verified,
            deletedAt = entity.deleted_at,
            suspendedReason = entity.suspendReason,
            passwordHash = "",
            capabilities = JsonObjAdaptor.decode<Set<UserCapability>>(entity.capabilities)!!
        )
    }
}

internal class SqlUserDao(
    private val manager: CustomerDatabaseManager
) : UserDao {

    override fun findById(id: String): Flow<UserEntity?> {
        return manager.observeSingle { it.usersQueries.selectById(id) }
    }

    override fun findByPhone(phone: String): Flow<UserEntity?> =
        manager.observeSingle { it.usersQueries.selectByPhone(phone) }

    override suspend fun save(entity: UserEntity) {
        val db = manager.getDatabase()
        db.transaction {
            db.usersQueries.insertOrReplace(entity)
        }
    }

    override suspend fun delete(id: String) {
        val db = manager.getDatabase()
        db.transaction {
            db.usersQueries.deleteById(id)
        }
    }

    override suspend fun getMaxVersionTimestamp(): Instant? {
        val db = manager.getDatabase()
        return db.transactionWithResult {
            db.usersQueries.getMaxUpdatedAt().executeAsOneOrNull()?.MAX
        }
    }
}
