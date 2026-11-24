package com.citacomsys.core.navigation

import android.net.Uri
import androidx.core.net.toUri
import com.citacomsys.core.spi.PlatformUri

//@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
//actual class PlatformUri(private val nativeUri: Uri) {
//    actual val host: String?
//        get() = nativeUri.host
//
//    actual val path: String?
//        get() = nativeUri.path
//
//    actual fun getQueryParameter(key: String): String? {
//        return nativeUri.getQueryParameter(key)
//    }
//
//
//    actual companion object {
//        actual fun parse(url: String): PlatformUri? {
//            return try {
//                val parsed = url.toUri()
//                if (parsed.scheme != null) PlatformUri(parsed) else null
//            } catch (e: Exception) {
//                null
//            }
//        }
//    }
//}
