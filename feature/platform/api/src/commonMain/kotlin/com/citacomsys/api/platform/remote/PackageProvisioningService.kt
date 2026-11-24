package com.citacomsys.api.platform.remote

import com.citacomsys.api.platform.model.PkgProvisionRequest
import com.citacomsys.api.platform.model.ProvisionedPackage

interface PackageProvisioningService {
    suspend fun provision(request: PkgProvisionRequest): Result<ProvisionedPackage>
}