package com.citacomsys.core.persistence

import com.citacomsys.core.persistence.config.DbConfig
import com.typesafe.config.Config
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.ktor.ext.getKoin
import java.io.File

val persistenceModule = module {
    // Singletons for individual jobs
    single<DataMaintenanceJob> { SiemDataMaintenanceJob() }

    // The Factory collects all registered DataMaintenanceJob instances
    single { MaintenanceJobFactory(getAll()) }

    single {
        loadDatabaseConfig(get())
    }

    single {
        PostgresNotificationListener(get())
    }

    singleOf(::SiemDataMaintenanceJob) {
        bind<DataMaintenanceJob>()
    }


    single {
        val config = get<Config>()
        val files = config.getConfig("app.storage")
        val baseDir = File(files.getString("base_dir"))
        val backupsDir = File(baseDir, "backups").resolve("db")

        DatabaseFactory(
            config = get(),
            backupDir = backupsDir
        )
    }
}

internal fun loadDatabaseConfig(config: Config): DbConfig {
    return DbConfig(
        url = config.getString("database.url"),
        user = config.getString("database.user"),
        pass = config.getString("database.password"),
        poolSize = config.getInt("database.poolSize"),
        isProduction = config.getString("ktor.environment") in listOf("production", "prod")
    )
}