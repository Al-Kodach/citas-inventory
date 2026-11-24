package com.citacomsys.core.navigation

import com.citacomsys.core.spi.PlatformUri
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

//@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
//actual class PlatformUri(private val nativeUri: URI) {
//    actual val host: String?
//        get() = nativeUri.host
//
//    actual val path: String?
//        get() = nativeUri.path
//
//    actual fun getQueryParameter(key: String): String? {
//        val query = nativeUri.rawQuery ?: return null
//        return query.split("&")
//            .map { it.split("=", limit = 2) }
//            .firstOrNull { it.size == 2 && URLDecoder.decode(it[0], StandardCharsets.UTF_8.id()) == key }
//            ?.let { URLDecoder.decode(it[1], StandardCharsets.UTF_8.id()) }
//    }
//
//    actual companion object {
//        actual fun parse(url: String): PlatformUri? {
//            return try {
//                val parsed = URI(url)
//                if (parsed.scheme != null) PlatformUri(parsed) else null
//            } catch (e: Exception) {
//                null
//            }
//        }
//    }
//}