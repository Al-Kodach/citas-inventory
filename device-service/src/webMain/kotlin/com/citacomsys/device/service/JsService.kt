package com.citacomsys.device.service

import com.citacomsys.core.domain.repository.PreferencesRepository
import com.citacomsys.types.HardwareTechInfo
import com.citacomsys.types.UserDeviceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi

internal class JsService(
    private val prefs: PreferencesRepository,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
): DeviceService {
    override val deviceInfo: StateFlow<UserDeviceInfo>
        get() = TODO("Not yet implemented")


    @OptIn(ExperimentalUuidApi::class)
    override suspend fun getDeviceId(): String {
        return ""
    }

    override suspend fun getHardwareInfo(): HardwareTechInfo {
        TODO("Not yet implemented")
    }

    override suspend fun updateDeviceInfo(info: UserDeviceInfo) {
        TODO("Not yet implemented")
    }

    override suspend fun generateContentPdf(content: List<PdfLineInfo>): ByteArray {
        TODO("Not yet implemented")
    }

    override suspend fun downloadFile(data: ByteArray, fileName: String) {
        TODO("Not yet implemented")
    }

    override suspend fun print(data: ByteArray, fileName: String) {
        TODO("Not yet implemented")
    }

    override suspend fun renderPdfToImages(data: ByteArray): List<ByteArray> {
        TODO("Not yet implemented")
    }
}