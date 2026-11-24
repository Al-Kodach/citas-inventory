package com.citacomsys.core.domain

interface AppInfoProvider {
    val databaseSchemaVersion: Int
    val appVersion: String
}
