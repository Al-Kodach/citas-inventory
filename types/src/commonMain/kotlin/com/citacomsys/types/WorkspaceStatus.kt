package com.citacomsys.types

enum class WorkspaceStatus {
    ACTIVE,
    SUSPENDED,
    READ_ONLY,
    TERMINATED,
    DELETED,
    EXPIRED,
    CANCELED;

    companion object {
        fun canAccess(status: WorkspaceStatus): Boolean {
            return status != TERMINATED && status != DELETED && status != CANCELED
        }
        fun canSync(status: WorkspaceStatus): Boolean {
            return status == ACTIVE
        }

        fun canWrite(status: WorkspaceStatus): Boolean {
            return status == ACTIVE
        }
    }
}