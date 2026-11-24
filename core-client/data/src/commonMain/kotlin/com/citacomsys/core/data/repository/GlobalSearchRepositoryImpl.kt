package com.citacomsys.core.data.repository

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import com.citacomsys.core.database.dao.inventory.ProductDao
import com.citacomsys.core.database.dao.sale.SalesDao
import com.citacomsys.core.domain.repository.GlobalSearchProvider
import com.citacomsys.core.domain.repository.GlobalSearchRepository
import com.citacomsys.core.domain.repository.SearchResult
import com.citacomsys.types.EntityType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal class GlobalSearchRepositoryImpl(
    private val productDao: ProductDao,
    private val salesDao: SalesDao
) : GlobalSearchRepository {

    private val providers = mutableStateOf<Set<GlobalSearchProvider>>(emptySet())

    override fun registerProvider(provider: GlobalSearchProvider) {
        providers.value += provider
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun search(query: String): Flow<List<SearchResult>> {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return flowOf(emptyList())

        val coreSearch = combine(
            productDao.search(cleanQuery).catch { emit(emptyList()) },
            salesDao.search(cleanQuery).catch { emit(emptyList()) }
        ) { products, sales ->
            val results = mutableListOf<SearchResult>()

            products.forEach { p ->
                results.add(
                    SearchResult(
                        id = p.id,
                        title = p.name,
                        subtitle = "Product • ${p.brandName} • ${p.totalAvailable} in stock",
                        type = EntityType.PRODUCT,
                        metadata = mapOf("price" to "${p.currentPriceAmount} ${p.currentPriceCurrency}")
                    )
                )
            }

            sales.forEach { s ->
                results.add(
                    SearchResult(
                        id = s.id,
                        title = "Sale #${s.id.take(8)}",
                        subtitle = "Sale • ${s.status} • ${s.actorName ?: "System"}",
                        type = EntityType.SALE,
                        metadata = mapOf("amount" to "${s.total_amount_minor} ${s.total_amount_currency}")
                    )
                )
            }
            results
        }

        return snapshotFlow { providers.value }.flatMapLatest { providerSet ->
            if (providerSet.isEmpty()) return@flatMapLatest coreSearch.map { it.sortedBy { r -> r.title } }

            val flows = listOf(coreSearch) + providerSet.map { provider ->
                runCatching { provider.search(cleanQuery) }
                    .getOrDefault(flowOf(emptyList()))
                    .catch { emit(emptyList()) }
            }
            combine(flows) { arrays ->
                arrays.flatMap { it }
                    .distinctBy { "${it.type}:${it.id}" }
                    .sortedBy { it.title }
            }
        }
    }
}
