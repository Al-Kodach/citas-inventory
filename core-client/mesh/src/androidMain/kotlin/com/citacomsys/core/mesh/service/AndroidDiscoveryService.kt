package com.citacomsys.core.mesh.service

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.citacomsys.core.mesh.model.MeshPeerInfo
import com.citacomsys.core.mesh.security.MeshProtocol
import io.github.aakira.napier.Napier
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock

internal class AndroidDiscoveryService(
    private val context: Context
) : DiscoveryService {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val serviceType = "_citas-mesh._tcp"
    private val peers = ConcurrentHashMap<String, MeshPeerInfo>()

    override fun startDiscovery(organizationId: String): Flow<List<MeshPeerInfo>> = callbackFlow {
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Napier.d { "NSD Discovery Started: $regType" }
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType == serviceType) {
                    nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            Napier.e { "NSD Resolve Failed: $errorCode" }
                        }

                        override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                            val orgId = resolvedInfo.attributes["orgId"]?.decodeToString()
                            if (orgId == organizationId) {
                                val peer = MeshPeerInfo(
                                    hardwareId = resolvedInfo.attributes["hwId"]?.decodeToString() ?: "",
                                    bindingId = resolvedInfo.attributes["bindId"]?.decodeToString() ?: "",
                                    organizationId = orgId ?: "",
                                    workspaceId = resolvedInfo.attributes["wsId"]?.decodeToString(),
                                    appType = null,
                                    workstationName = resolvedInfo.serviceName,
                                    ipAddress = resolvedInfo.host.hostAddress ?: "",
                                    port = resolvedInfo.port,
                                    isMotherHub = resolvedInfo.attributes["isHub"]?.decodeToString() == "true",
                                    orgVerificationToken = resolvedInfo.attributes["orgToken"]?.decodeToString(),
                                    tlsFingerprint = resolvedInfo.attributes[MeshProtocol.MDNS_FINGERPRINT_ATTR]
                                        ?.decodeToString()
                                        ?.takeIf { it.isNotBlank() },
                                    lastSeenAt = Clock.System.now()
                                )
                                peers[resolvedInfo.serviceName] = peer
                                trySend(peers.values.toList())
                            }
                        }
                    })
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                peers.remove(serviceInfo.serviceName)
                trySend(peers.values.toList())
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Napier.d { "NSD Discovery Stopped" }
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Napier.e { "NSD Start Discovery Failed: $errorCode" }
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Napier.e { "NSD Stop Discovery Failed: $errorCode" }
                nsdManager.stopServiceDiscovery(this)
            }
        }

        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)

        awaitClose {
            nsdManager.stopServiceDiscovery(discoveryListener)
        }
    }

    override fun stopDiscovery() {
        // Managed by flow awaitClose
        peers.clear()
    }

    override fun broadcastPresence(info: MeshPeerInfo) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = info.workstationName
            serviceType = this@AndroidDiscoveryService.serviceType
            port = info.port
            setAttribute("orgId", info.organizationId)
            setAttribute("hwId", info.hardwareId)
            setAttribute("bindId", info.bindingId)
            setAttribute("wsId", info.workspaceId ?: "")
            setAttribute("isHub", info.isMotherHub.toString())
            setAttribute("orgToken", info.orgVerificationToken ?: "")
            setAttribute(MeshProtocol.MDNS_PROTO_ATTR, MeshProtocol.MDNS_PROTO_TLS)
            info.tlsFingerprint?.let { setAttribute(MeshProtocol.MDNS_FINGERPRINT_ATTR, it) }
        }

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                Napier.d { "NSD Service Registered: ${NsdServiceInfo.serviceName}" }
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Napier.e { "NSD Registration Failed: $errorCode" }
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                Napier.d { "NSD Service Unregistered: ${arg0.serviceName}" }
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Napier.e { "NSD Unregistration Failed: $errorCode" }
            }
        })
    }
}
