package com.citacomsys.device.default

import com.citacomsys.device.service.BiometricService

internal class DefaultBiometricService : BiometricService {

    override fun isAvailable(): Boolean = false

    override suspend fun authenticate(reason: String): Boolean = false
}