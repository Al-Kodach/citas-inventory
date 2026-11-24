package com.citacomsys.api.platform.repository

import com.citacomsys.api.platform.model.SmsPackage

interface SmsPackageRepository {

    suspend fun get(id: String): Result<SmsPackage>
    suspend fun getAll(): List<SmsPackage>
    suspend fun save(pkg: SmsPackage)

    suspend fun getActive(): List<SmsPackage>
}