package com.citacomsys.device.service

internal class DesktopBiometricService : BiometricService {

    override fun isAvailable(): Boolean = false

    override suspend fun authenticate(reason: String): Boolean = false
}