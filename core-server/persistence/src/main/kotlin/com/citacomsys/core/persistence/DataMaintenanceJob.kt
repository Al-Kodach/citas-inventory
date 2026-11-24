package com.citacomsys.core.persistence

import kotlinx.coroutines.CoroutineScope

interface DataMaintenanceJob {
    val identity: String
    fun run(scope: CoroutineScope)
}