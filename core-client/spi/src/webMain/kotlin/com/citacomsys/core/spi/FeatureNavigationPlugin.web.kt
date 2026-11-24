package com.citacomsys.core.spi

@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual class PlatformUri(private val url: String) {
    actual val host: String?
        get() {
            val start = url.indexOf("://") + 3
            if (start < 3) return null
            val end = url.indexOf("/", start)
            return if (end == -1) url.substring(start) else url.substring(start, end)
        }
    actual val path: String?
        get() {
            val start = url.indexOf("://") + 3
            if (start < 3) return null
            val end = url.indexOf("/", start)
            if (end == -1) return null
            val queryStart = url.indexOf("?", end)
            return if (queryStart == -1) url.substring(end) else url.substring(end, queryStart)
        }

    actual fun getQueryParameter(key: String): String? {
        val queryStart = url.indexOf("?")
        if (queryStart == -1) return null
        val query = url.substring(queryStart + 1)
        return query.split("&")
            .map { it.split("=") }
            .firstOrNull { it.first() == key }
            ?.getOrNull(1)
    }

    actual companion object {
        actual fun parse(url: String): PlatformUri? {
            return if (url.contains("://")) PlatformUri(url) else null
        }
    }
}
