package com.citacomsys.core.mesh.security

import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

internal class AndroidWorkstationSignatureVerifier : WorkstationSignatureVerifier {
    override fun verify(
        publicKeyBase64: String,
        data: ByteArray,
        signatureBase64: String
    ): Boolean {
        return try {
            val keyBytes = Base64.getDecoder().decode(publicKeyBase64)
            val keySpec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            val publicKey = keyFactory.generatePublic(keySpec)

            val sig = Signature.getInstance("SHA256withRSA")
            sig.initVerify(publicKey)
            sig.update(data)
            sig.verify(Base64.getDecoder().decode(signatureBase64))
        } catch (e: Exception) {
            false
        }
    }
}

actual fun createWorkstationSignatureVerifier(): WorkstationSignatureVerifier = AndroidWorkstationSignatureVerifier()
