package com.citacomsys.core.data.file

import okio.FileSystem

@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual class PlatformFileSupport {
    actual fun getTempDir(): String {
        TODO("Not yet implemented")
    }

    actual fun getInternalDir(): String {
        TODO("Not yet implemented")
    }

    actual fun getUserHomeDir(): String {
        TODO("Not yet implemented")
    }

    actual fun getDocumentsDir(): String {
        TODO("Not yet implemented")
    }

    actual fun getDatabaseFile(name: String): String {
        return name
    }

    actual val hostFileSystem: FileSystem
        get() = TODO("Not yet implemented")
}