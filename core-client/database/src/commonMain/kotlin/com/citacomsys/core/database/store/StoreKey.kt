package com.citacomsys.core.database.store

import kotlin.time.Instant


/**
 * A unified key pattern for Store operations.
 */
sealed interface StoreKey {
    data class ChangeList(val since: Instant?) : StoreKey
    data class ChangeListFor(val id: Set<String>, val since: Instant?) : StoreKey
    data class Delete(val ids: List<String>) : StoreKey
    data object DeleteAll : StoreKey
}
