package com.citacomsys.feature.platform.service

import com.citacomsys.api.platform.model.SmsPackage
import com.citacomsys.api.platform.repository.SmsPackageRepository
import com.citacomsys.api.platform.remote.SmsPackageService

internal class SmsPackageServiceImpl(
    private val smsPackageRepository: SmsPackageRepository
): SmsPackageService {
    override suspend fun get(id: String): SmsPackage {
        return smsPackageRepository.get(id).getOrThrow()
    }

    override suspend fun getActive(): List<SmsPackage> {
        return smsPackageRepository.getActive()
    }

    override suspend fun save(pkg: SmsPackage) {
        smsPackageRepository.save(pkg)
    }

    override suspend fun delete(id: String) {
    }
}
