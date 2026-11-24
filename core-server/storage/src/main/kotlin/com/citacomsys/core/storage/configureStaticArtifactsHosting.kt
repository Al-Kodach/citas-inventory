package com.citacomsys.core.storage


import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import java.io.File

fun Route.configureStaticArtifactsHosting(baseStorageDirectoryPath: String) {
    // Maps to physical testing directory onto the http://localhost:8080/static/builds/* route
    staticFiles(
        remotePath = "/static/builds",
        dir = File(baseStorageDirectoryPath)
    )
}
