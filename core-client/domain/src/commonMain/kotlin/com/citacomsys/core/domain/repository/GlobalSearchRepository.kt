package com.citacomsys.core.domain.repository

import com.citacomsys.types.EntityType
import kotlinx.coroutines.flow.Flow

data class SearchResult(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: EntityType,
    val metadata: Map<String, String> = emptyMap()
)

interface GlobalSearchProvider {
    fun search(query: String): Flow<List<SearchResult>>
}

interface GlobalSearchRepository {
    fun search(query: String): Flow<List<SearchResult>>
    fun registerProvider(provider: GlobalSearchProvider)
}
