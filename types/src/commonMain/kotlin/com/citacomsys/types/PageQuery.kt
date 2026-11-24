package com.citacomsys.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("query")
data class PageQuery(
    val page: Int = 0,
    val size: Int = 20,
    val sortBy: String? = null,
    val filters: Map<String, String>? = null,
    val workspaceId: String? = null,
    val branchId: String? = null,
    val search: String? = null,
    val sortDirection: Order = Order.DESC
)

@Serializable
enum class Order { ASC, DESC }

@Serializable
@SerialName("page")
data class PageResult<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totalPages: Int = if (size > 0) ((totalItems + size - 1) / size).toInt() else 0,
    val isLast: Boolean = (page + 1) >= totalPages,
    val isFirst: Boolean = page == 0
) {
    val nextPageNumber: Int?
        get() = if (isLast) null else page + 1

    companion object {
        fun <T> empty() = PageResult<T>(emptyList(), 0, 0, 0)
    }
}


