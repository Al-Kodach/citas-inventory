package com.citacomsys.core.mesh.util

import java.net.InetAddress
import java.net.NetworkInterface

actual fun getAllLocalIPs(): List<String> {
    val ips = mutableListOf<String>()
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val iface = interfaces.nextElement()
            if (iface.isLoopback || !iface.isUp) continue
            val addresses = iface.inetAddresses
            while (addresses.hasMoreElements()) {
                val addr = addresses.nextElement()
                if (addr is InetAddress && !addr.isLoopbackAddress) {
                    ips.add(addr.hostAddress ?: "")
                }
            }
        }
    } catch (e: Exception) {
        // Log error
    }
    return ips.filter { it.isNotBlank() }
}

actual fun getBestLocalIP(): String? {
    val ips = getAllLocalIPs()
    return ips.find { it.startsWith("192.") || it.startsWith("10.") || it.startsWith("172.") }
        ?: ips.firstOrNull()
}
