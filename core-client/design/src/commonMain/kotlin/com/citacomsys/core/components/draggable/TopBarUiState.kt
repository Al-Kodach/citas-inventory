package com.citacomsys.core.components.draggable

data class TopBarUiState(
    val appName: String,
    val welcomeMessage: String?,
    val sseStatus: SseUiStatus,
    val openIssuesCount: Long,
    val isDark: Boolean
)

enum class SseUiStatus {
    CONNECTED,
    CONNECTING,
    DISCONNECTED
}
