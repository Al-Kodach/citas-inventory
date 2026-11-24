package com.citacomsys.device.service

interface BiometricService {

    /**
     * Returns true if biometric is available AND enrolled
     */
    fun isAvailable(): Boolean

    /**
     * Performs biometric authentication.
     * Returns true only if the user is verified.
     */
    suspend fun authenticate(reason: String): Boolean
}