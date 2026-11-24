package com.citacomsys.core.mesh.security

/**
 * Interface for verifying workstation signatures on the local hub.
 */
interface WorkstationSignatureVerifier {
    fun verify(
        publicKeyBase64: String,
        data: ByteArray,
        signatureBase64: String
    ): Boolean
}

expect fun createWorkstationSignatureVerifier(): WorkstationSignatureVerifier
