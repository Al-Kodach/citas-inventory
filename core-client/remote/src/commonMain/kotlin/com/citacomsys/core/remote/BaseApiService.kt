package com.citacomsys.core.remote

import com.citacomsys.types.ChangeListResponse
import kotlin.time.Instant

interface BaseApiService<Dto, ID> {

     suspend fun fetchByIds(ids: Set<ID>): List<Dto>

     suspend fun fetchSince(sinceTimestamp: Instant?): ChangeListResponse<Dto, ID>
    suspend fun update(id: ID, dto: Dto): Unit

    suspend fun delete(id: ID)

    suspend fun create(request: Dto)

    suspend fun getById(id: ID): Dto
}