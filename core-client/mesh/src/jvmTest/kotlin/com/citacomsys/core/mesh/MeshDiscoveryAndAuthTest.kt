package com.citacomsys.core.mesh

import com.citacomsys.core.mesh.security.MeshCryptoManager
import com.citacomsys.core.mesh.security.createWorkstationSignatureVerifier
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MeshDiscoveryAndAuthTest {

    @Test
    fun testDiscoveryTokenOrganizationFiltering() {
        val expectedOrgId = "org-enterprise-001"
        val secret = "enterprise-secret-key".encodeToByteArray()

        val validToken = MeshCryptoManager.generateDiscoveryToken(expectedOrgId, secret)
        val maliciousToken = MeshCryptoManager.generateDiscoveryToken("org-foreign-hacker", secret)

        // Verify valid token belongs to expected org
        assertTrue(MeshCryptoManager.verifyDiscoveryToken(validToken, expectedOrgId, secret))

        // Verify malicious token fails validation against expected org
        assertFalse(MeshCryptoManager.verifyDiscoveryToken(maliciousToken, expectedOrgId, secret))
    }

    @Test
    fun testWorkstationSignatureVerifierCreation() {
        val verifier = createWorkstationSignatureVerifier()
        assertNotNull(verifier, "Platform workstation signature verifier should be successfully instantiated")
    }
}
