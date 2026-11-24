package com.citacomsys.core.mesh.service

import com.citacomsys.core.mesh.security.MeshCryptoManager
import com.citacomsys.core.mesh.security.toChunks
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.net.Inet4Address
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * JVM Implementation of mDNS Hub Advertising.
 */
class JvmLocalHubAdvertiser(private val scope: CoroutineScope) : LocalHubAdvertiser {
    private var jmdns: JmDNS? = null
    private var activeJob: Job? = null

    private val json = Json {
        encodeDefaults = true
    }

    override fun start(identity: HubIdentity, secret: ByteArray) {
        activeJob?.cancel()
        activeJob = scope.launch {
            stopInternal()

            try {
                // Serialize HubIdentity
                val identityJson = json.encodeToString(identity)

                // Encrypt using Organization Secret
                val encryptedBlob =
                    MeshCryptoManager.encrypt(identityJson.encodeToByteArray(), secret)

                // Chunked Delivery (240 bytes per TXT record entry)
                val chunks = encryptedBlob.toChunks(240)
                val txtMap = mutableMapOf<String, String>()
                chunks.forEachIndexed { index, data ->
                    txtMap["d${index + 1}"] = data
                }
                txtMap["id"] = "citas_v1"

                // Bind to local LAN IP
                val bindingAddress = if (identity.ipAddress.isNotBlank()) {
                    InetAddress.getByName(identity.ipAddress)
                } else {
                    getLanInetAddress()
                }

                jmdns = JmDNS.create(bindingAddress, identity.hubName).apply {
                    val serviceInfo = ServiceInfo.create(
                        "_citas-mesh._tcp.local.",
                        identity.hubName.replace(" ", ""),
                        identity.port,
                        0,
                        0,
                        txtMap
                    )
                    registerService(serviceInfo)
                }
                Napier.i(tag = "Mesh-Hub") { "Mother Hub advertised via JmDNS on [${bindingAddress.hostAddress}:${identity.port}]" }
            } catch (e: Exception) {
                Napier.e(tag = "Mesh-Hub", throwable = e) { "Failed to start JmDNS advertiser" }
            }
        }
    }

    override fun stop() {
        activeJob?.cancel()
        scope.launch { stopInternal() }
    }

    private fun stopInternal() {
        jmdns?.let {
            it.unregisterAllServices()
            it.close()
        }
        jmdns = null
        Napier.i(tag = "Mesh-Hub") { "JmDNS advertiser stopped" }
    }

    private fun getLanInetAddress(): InetAddress {
        runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr
                    }
                }
            }
        }
        return InetAddress.getLocalHost()
    }
}
