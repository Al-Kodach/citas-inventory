package com.citacomsys.shared.sync

enum class SyncPriority(val level: Int) {
    CRITICAL(0),

    HIGHEST(1),

    NORMAL(2),

    LOW(3)
}