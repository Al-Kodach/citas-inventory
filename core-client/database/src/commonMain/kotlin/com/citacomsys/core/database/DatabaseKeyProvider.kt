package com.citacomsys.core.database

import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.getOrCreateSecret
import io.github.aakira.napier.Napier
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Centralized provider for the database master encryption key.
 * Anchors security to the KSafe vault and derives database-specific keys on demand.
 */
class DatabaseKeyProvider(private val vault: KSafe) {
    private val mutex = Mutex()

    /**
     * Derives the database passphrase on demand from the hardware-bound master key.
     */
    suspend fun getPassphrase(): ByteArray = mutex.withLock {
        Napier.v { "Deriving Database Passphrase from Master Secret..." }
        val dbKey = vault.getOrCreateSecret("database_master_key")
        return dbKey
    }
}
