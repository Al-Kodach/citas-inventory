package com.citacomsys.core.spi

@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual class PlatformUri {
    actual val host: String?
        get() = TODO("Not yet implemented")
    actual val path: String?
        get() = TODO("Not yet implemented")

    actual fun getQueryParameter(key: String): String? {
        TODO("Not yet implemented")
    }

    actual companion object {
        actual fun parse(url: String): PlatformUri? {
            TODO("Not yet implemented")
        }
    }
}