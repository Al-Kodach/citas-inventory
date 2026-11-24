package com.citacomsys.core.data.file

import com.citacomsys.shared.AppConfig
import okio.FileSystem
import java.io.File


@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual class PlatformFileSupport(appConfig: AppConfig) {
    private val citacomsysRootDir = "citacomsys/${appConfig.prefsPrefix}"

    actual fun getInternalDir(): String {
        val os = System.getProperty("os.name").lowercase()
        val userHome = System.getProperty("user.home")

        val path = when {
            os.contains("win") -> File(System.getenv("APPDATA"), citacomsysRootDir)
            os.contains("mac") -> File(userHome, "Library/Application Support/$citacomsysRootDir")
            else -> File(userHome, ".local/share/$citacomsysRootDir")
        }

        if (!path.exists()) path.mkdirs()
        return path.absolutePath
    }

    actual fun getUserHomeDir(): String {
        return System.getProperty("user.home")
    }

    actual fun getTempDir(): String {
        return System.getProperty("java.io.tmpdir")
    }

    actual fun getDocumentsDir(): String {
        val path = File(getUserHomeDir(), "Documents/$citacomsysRootDir")
        if (!path.exists()) path.mkdirs()
        return path.absolutePath
    }

    actual fun getDatabaseFile(name: String): String {
        return File(getInternalDir(), name).absolutePath
    }

    actual val hostFileSystem: FileSystem = FileSystem.SYSTEM
}
