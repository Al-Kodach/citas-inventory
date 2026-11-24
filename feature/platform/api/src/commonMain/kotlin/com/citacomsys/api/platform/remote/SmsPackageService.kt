package com.citacomsys.api.platform.remote

import com.citacomsys.api.platform.model.SmsPackage

interface SmsPackageService {
    suspend fun get(id: String): SmsPackage

    suspend fun getActive(): List<SmsPackage>

    suspend fun save(pkg: SmsPackage)

    suspend fun delete(id: String)
}