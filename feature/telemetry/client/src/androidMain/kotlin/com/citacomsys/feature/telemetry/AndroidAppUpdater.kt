package com.citacomsys.feature.telemetry

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.citacomsys.feature.api.telemetry.CommonUpdateState
import com.citacomsys.feature.api.telemetry.TelemetryLogClient
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.contentLength
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.io.readByteArray
import java.io.File

class AndroidAppUpdater(
    private val context: Context,
    httpClient: HttpClient,
    telemetryClient: TelemetryLogClient,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : BaseAppUpdater(httpClient, telemetryClient) {

    override fun startDownloading() {
        val release = discoveredRelease ?: return
        val targetAsset = release.binaries.firstOrNull { it.fileName.endsWith(".apk") } ?: run {
            _syncState.value = CommonUpdateState.Failed("No Android .apk package specified on server.")
            coroutineScope.launch {
                trackUpdateEvent("DOWNLOAD_FAILED", "No .apk binary found in release v${release.version}")
            }
            return
        }

        _syncState.value = CommonUpdateState.Downloading(0.0f)

        coroutineScope.launch {
            trackUpdateEvent("DOWNLOAD_START", "Starting Android APK download: v${release.version}")
            runCatching {
                val apkFile = File(context.cacheDir, targetAsset.fileName)

                client.prepareGet(targetAsset.downloadUrl).execute { response ->
                    val channel: ByteReadChannel = response.bodyAsChannel()
                    val totalBytes = response.contentLength() ?: targetAsset.sizeInBytes
                    var bytesDownloaded = 0L

                    apkFile.outputStream().use { output ->
                        while (!channel.isClosedForRead) {
                            val packet = channel.readRemaining(max = 4096L)
                            while (!packet.exhausted()) {
                                val bytes = packet.readByteArray()
                                output.write(bytes)
                                bytesDownloaded += bytes.size
                                _syncState.value = CommonUpdateState.Downloading(bytesDownloaded.toFloat() / totalBytes.toFloat())
                            }
                        }
                    }
                }

                downloadedFileAbsolutePath = apkFile.absolutePath
                _syncState.value = CommonUpdateState.ReadyToInstall
                trackUpdateEvent("READY_TO_INSTALL", "Verified Android APK saved: v${release.version}")
            }.onFailure { error ->
                _syncState.value = CommonUpdateState.Failed("Android file sync failed: ${error.message}")
                trackUpdateEvent("DOWNLOAD_FAILED", "APK download error: ${error.message}")
            }
        }
    }

    override fun triggerInstallation() {
        val path = downloadedFileAbsolutePath ?: return
        val file = File(path)

        coroutineScope.launch {
            trackUpdateEvent("INSTALL_TRIGGERED", "Handing off to Android Package Installer Activity.")
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
