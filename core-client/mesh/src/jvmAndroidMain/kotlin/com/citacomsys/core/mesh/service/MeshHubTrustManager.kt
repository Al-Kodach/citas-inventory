package com.citacomsys.core.mesh.service

import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Trusts the discovered Mother Hub certificate by SHA-256 pin.
 * Any other connection (Citas Cloud) uses the platform CA store.
 */
internal class MeshHubTrustManager(
    private val pinnedFingerprint: () -> String?
) : X509TrustManager {

    private val platformTrustManager: X509TrustManager = defaultTrustManager()

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) {
        platformTrustManager.checkClientTrusted(chain, authType)
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
        if (chain.isEmpty()) {
            throw CertificateException("Empty TLS certificate chain")
        }
        val pin = pinnedFingerprint()?.lowercase()?.trim().orEmpty()
        if (pin.isNotEmpty()) {
            val presented = sha256Hex(chain[0].encoded)
            if (presented == pin) {
                return
            }
        }
        try {
            platformTrustManager.checkServerTrusted(chain, authType)
        } catch (cause: Exception) {
            if (pin.isNotEmpty()) {
                throw CertificateException("Mesh hub TLS fingerprint mismatch")
            }
            throw cause
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = platformTrustManager.acceptedIssuers

    private fun defaultTrustManager(): X509TrustManager {
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        factory.init(null as java.security.KeyStore?)
        return factory.trustManagers.filterIsInstance<X509TrustManager>().first()
    }
}
