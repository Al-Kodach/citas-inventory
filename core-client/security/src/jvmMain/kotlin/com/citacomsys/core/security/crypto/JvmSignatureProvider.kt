package com.citacomsys.core.security.crypto

import eu.anifantakis.lib.ksafe.KSafe
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

internal class JvmSignatureProvider(
    private val vault: KSafe
) : SignatureProvider {

    private val privateKeyKey = "workstation_private_key"
    private val publicKeyKey = "workstation_public_key"

    override suspend fun getWorkstationPublicKey(): String {
        val cachedPub = vault.get<String?>(publicKeyKey, null)
        if (cachedPub != null) return cachedPub

        return generateAndStoreKeyPair().second
    }

    override suspend fun sign(data: ByteArray): String {
        val privKey = getPrivateKey()
        // Using RSA for broad compatibility, move to Ed25519 if using Java 15+
        val sig = Signature.getInstance("SHA256withRSA")
        sig.initSign(privKey)
        sig.update(data)
        return Base64.getEncoder().encodeToString(sig.sign())
    }

    private suspend fun getPrivateKey(): PrivateKey {
        val encoded = vault.get<String?>(privateKeyKey, null) ?: generateAndStoreKeyPair().first
        val keySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(encoded))
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec)
    }

    private suspend fun generateAndStoreKeyPair(): Pair<String, String> {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val kp = kpg.generateKeyPair()

        val priv = Base64.getEncoder().encodeToString(kp.private.encoded)
        val pub = Base64.getEncoder().encodeToString(kp.public.encoded)

        vault.put(privateKeyKey, priv)
        vault.put(publicKeyKey, pub)

        return priv to pub
    }
}
