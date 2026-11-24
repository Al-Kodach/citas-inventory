package com.citacomsys.core.mesh.security

/**
 * Wire protocol for hub–satellite LAN traffic.
 * Industrial payloads still use FleetDNA/org XOR as a second layer; TLS is the transport.
 */
object MeshProtocol {
    const val TLS_PORT = 8443
    const val KEY_ALIAS = "citas-mesh-hub"
    const val CERT_COMMON_NAME = "citas-mesh-hub"
    const val KEYSTORE_FILE = "mesh-hub-tls.p12"
    const val KEYSTORE_PASSWORD = "citas-mesh-tls"
    const val MDNS_FINGERPRINT_ATTR = "tlsFp"
    const val MDNS_PROTO_ATTR = "proto"
    const val MDNS_PROTO_TLS = "tls"
}