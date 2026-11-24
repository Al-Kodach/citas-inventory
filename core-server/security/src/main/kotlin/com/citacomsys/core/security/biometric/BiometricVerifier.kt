package com.citacomsys.core.security.biometric

interface BiometricVerifier {
    // Verifies that a signature was correctly created by the user's private key
    // for a given challenge.
    suspend fun verify(userId: String, challenge: String, signature: String): Boolean
}

class BiometricVerifierImpl: BiometricVerifier {
    override suspend fun verify(
        userId: String,
        challenge: String,
        signature: String
    ): Boolean {
        return false
    }

}