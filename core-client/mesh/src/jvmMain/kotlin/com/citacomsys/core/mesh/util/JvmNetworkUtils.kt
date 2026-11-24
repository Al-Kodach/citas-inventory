package com.citacomsys.core.mesh.util

import java.net.Inet4Address
import java.net.NetworkInterface

actual fun getAllLocalIPs(): List<String> {
    return NetworkInterface.getNetworkInterfaces().toList()
        .filter { 
            it.isUp && !it.isLoopback && !it.isVirtual && 
            !it.name.startsWith("docker") && 
            !it.name.startsWith("vbox") && 
            !it.name.contains("bridge") &&
            !it.name.startsWith("veth")
        }
        .flatMap { it.inetAddresses.toList() }
        .filter { !it.isLoopbackAddress && it is Inet4Address }
        .map { it.hostAddress ?: "" }
        .filter { it.isNotEmpty() }
}

actual fun getBestLocalIP(): String? {
    val ips = getAllLocalIPs()
    return ips.firstOrNull { it.startsWith("192.168.") }
        ?: ips.firstOrNull { it.startsWith("10.") }
        ?: ips.firstOrNull { it.startsWith("172.") }
        ?: ips.firstOrNull()
}
