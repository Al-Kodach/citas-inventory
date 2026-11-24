package com.citacomsys.admin.api.device

import com.citacomsys.feature.api.telemetry.TraceTimelineEntry
import com.citacomsys.types.DeviceSummary
import com.citacomsys.types.HardwareTechInfo
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult

interface AdminDeviceApiService {
    suspend fun fetch(query: PageQuery): PageResult<DeviceSummary>
    suspend fun fetchEvents(deviceId: String): List<TraceTimelineEntry>
    suspend fun getDevice(deviceId: String): HardwareTechInfo
    suspend fun revokeDevice(deviceId: String): Result<Unit>
    suspend fun renameDevice(deviceId: String, newName: String): Result<Unit>
}
