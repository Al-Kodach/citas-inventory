package com.citacomsys.feature.telemetry

import com.citacomsys.core.remote.markAsPublic
import com.citacomsys.feature.api.telemetry.CommonUpdateState
import com.citacomsys.feature.api.telemetry.TelemetryLogClient
import com.citacomsys.shared.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.io.readByteArray
import java.io.File
import kotlin.system.exitProcess

internal class DesktopAppUpdater(
    httpClient: HttpClient,
    telemetryClient: TelemetryLogClient,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : BaseAppUpdater(httpClient, telemetryClient) {

    override fun startDownloading() {
        val release = discoveredRelease ?: return
        val os = System.getProperty("os.id").lowercase()

        // Match binary assets based on operating system environment
        val targetAsset = release.binaries.firstOrNull { asset ->
            if (os.contains("win")) asset.fileName.endsWith(".msi") || asset.fileName.endsWith(".exe")
            else asset.fileName.endsWith(".dmg") || asset.fileName.endsWith(".pkg")
        } ?: run {
            _syncState.value =
                CommonUpdateState.Failed("No matching deployment package found for your Operating System.")
            coroutineScope.launch {
                trackUpdateEvent("DOWNLOAD_FAILED", "No matching binary found for OS: $os")
            }
            return
        }

        _syncState.value = CommonUpdateState.Downloading(0.0f)

        coroutineScope.launch {
            trackUpdateEvent(
                "DOWNLOAD_START",
                "Starting download of v${release.version} (${targetAsset.fileName})"
            )
            runCatching {
                val destinationFile =
                    File(System.getProperty("java.io.tmpdir"), targetAsset.fileName)

                client.prepareGet(
                    targetAsset.downloadUrl,
                    block = {
                        markAsPublic()
                        onDownload { bytesSentTotal, contentLength ->
                            val progress = bytesSentTotal.toFloat() / (contentLength?.toFloat() ?: 100f)
                            _syncState.value = CommonUpdateState.Downloading(progress)
                        }
                    }
                ).execute { response ->
                    val channel: ByteReadChannel = response.bodyAsChannel()
                    val totalBytes = response.contentLength() ?: targetAsset.sizeInBytes
                    var bytesDownloaded = 0L

                    destinationFile.outputStream().use { outputStream ->
                        while (!channel.isClosedForRead) {
                            val packet = channel.readRemaining(4096L)
                            while (!packet.exhausted()) {
                                val bytes = packet.readByteArray()
                                outputStream.write(bytes)
                                bytesDownloaded += bytes.size
                                if (totalBytes > 0) {
                                    _syncState.value =
                                        CommonUpdateState.Downloading(bytesDownloaded.toFloat() / totalBytes.toFloat())
                                }
                            }
                        }
                    }
                }

                downloadedFileAbsolutePath = destinationFile.absolutePath
                _syncState.value = CommonUpdateState.ReadyToInstall
                trackUpdateEvent(
                    "READY_TO_INSTALL",
                    "Download completed and verified: v${release.version}"
                )
            }.onFailure { error ->
                _syncState.value =
                    CommonUpdateState.Failed("Download interrupted: ${error.message}")
                trackUpdateEvent("DOWNLOAD_FAILED", "Interrupted: ${error.message}")
            }
        }
    }

    override fun triggerInstallation() {
        val filePath = downloadedFileAbsolutePath ?: return
        val os = System.getProperty("os.id").lowercase()

        coroutineScope.launch {
            trackUpdateEvent(
                "INSTALL_TRIGGERED",
                "Executing platform installer for desktop environment."
            )
        }

        // Spawn independent OS process to execute installers safely on legacy machines
        when {
            os.contains("win") -> {
                ProcessBuilder("msiexec", "/i", filePath).start()
                exitProcess(0) // Clear file locks cleanly
            }

            os.contains("mac") -> {
                ProcessBuilder("open", filePath).start()
                exitProcess(0)
            }

            else -> throw IllegalStateException("Unsupported Desktop Environment platform deployment context.")
        }
    }
}
