package com.citacomsys.core.persistence

import com.citacomsys.core.persistence.plugin.Transactional
import io.ktor.server.application.Application
import io.ktor.server.application.install
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.koin.ktor.ext.inject
import kotlin.getValue

private val logger = KotlinLogging.logger("com.citacomsys.CORE-PERSISTENCE")

fun Application.configurePersistence() {
    logger.info { "Configuring database..." }

    val dbFactory by inject<DatabaseFactory>()
    runBlocking {
        dbFactory.init()
    }

    logger.info { "Registering transaction plugin..." }
    install(Transactional)

    // Start the Maintenance Factory (SIEM Purge, etc.)
    val maintenanceFactory by inject<MaintenanceJobFactory>()
    maintenanceFactory.startAll(this)
}


