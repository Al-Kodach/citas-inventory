package com.citacomsys.core.mesh

import com.citacomsys.core.mesh.service.MeshHubTrustManager
import io.mockk.mockk
import java.security.cert.X509Certificate
import kotlin.test.Test
import kotlin.test.assertFailsWith

class MeshTrustAuditTest {

    @Test
    fun testRogueNodeTlsFingerprintMismatchRejection() {
        val expectedFingerprint = "SHA256:CORRECT_FINGERPRINT"
        val trustManager = MeshHubTrustManager { expectedFingerprint }

        assertFailsWith<Exception> {
            val dummyCert = mockk<X509Certificate>(relaxed = true)
            trustManager.checkServerTrusted(arrayOf(dummyCert), "RSA")
        }
    }
}
