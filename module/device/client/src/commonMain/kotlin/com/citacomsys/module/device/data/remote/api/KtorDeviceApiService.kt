package com.citacomsys.module.device.data.remote.api

import com.citacomsys.api.auth.WorkstationSession
import com.citacomsys.api.device.service.DeviceApiService
import com.citacomsys.core.remote.BaseApi
import com.citacomsys.types.DeviceSummary
import com.citacomsys.types.UserDeviceInfo
import com.citacomsys.types.WorkstationBinding
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put

internal class KtorDeviceApiService(
    client: HttpClient,
    private val endpoint: String = "api/v1/devices"
) : DeviceApiService, BaseApi(client) {

    override suspend fun getDevices(): List<UserDeviceInfo> {
        return executeRequest { client.get(endpoint) }
    }

    override suspend fun getDeviceSummary(hardwareId: String): DeviceSummary {
        return executeRequest<DeviceSummary> {
            client.get("$endpoint/$hardwareId/summary")
        }
    }

    override suspend fun getDeviceBindings(hardwareId: String): List<WorkstationBinding> {
        return runCatching {
            executeRequest<List<WorkstationBinding>> {
                client.get("$endpoint/$hardwareId/bindings")
            }
        }.getOrDefault(emptyList())
    }

    override suspend fun getDeviceSessions(hardwareId: String): List<WorkstationSession> {
        return runCatching {
            executeRequest<List<WorkstationSession>> {
                client.get("$endpoint/$hardwareId/sessions")
            }
        }.getOrDefault(emptyList())
    }

    override suspend fun renameDevice(
        hardwareId: String,
        newName: String
    ): Result<Unit> = runCatching {
        executeRequest {
            client.put("$endpoint/admin/$hardwareId/rename") {
                url {
                    parameters.append("name", newName)
                }
            }
        }
    }

    override suspend fun revokeBinding(bindingId: String): Result<Unit> =
        runCatching {
            executeRequest {
                client.post("$endpoint/admin/$bindingId/revoke")
            }
        }

    override suspend fun unbindWorkstation(bindingId: String): Result<Unit> =
        runCatching {
            executeRequest {
                client.delete("$endpoint/admin/bindings/$bindingId")
            }
        }

    override suspend fun relinkWorkstation(bindingId: String): Result<Unit> =
        runCatching {
            executeRequest {
                client.post("$endpoint/admin/bindings/$bindingId/relink")
            }
        }

    override suspend fun revokeDeviceGlobally(hardwareId: String): Result<Unit> =
        runCatching {
            executeRequest {
                client.post("$endpoint/admin/$hardwareId/revoke")
            }
        }

    override suspend fun revokeSession(sessionId: String): Result<Unit> =
        runCatching {
            executeRequest {
                client.post("$endpoint/admin/sessions/$sessionId/revoke")
            }
        }
}
