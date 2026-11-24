package com.citacomsys.core.security.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.citacomsys.shared.AppConfig
import java.security.KeyStore
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

internal class AndroidSignatureProvider : SignatureProvider {

    private val alias = "citas_workstation_identity"
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    override suspend fun getWorkstationPublicKey(): String {
        if (!keyStore.containsAlias(alias)) {
            generateKeyPair()
        }
        val entry = keyStore.getEntry(alias, null) as KeyStore.PrivateKeyEntry
        return Base64.getEncoder().encodeToString(entry.certificate.publicKey.encoded)
    }

    override suspend fun sign(data: ByteArray): String {
        if (!keyStore.containsAlias(alias)) {
            generateKeyPair()
        }
        val entry = keyStore.getEntry(alias, null) as KeyStore.PrivateKeyEntry
        val sig = Signature.getInstance("SHA256withRSA")
        sig.initSign(entry.privateKey)
        sig.update(data)
        return Base64.getEncoder().encodeToString(sig.sign())
    }

    private fun generateKeyPair() {
        val kpg = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA, 
            "AndroidKeyStore"
        )
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .setKeySize(2048)
            .build()
            
        kpg.initialize(spec)
        kpg.generateKeyPair()
    }
}
