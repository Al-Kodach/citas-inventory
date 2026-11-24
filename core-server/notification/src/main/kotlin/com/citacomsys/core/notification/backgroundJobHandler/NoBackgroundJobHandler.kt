package com.citacomsys.core.notification.backgroundJobHandler

internal class NoBackgroundJobHandler: BackgroundJobHandler {
    override val id: String = "no_service_handler"

    override suspend fun execute(payload: String) {
        println("No notification handler for $id")
    }
}