package com.citacomsys.core.persistence.query

import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.types.Order
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

/**
 * A generic utility to perform paginated, searchable, and filterable queries on any Exposed table or join.
 *
 * @param pageQuery The standard SaaS pagination and search parameters.
 * @param searchColumns List of columns or expressions to apply the search against.
 *                      Uses 'out String?' to natively support both nullable and non-nullable string columns.
 * @param filterBuilder Lambda to apply additional WHERE clauses to the query.
 * @param sortMap Mapping of sortBy strings (from client) to actual database columns.
 * @param defaultSort The column to sort by if no valid sortBy is provided.
 * @param mapper Transformer from ResultRow to the desired domain/DTO type.
 */
@Suppress("UNCHECKED_CAST")
suspend inline fun <reified T : ColumnSet, reified R> T.queryPage(
    pageQuery: PageQuery,
    searchColumns: List<Expression<out String?>> = emptyList(),
    crossinline filterBuilder: Query.() -> Query = { this },
    sortMap: Map<String, Column<*>> = emptyMap(),
    defaultSort: Column<*>,
    crossinline mapper: (ResultRow) -> R
): PageResult<R> = dbQuery {
    val query = this@queryPage.selectAll()

    // Apply Search (Across specified columns using ILIKE and OR)
    pageQuery.search?.takeIf { it.isNotBlank() }?.let { search ->
        val term = "%${search.lowercase()}%"
        if (searchColumns.isNotEmpty()) {
            query.andWhere {
                searchColumns.map { col ->
                    // Explicitly cast to Expression<String?> to satisfy lowerCase() / like bounds
                    val expr = col as Expression<String?>
                    (expr.lowerCase() like term) as Op<Boolean>
                }.reduce { acc, op -> acc or op }
            }
        }
    }

    // Apply Custom Filters and logic
    query.filterBuilder()

    // Capture total count before applying limit/offset
    val total = query.count()

    // Apply Sorting
    val sortCol = pageQuery.sortBy?.let { sortMap[it] } ?: defaultSort
    val sortDir = if (pageQuery.sortDirection == Order.ASC) SortOrder.ASC else SortOrder.DESC
    query.orderBy(sortCol, sortDir)

    // Apply Pagination (Supports both 1-based and 0-based page index)
    val zeroBasedPage = if (pageQuery.page > 1) pageQuery.page - 1 else 0
    val offset = (zeroBasedPage * pageQuery.size).toLong()
    val items = query
        .limit(pageQuery.size)
        .offset(offset)
        .map(mapper)

    PageResult(
        items = items,
        page = if (pageQuery.page <= 0) 1 else pageQuery.page,
        size = pageQuery.size,
        totalItems = total
    )
}
