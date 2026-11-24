package com.citacomsys.core.barcode

import kotlinx.serialization.Serializable

@Serializable
data class ScannedBarcode(
    val rawValue: String,
    val format: String,
    val timestamp: Long
)

/**
 * A camera [ScannerView] can be pointed at, from `availableCameras()`.
 *
 * @property id Pass to [ScannerView]'s `cameraId`.
 * @property label What to show in a picker. Blank where the platform gives no ID.
 */
data class CameraDevice(
    val id: String,
    val label: String,
)

