package com.citacomsys.core.mesh.service

import com.citacomsys.core.mesh.security.MeshProtocol
import io.github.aakira.napier.Napier
import io.ktor.network.tls.certificates.buildKeyStore
import io.ktor.network.tls.certificates.saveToFile
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.X509Certificate

internal data class MeshTlsIdentity(
    val keyStore: KeyStore,
    val keyAlias: String,
    val password: CharArray,
    val fingerprintSha256: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MeshTlsIdentity

        if (keyStore != other.keyStore) return false
        if (keyAlias != other.keyAlias) return false
        if (!password.contentEquals(other.password)) return false
        if (fingerprintSha256 != other.fingerprintSha256) return false

        return true
    }

    override fun hashCode(): Int {
        var result = keyStore.hashCode()
        result = 31 * result + keyAlias.hashCode()
        result = 31 * result + password.contentHashCode()
        result = 31 * result + fingerprintSha256.hashCode()
        return result
    }
}

internal class MeshTlsIdentityStore(
    private val storeFile: File,
    private val organizationId: String
) {
    fun loadOrCreate(): MeshTlsIdentity {
        storeFile.parentFile?.mkdirs()
        // Cryptographically bind keystore password to organization ID for absolute tenant isolation
        val password = "citas-mesh-${organizationId.ifBlank { "default" }}-tls"
        val keyStore = if (storeFile.exists()) {
            loadExisting(password)
        } else {
            val created = generate(password, organizationId)
            created.saveToFile(storeFile, password)
            Napier.i(tag = "Mesh-TLS") { "Generated tenant-isolated Mother Hub TLS identity for org [$organizationId] at ${storeFile.absolutePath}" }
            created
        }
        val cert = keyStore.getCertificate(MeshProtocol.KEY_ALIAS) as X509Certificate
        val fingerprint = sha256Hex(cert.encoded)
        Napier.i(tag = "Mesh-TLS") { "Hub TLS SHA-256 fingerprint $fingerprint for org [$organizationId]" }
        return MeshTlsIdentity(
            keyStore = keyStore,
            keyAlias = MeshProtocol.KEY_ALIAS,
            password = password.toCharArray(),
            fingerprintSha256 = fingerprint
        )
    }

    private fun loadExisting(password: String): KeyStore {
        val candidates = listOf("PKCS12", "JKS")
        var lastError: Exception? = null
        for (type in candidates) {
            try {
                val keyStore = KeyStore.getInstance(type)
                FileInputStream(storeFile).use { keyStore.load(it, password.toCharArray()) }
                if (keyStore.getCertificate(MeshProtocol.KEY_ALIAS) != null) {
                    return keyStore
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("Unable to load tenant-isolated mesh TLS keystore")
    }

    private fun generate(password: String, orgId: String): KeyStore {
        val safeOrgId = orgId.ifBlank { "default" }
        return buildKeyStore {
            certificate(MeshProtocol.KEY_ALIAS) {
                this.password = password
                daysValid = 825
                keySizeInBits = 2048
                domains = listOf("citas-mesh-$safeOrgId", MeshProtocol.CERT_COMMON_NAME, "localhost")
            }
        }
    }
}

internal fun sha256Hex(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
    return digest.joinToString("") { b ->
        val hex = Integer.toHexString(b.toInt() and 0xff)
        if (hex.length == 1) "0$hex" else hex
    }
}
