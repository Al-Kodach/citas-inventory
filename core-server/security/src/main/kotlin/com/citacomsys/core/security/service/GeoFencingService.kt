package com.citacomsys.core.security.service

import com.maxmind.geoip2.DatabaseReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.util.zip.GZIPInputStream
import mu.KotlinLogging
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream

/**
 * Platform-wide security service for verifying client geolocation and enforcing
 * regional traffic constraints (e.g., Ghana only).
 */
class GeoFencingService(
    databasePath: String = System.getenv("GEOIP_DATABASE_PATH") ?: "storage/geo-ip/GeoLite2-Country.mmdb"
) {
    private val logger = KotlinLogging.logger("GEO_FENCING_SERVICE")

    // Load credentials cleanly from environment variables
    private val accountId = System.getenv("MAXMIND_ACCOUNT_ID") ?: "1367265"
    private val licenseKey =
        System.getenv("MAXMIND_LICENSE_KEY") ?: "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"

    private val dbReader: DatabaseReader? = try {
        val database = File(databasePath)
        if (!database.exists()) {
            logger.info { "Local GeoLite2 database missing. Pulling file from MaxMind..." }
            downloadAndExtractDatabase(database)
        }
        DatabaseReader.Builder(database).build()
    } catch (e: Exception) {
        logger.error(e) { "Failed to initialize MaxMind Geolocation database. Geo-fencing is disabled!" }
        null
    }

    private fun downloadAndExtractDatabase(targetFile: File) {
        targetFile.parentFile?.mkdirs()

        val secureDownloadUrl = "https://download.maxmind.com/app/geoip_download" +
                "?edition_id=GeoLite2-Country" +
                "&license_key=$licenseKey" +
                "&suffix=tar.gz"

        logger.info { "Connecting to MaxMind download servers..." }
        val connection = URL(secureDownloadUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 30000

        connection.setRequestProperty("Accept", "application/x-gzip, application/gzip")

        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            val errorResponse =
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error body"
            throw RuntimeException("MaxMind rejected connection. HTTP $responseCode: $errorResponse")
        }

        val contentType = connection.contentType ?: ""
        if (contentType.contains("text/html")) {
            val pageText = connection.inputStream.bufferedReader().use { it.readText() }
            throw RuntimeException("Received unexpected web content instead of archive file: $pageText")
        }

        logger.info { "Archive stream connected. Beginning extraction pipeline..." }
        connection.inputStream.use { inputStream ->
            GZIPInputStream(inputStream).use { gzipStream ->
                val tarStream = TarArchiveInputStream(gzipStream)
                var entry = tarStream.nextTarEntry
                var extracted = false

                while (entry != null) {
                    if (entry.name.endsWith(".mmdb")) {
                        logger.info { "Found database payload: ${entry.name}. Unpacking..." }
                        FileOutputStream(targetFile).use { outputStream ->
                            tarStream.copyTo(outputStream)
                        }
                        extracted = true
                        break
                    }
                    entry = tarStream.nextTarEntry
                }

                if (!extracted) {
                    throw FileNotFoundException("Archive download completed, but no .mmdb file layout was located.")
                }
            }
        }
        logger.info { "GeoLite2 Country database downloaded and initialized locally successfully!" }
    }

    /**
     * Returns true if the IP is verified to be in Ghana, or if it's a local/internal/LAN testing IP.
     */
    fun isTrafficAllowed(ipAddressString: String): Boolean {
        if (dbReader == null) return true // Fallback safe if DB file is missing during deployment

        val sanitizedIp = ipAddressString.trim().lowercase()

        // Fast-path checks for local hostnames, loopbacks & private LAN IP prefixes
        if (sanitizedIp.isBlank() ||
            sanitizedIp == "127.0.0.1" ||
            sanitizedIp == "0:0:0:0:0:0:0:1" ||
            sanitizedIp == "::1" ||
            sanitizedIp == "localhost" ||
            sanitizedIp.contains("local") ||
            sanitizedIp.contains("gateway") ||
            sanitizedIp.startsWith("192.168.") ||
            sanitizedIp.startsWith("10.") ||
            sanitizedIp.startsWith("172.") ||
            sanitizedIp.startsWith("100.") ||
            sanitizedIp.startsWith("169.254.")
        ) {
            return true
        }

        return try {
            val ipAddress = InetAddress.getByName(ipAddressString)

            // Java native checks for Loopback, LAN Site-Local & Link-Local IPs
            if (ipAddress.isLoopbackAddress ||
                ipAddress.isSiteLocalAddress ||
                ipAddress.isAnyLocalAddress ||
                ipAddress.isLinkLocalAddress
            ) {
                return true
            }

            val response = dbReader.country(ipAddress)
            val countryIso = response.country().isoCode()

            val isGhana = "GH".equals(countryIso, ignoreCase = true)
            if (!isGhana) {
                logger.warn { "Blocked malicious/unauthorized cross-border traffic from IP: $ipAddressString ($countryIso)" }
            }
            isGhana
        } catch (e: Exception) {
            logger.warn { "IP location lookup skipped for local/unresolvable address '$ipAddressString': ${e.message}" }
            true
        }
    }
}
