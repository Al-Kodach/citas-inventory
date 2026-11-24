package com.citacomsys.core.spi

interface PlatformWorkRegistry : StartupTask {
    override val id: String get() = "platform_work_registry"
    override val runLevel: StartupRunLevel get() = StartupRunLevel.BOOTSTRAP
    override val order: Int get() = 10

    fun registerWorker(worker: PlatformWorker)
    fun getWorker(workerId: String): PlatformWorker?
    fun getAllWorkers(): List<PlatformWorker>
}

/**
 * Engine contract extending [PlatformWorkRegistry] and [StartupTask] to control background worker collection,
 * multi-workspace scheduling, and execution control across platforms.
 */
interface PlatformWorkManagerEngine : PlatformWorkRegistry {
    override val id: String get() = "platform_work_manager_engine"
    override val runLevel: StartupRunLevel get() = StartupRunLevel.POST_ONBOARDING
    override val order: Int get() = 50

    /** Triggers immediate execution of a specific worker, optionally scoped to a workspace. */
    fun triggerWorkerNow(workerId: String, workspaceId: String? = null)

    /** Triggers immediate execution of all workers bound to a workspace or globally. */
    fun triggerAllWorkersNow(workspaceId: String? = null)

    /** Activates and schedules worker loops for a newly configured/activated workspace. */
    fun activateWorkspaceWorkers(workspaceId: String)

    /** Cancels background worker execution for a deactivated workspace. */
    fun cancelWorkspaceWorkers(workspaceId: String)

    /** Cancels a specific worker by ID. */
    fun cancelWorker(workerId: String)
}
