package com.citacomsys.device.service

import android.content.Context


internal class AndroidBiometricService(
    private val context: Context
) : BiometricService {

    override fun isAvailable(): Boolean = true

    override suspend fun authenticate(reason: String): Boolean = true
}