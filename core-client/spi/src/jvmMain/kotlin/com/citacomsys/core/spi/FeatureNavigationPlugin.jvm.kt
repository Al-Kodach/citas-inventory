package com.citacomsys.core.spi

import java.net.URI
import java.net.URLDecoder

@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual class PlatformUri(private val uri: URI) {
    actual val host: String? get() = uri.host
    actual val path: String? get() = uri.path

    actual fun getQueryParameter(key: String): String? {
        val query = uri.query ?: return null
        return query.split("&")
            .map { it.split("=") }
            .firstOrNull { it.first() == key }
            ?.getOrNull(1)
            ?.let { URLDecoder.decode(it, "UTF-8") }
    }

    actual companion object {
        actual fun parse(url: String): PlatformUri? {
            return try {
                PlatformUri(URI.create(url))
            } catch (e: Exception) {
                null
            }
        }
    }
}
