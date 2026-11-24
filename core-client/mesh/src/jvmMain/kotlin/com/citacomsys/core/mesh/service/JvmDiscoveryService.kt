package com.citacomsys.core.mesh.service

import com.citacomsys.core.mesh.model.MeshPeerInfo
import com.citacomsys.core.mesh.security.MeshProtocol
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceInfo
import javax.jmdns.ServiceListener
import kotlin.time.Clock

internal class JvmDiscoveryService : DiscoveryService {

    private var jmdns: JmDNS? = null
    private val serviceType = "_citas-mesh._tcp.local."
    private val peers = ConcurrentHashMap<String, MeshPeerInfo>()

    override fun startDiscovery(organizationId: String): Flow<List<MeshPeerInfo>> = callbackFlow {
        val localhost = InetAddress.getLocalHost()
        val jmdnsInstance = JmDNS.create(localhost, localhost.hostName)
        jmdns = jmdnsInstance

        val listener = object : ServiceListener {
            override fun serviceAdded(event: ServiceEvent) {
                jmdnsInstance.requestServiceInfo(event.type, event.name)
            }

            override fun serviceRemoved(event: ServiceEvent) {
                peers.remove(event.name)
                trySend(peers.values.toList())
            }

            override fun serviceResolved(event: ServiceEvent) {
                val info = event.info
                val orgId = info.getPropertyString("orgId")
                if (orgId == organizationId) {
                    val peer = MeshPeerInfo(
                        hardwareId = info.getPropertyString("hwId") ?: "",
                        bindingId = info.getPropertyString("bindId") ?: "",
                        organizationId = orgId,
                        workspaceId = info.getPropertyString("wsId"),
                        appType = null,
                        workstationName = info.name,
                        ipAddress = info.inetAddresses.firstOrNull()?.hostAddress ?: "",
                        port = info.port,
                        isMotherHub = info.getPropertyString("isHub") == "true",
                        orgVerificationToken = info.getPropertyString("orgToken"),
                        tlsFingerprint = info.getPropertyString(MeshProtocol.MDNS_FINGERPRINT_ATTR)
                            ?.takeIf { it.isNotBlank() },
                        lastSeenAt = Clock.System.now()
                    )
                    peers[event.name] = peer
                    trySend(peers.values.toList())
                }
            }
        }

        jmdnsInstance.addServiceListener(serviceType, listener)

        awaitClose {
            jmdnsInstance.removeServiceListener(serviceType, listener)
            jmdnsInstance.close()
            jmdns = null
        }
    }

    override fun stopDiscovery() {
        jmdns?.close()
        jmdns = null
        peers.clear()
    }

    override fun broadcastPresence(info: MeshPeerInfo) {
        val props = mapOf(
            "orgId" to info.organizationId,
            "hwId" to info.hardwareId,
            "bindId" to info.bindingId,
            "wsId" to (info.workspaceId ?: ""),
            "isHub" to info.isMotherHub.toString(),
            "orgToken" to (info.orgVerificationToken ?: ""),
            MeshProtocol.MDNS_PROTO_ATTR to MeshProtocol.MDNS_PROTO_TLS,
            MeshProtocol.MDNS_FINGERPRINT_ATTR to (info.tlsFingerprint ?: "")
        )
        
        val serviceInfo = ServiceInfo.create(
            serviceType,
            info.workstationName,
            info.port,
            0, 0,
            props
        )
        jmdns?.registerService(serviceInfo)
    }
}
