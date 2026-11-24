package com.citacomsys.core.data.file


import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.citacomsys.shared.AppConfig
import okio.FileSystem
import java.io.File


@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual class PlatformFileSupport(
    private val context: Context,
    appConfig: AppConfig
) {

    // Move appName out of global scopes to secure namespace insulation
    private val appSubFolder = appConfig.prefsPrefix

    actual fun getInternalDir(): String {
        val path = File(context.filesDir, appSubFolder)
        if (!path.exists()) path.mkdirs()
        return path.absolutePath
    }

    actual fun getTempDir(): String {
        return context.cacheDir.absolutePath
    }

    @RequiresApi(Build.VERSION_CODES.FROYO)
    actual fun getUserHomeDir(): String {
        return context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath
    }

    actual fun getDocumentsDir(): String {
        // Android scopes internal public items properly within specific target subfolders
        val path = File(context.filesDir, "$appSubFolder/Documents")
        if (!path.exists()) path.mkdirs()
        return path.absolutePath
    }

    actual fun getDatabaseFile(name: String): String {
        return context.getDatabasePath(name).absolutePath
    }

    actual val hostFileSystem: FileSystem = FileSystem.SYSTEM
}
