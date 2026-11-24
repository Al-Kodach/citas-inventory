package com.citacomsys.device.service

import com.citacomsys.types.HardwareTechInfo
import com.citacomsys.types.UserDeviceInfo
import kotlinx.coroutines.flow.StateFlow

interface DeviceService {

    // User device info
    val deviceInfo: StateFlow<UserDeviceInfo>

    suspend fun getDeviceId(): String
    suspend fun getHardwareInfo(): HardwareTechInfo
    
    suspend fun updateDeviceInfo(info: UserDeviceInfo)

    suspend fun generateContentPdf(content: List<PdfLineInfo>): ByteArray

    suspend fun downloadFile(data: ByteArray, fileName: String)

    suspend fun print(data: ByteArray, fileName: String, silent: Boolean = false)

    /**
     * Renders PDF pages to images (e.g. PNG) for previewing.
     * Returns a list of byte arrays, each representing a page image.
     */
    suspend fun renderPdfToImages(data: ByteArray): List<ByteArray>
}

data class PdfLineInfo(
    val value: String,
    val type: ValueType,
    val width: Int = 0,
    val height: Int = 0
)

enum class ValueType {
    FONT_BOLD,
    FONT_REGULAR,
    TEXT,
    NEW_LINE,
    SPACE
}
