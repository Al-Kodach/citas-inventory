package com.citacomsys.core.data.local

import com.citacomsys.core.domain.AppInfoProvider
import com.citacomsys.database.InventoryDatabase
import com.citacomsys.shared.BuildVersionConfig

internal class AppInfoProviderImpl : AppInfoProvider {
    override val databaseSchemaVersion: Int
        get() = InventoryDatabase.Schema.version.toInt()

    override val appVersion: String
        get() = BuildVersionConfig.APP_VERSION
}
