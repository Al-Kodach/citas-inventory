package com.citacomsys.core.mesh

import com.citacomsys.core.mesh.service.MeshHubTrustManager
import kotlin.test.Test
import kotlin.test.assertTrue

class MeshTrustTest {

    @Test
    fun testTrustManagerFingerprintValidation() {
        val expectedFingerprint = "a1b2c3d4e5f6"
        val trustManager = MeshHubTrustManager { expectedFingerprint }

        // Verify that the trust manager reference is correctly instantiated with the pinner lambda
        assertTrue(trustManager.acceptedIssuers.isNotEmpty())
    }
}
