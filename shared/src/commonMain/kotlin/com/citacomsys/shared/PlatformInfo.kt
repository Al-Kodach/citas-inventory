package com.citacomsys.shared

interface PlatformInfo {
    val deviceId: String
    val serialNumber: String
    val model: String
    val packageName: String
    val appVersion: String
    val appVersionCode: Int
    val hardwareFingerprint: String
    val fleetSecret: ByteArray
    val tlsKeyStorePassword: String
    val tlsKeyAlias: String
    val tlsKeyStoreFilename: String
    val baseStoragePath: String
    fun closeApp()
}
