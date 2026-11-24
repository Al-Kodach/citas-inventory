package com.citacomsys.core.security.crypto

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

class SignatureIntegrationTest {

    @Test
    fun testRealRsaSignatureVerificationPipeline() {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val kp = kpg.generateKeyPair()

        val publicKeyBase64 = Base64.getEncoder().encodeToString(kp.public.encoded)

        val verificationPayload = "test-hardware-123:test-workspace-456:${System.currentTimeMillis()}"
        val sig = Signature.getInstance("SHA256withRSA")
        sig.initSign(kp.private)
        sig.update(verificationPayload.encodeToByteArray())
        val signatureBase64 = Base64.getEncoder().encodeToString(sig.sign())

        val isValid = SignatureVerifier.verify(
            publicKeyBase64 = publicKeyBase64,
            data = verificationPayload.encodeToByteArray(),
            signatureBase64 = signatureBase64
        )
        assertTrue(isValid)

        val isTamperedValid = SignatureVerifier.verify(
            publicKeyBase64 = publicKeyBase64,
            data = "tampered-payload".encodeToByteArray(),
            signatureBase64 = signatureBase64
        )
        assertFalse(isTamperedValid)
    }
}
