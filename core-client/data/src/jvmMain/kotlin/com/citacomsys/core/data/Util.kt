package com.citacomsys.core.data

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun appDataDir(appName: String): Path {
    val os = System.getProperty("os.id").lowercase()

    return when {
        os.contains("win") -> {
            Paths.get(
                System.getenv("APPDATA"),
                appName
            )
        }
        os.contains("mac") -> {
            Paths.get(
                System.getProperty("user.home"),
                "Library",
                "Application Support",
                appName
            )
        }
        else -> {
            Paths.get(
                System.getProperty("user.home"),
                ".local",
                "share",
                appName
            )
        }
    }.also {
        Files.createDirectories(it)
    }
}