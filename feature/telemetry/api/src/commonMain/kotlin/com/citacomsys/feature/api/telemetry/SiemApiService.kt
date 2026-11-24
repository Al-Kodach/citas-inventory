package com.citacomsys.feature.api.telemetry

interface SiemApiService {
    suspend fun getDeviceTimeline(deviceId: String): List<TraceTimelineEntry>
    suspend fun getGlobalSecurityFeed(limit: Int = 50): List<TraceTimelineEntry>
    suspend fun getRequestTrace(requestId: String): List<TraceTimelineEntry>
}
