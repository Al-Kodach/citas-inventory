package com.citacomsys.core.database.dao.sale

import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.database.PosCartEntity
import com.citacomsys.database.PosCartItemEntity
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

interface PosCartDao {
    fun observeAllCarts(workspaceId: String, branchId: String): Flow<List<PosCartEntity>>
    fun observeActiveCart(workspaceId: String, branchId: String): Flow<PosCartEntity?>
    suspend fun getActiveCart(workspaceId: String, branchId: String): PosCartEntity?
    suspend fun getItemsForCart(cartId: String): List<PosCartItemEntity>
    
    suspend fun saveCart(cart: PosCartEntity, items: List<PosCartItemEntity>)
    suspend fun setActiveCart(cartId: String, workspaceId: String, branchId: String)
    suspend fun updateCartTimestamp(cartId: String, timestamp: Instant)
    suspend fun deleteCart(cartId: String, workspaceId: String, branchId: String)
    suspend fun clearAllCarts()
}

internal class SqlDelightPosCartDao(
    private val manager: InventoryDatabaseManager
) : PosCartDao {

    override fun observeAllCarts(workspaceId: String, branchId: String): Flow<List<PosCartEntity>> = 
        manager.observesList { it.posCartQueries.selectAllCarts(workspaceId, branchId) }

    override fun observeActiveCart(workspaceId: String, branchId: String): Flow<PosCartEntity?> = 
        manager.observeSingle { it.posCartQueries.selectActiveCart(workspaceId, branchId) }

    override suspend fun getActiveCart(workspaceId: String, branchId: String): PosCartEntity? {
        val db = manager.getDatabase()
        return db.transactionWithResult {
            db.posCartQueries.selectActiveCart(workspaceId, branchId).executeAsOneOrNull()
        }
    }

    override suspend fun getItemsForCart(cartId: String): List<PosCartItemEntity> {
        val db = manager.getDatabase()
        return db.transactionWithResult {
            db.posCartQueries.selectItemsForCart(cartId).executeAsList()
        }
    }

    override suspend fun saveCart(cart: PosCartEntity, items: List<PosCartItemEntity>) {
        val db = manager.getDatabase()
        db.transaction {
            db.posCartQueries.insertCart(
                PosCartEntity(
                    id = cart.id,
                    workspace_id = cart.workspace_id,
                    branch_id = cart.branch_id,
                    name = cart.name,
                    last_updated_at = cart.last_updated_at,
                    is_active = cart.is_active,
                    customer_id = cart.customer_id,
                    customer_name = cart.customer_name,
                    customer_phone = cart.customer_phone
                )
            )
            db.posCartQueries.deleteCartItems(cart.id)
            items.forEach { db.posCartQueries.insertCartItem(it) }
        }
    }

    override suspend fun setActiveCart(cartId: String, workspaceId: String, branchId: String) {
        val db = manager.getDatabase()
        db.transaction {
            db.posCartQueries.setActiveCart(id = cartId, workspaceId = workspaceId, branchId = branchId)
        }
    }

    override suspend fun updateCartTimestamp(cartId: String, timestamp: Instant) {
        val db = manager.getDatabase()
        db.transaction {
            db.posCartQueries.updateCartTimestamp(timestamp, cartId)
        }
    }

    override suspend fun deleteCart(cartId: String, workspaceId: String, branchId: String) {
        val db = manager.getDatabase()
        db.transaction {
            db.posCartQueries.deleteCart(id = cartId, workspaceId = workspaceId, branchId = branchId)
        }
    }

    override suspend fun clearAllCarts() {
        val db = manager.getDatabase()
        db.transaction {
            db.posCartQueries.deleteAllCarts()
        }
    }
}
