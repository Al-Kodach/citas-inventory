package com.citacomsys.core.database.dao.inventory

import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.database.WorkstationBindingEntity
import kotlinx.coroutines.flow.Flow

interface WorkstationDao {
    fun observeAll(): Flow<List<WorkstationBindingEntity>>
    suspend fun findById(hardwareId: String): WorkstationBindingEntity?
    suspend fun save(entity: WorkstationBindingEntity)
    suspend fun delete(hardwareId: String)
}

internal class SqlDelightWorkstationDao(
    private val manager: InventoryDatabaseManager
) : WorkstationDao {
    override fun observeAll(): Flow<List<WorkstationBindingEntity>> =
        manager.observesList { it.workstationQueries.selectAll() }

    override suspend fun findById(hardwareId: String): WorkstationBindingEntity? {
        val db = manager.getDatabase()
        return db.workstationQueries.selectById(hardwareId).executeAsOneOrNull()
    }

    override suspend fun save(entity: WorkstationBindingEntity) {
        val db = manager.getDatabase()
        db.transaction {
            db.workstationQueries.insertOrUpdate(entity)
        }
    }

    override suspend fun delete(hardwareId: String) {
        val db = manager.getDatabase()
        db.transaction {
            db.workstationQueries.deleteById(hardwareId)
        }
    }
}
