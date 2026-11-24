package com.citacomsys.core.database.dao

import kotlinx.coroutines.flow.Flow

/**
 * Base interface for all DAOs to support Store integration and Paging.
 */
interface BaseDao<ENTITY, LISTVIEW, ID> {
    fun observeAll(): Flow<List<LISTVIEW>>
    fun search(query: String): Flow<List<LISTVIEW>>

    fun findById(id: ID): Flow<ENTITY?>

    suspend fun count(): Flow<Long?>
}
