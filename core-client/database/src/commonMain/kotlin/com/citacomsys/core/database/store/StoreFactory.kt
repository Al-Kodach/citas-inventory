package com.citacomsys.core.database.store

import com.citacomsys.core.database.dao.BaseDao
import com.citacomsys.types.ChangeListResponse
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

object StoreFactory {

    /**
     * Creates a Store that handles incremental synchronization via ChangeListResponse.
     *
     * @param DOMAIN The high-level UI model
     * @param DTO The raw network model from the server
     * @param Entity The local database entity
     * @param ID The unique identifier type
     */
    inline fun <reified DOMAIN, reified Entity : Any, reified VIEW, reified ID : Any> createStore(
        dao: BaseDao<Entity, VIEW, ID>,
        crossinline fetcher: suspend (StoreKey) -> ChangeListResponse<DOMAIN, ID>,
        crossinline mapToEntity: (DOMAIN) -> Entity,
    ): Store<StoreKey, List<VIEW>> {

        return StoreBuilder.from<StoreKey, ChangeListResponse<DOMAIN, ID>, List<VIEW>>(
            fetcher = Fetcher.of { key -> fetcher(key) },
            sourceOfTruth = SourceOfTruth.of<StoreKey, ChangeListResponse<DOMAIN, ID>, List<VIEW>>(
                reader = { _ ->
                    dao.observeAll()
                },
                writer = { _, result ->
                    // Process Batch Deletions first to prevent stale references
                    if (result.deleted.isNotEmpty()) {
                    }

                    // Upsert newly created or updated items
                    if (result.updated.isNotEmpty()) {
                    }
                },
                delete = { key: Any ->
                    if (key is StoreKey.Delete) {
                    }
                },
                deleteAll = { }
            )
        ).build()
    }
}

