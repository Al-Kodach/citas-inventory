package com.citacomsys.core.mesh

import com.citacomsys.core.mesh.security.MeshCryptoManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MeshCryptoTest {

    @Test
    fun testEncryptionAndDecryption() {
        val secret = "my-secure-org-secret".encodeToByteArray()
        val originalMessage = "Citas Industrial Mesh Payload 12345".encodeToByteArray()

        val encrypted = MeshCryptoManager.encrypt(originalMessage, secret)
        val decrypted = MeshCryptoManager.decrypt(encrypted, secret)

        assertEquals(originalMessage.decodeToString(), decrypted.decodeToString())
    }

    @Test
    fun testDiscoveryTokenGenerationAndVerification() {
        val orgId = "org-citas-999"
        val secret = "org-secret-key".encodeToByteArray()

        val token = MeshCryptoManager.generateDiscoveryToken(orgId, secret)

        val isValid = MeshCryptoManager.verifyDiscoveryToken(token, orgId, secret)
        assertTrue(isValid, "Valid discovery token should verify successfully")

        val isInvalid = MeshCryptoManager.verifyDiscoveryToken(token, "wrong-org-id", secret)
        assertFalse(isInvalid, "Discovery token with wrong organization ID should fail verification")
    }

    @Test
    fun testFleetSecretDerivation() {
        val fleetDna = "dna-secret-9876"
        val derived = MeshCryptoManager.deriveFleetSecret(fleetDna)
        assertEquals(fleetDna, derived.decodeToString())
    }
}
