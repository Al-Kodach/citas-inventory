package com.citacomsys.device.service

import com.citacomsys.types.DeviceStatus
import com.citacomsys.shared.AppConfig
import kotlin.time.Clock
import com.citacomsys.types.HardwareMasterStatus
import com.citacomsys.types.HardwareTechInfo
import com.citacomsys.types.UserDeviceInfo
import eu.anifantakis.lib.ksafe.KSafe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.apache.pdfbox.printing.PDFPageable
import org.apache.pdfbox.rendering.PDFRenderer
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.awt.print.PrinterJob
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.InetAddress
import javax.imageio.ImageIO
import java.nio.file.Files
import java.util.UUID
import io.github.aakira.napier.Napier

internal class DesktopDeviceService(
    private val appConfig: AppConfig,
    private val ksafe: KSafe
) : DeviceService {
    private val deviceIdKey = appConfig.appPrefix + "_persistent_device_id"

    private val os = System.getProperty("os.ID") ?: "JVM Desktop"
    private val version = System.getProperty("os.version") ?: "Unknown"
    private val arch = System.getProperty("os.arch") ?: "Unknown"

    private val deviceId: String by lazy {
        val stored = ksafe.getDirect(deviceIdKey, "")
        if (stored.isNotBlank()) {
            stored
        } else {
            val newId = UUID.randomUUID().toString()
            ksafe.putDirect(deviceIdKey, newId)
            newId
        }
    }
    private val localDeviceName: String by lazy {
        runCatching {
            InetAddress.getLocalHost().hostName
        }.getOrNull()?.takeIf { it.isNotBlank() && it != "localhost" }
            ?: System.getenv("COMPUTERNAME")
            ?: System.getenv("HOSTNAME")
            ?: "$os-$arch"
    }

    private val _deviceInfo = MutableStateFlow(
        UserDeviceInfo(
            hardwareId = deviceId,
            status = DeviceStatus.ACTIVE,
            displayName = localDeviceName,
            lastUsedAt = Clock.System.now()
        )
    )

    override val deviceInfo: StateFlow<UserDeviceInfo> = _deviceInfo.asStateFlow()

    override suspend fun getDeviceId(): String = deviceId

    override suspend fun getHardwareInfo(): HardwareTechInfo {
        // High-fidelity fingerprint for JVM
        val cpuCores = Runtime.getRuntime().availableProcessors().toString()
        val totalMem = (Runtime.getRuntime().totalMemory() / (1024 * 1024)).toString() + "MB"
        val fingerprintSource = "$os|$version|$arch|$cpuCores|$totalMem"
        val fingerprint = UUID.nameUUIDFromBytes(fingerprintSource.toByteArray()).toString()

        return HardwareTechInfo(
            hardwareId = deviceId,
            fingerprint = fingerprint,
            model = version,
            osName = os,
            architecture = arch,
            appFlavor = "",
            masterStatus = HardwareMasterStatus.ACTIVE,
            updatedAt = Clock.System.now()
        )
    }

    override suspend fun updateDeviceInfo(info: UserDeviceInfo) {
        _deviceInfo.value = info
    }

    override suspend fun generateContentPdf(content: List<PdfLineInfo>): ByteArray =
        withContext(Dispatchers.IO) {
            ByteArrayOutputStream().use { out ->
                PDDocument().use { document ->
                    val page = PDPage(PDRectangle.A4)
                    document.addPage(page)

                    val margin = 50f
                    var currentY = page.mediaBox.height - margin
                    var currentX = margin
                    var currentFontSize = 12f

                    val defaultFont = PDType1Font(Standard14Fonts.FontName.HELVETICA)
                    val boldFont = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)

                    PDPageContentStream(document, page).use { stream ->
                        stream.setFont(defaultFont, currentFontSize)

                        content.forEach { line ->
                            when (line.type) {
                                ValueType.FONT_BOLD -> {
                                    currentFontSize = line.value.toFloatOrNull() ?: 12f
                                    stream.setFont(boldFont, currentFontSize)
                                }

                                ValueType.FONT_REGULAR -> {
                                    currentFontSize = line.value.toFloatOrNull() ?: 12f
                                    stream.setFont(defaultFont, currentFontSize)
                                }

                                ValueType.TEXT -> {
                                    stream.beginText()
                                    stream.newLineAtOffset(currentX, currentY)
                                    stream.showText(line.value)
                                    stream.endText()
                                    currentY -= currentFontSize + 8f
                                }

                                ValueType.NEW_LINE -> {
                                    currentY -= currentFontSize + 8f
                                }

                                ValueType.SPACE -> {
                                    currentX += 10f
                                }
                            }
                        }
                    }
                    document.save(out)
                }
                out.toByteArray()
            }
        }

    override suspend fun downloadFile(data: ByteArray, fileName: String) {
        withContext(Dispatchers.IO) {
            val userHome = System.getProperty("user.home") ?: "."
            val downloadsDir = File(userHome, "Downloads")
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val defaultFile = File(downloadsDir, fileName)
            Files.write(defaultFile.toPath(), data)
            Napier.d(
                "File saved to default Downloads directory: ${defaultFile.absolutePath}",
                tag = "DesktopDeviceService"
            )

            // Show native OS Save File dialog
            try {
                val dialog = FileDialog(null as Frame?, "Save Document", FileDialog.SAVE).apply {
                    file = fileName
                    directory = downloadsDir.absolutePath
                    isVisible = true
                }

                val selectedDir = dialog.directory
                val selectedFileName = dialog.file
                if (selectedDir != null && selectedFileName != null) {
                    val targetFile = File(selectedDir, selectedFileName)
                    Files.write(targetFile.toPath(), data)
                    Napier.d(
                        "File saved via FileDialog to: ${targetFile.absolutePath}",
                        tag = "DesktopDeviceService"
                    )

                    // Automatically open the saved document in default viewer
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop()
                            .isSupported(Desktop.Action.OPEN)
                    ) {
                        Desktop.getDesktop().open(targetFile)
                    }
                }
            } catch (e: Exception) {
                Napier.d(
                    "Native FileDialog/Open failed: ${e.message}",
                    tag = "DesktopDeviceService"
                )
            }
        }
    }

    override suspend fun print(data: ByteArray, fileName: String, silent: Boolean) {
        withContext(Dispatchers.IO) {
            val document = Loader.loadPDF(data)
            val printJob = PrinterJob.getPrinterJob()
            printJob.setPageable(PDFPageable(document))
            if (silent) {
                printJob.print()
            } else {
                if (printJob.printDialog()) {
                    printJob.print()
                }
            }
            document.close()
        }
    }

    override suspend fun renderPdfToImages(data: ByteArray): List<ByteArray> =
        withContext(Dispatchers.IO) {
            val document = Loader.loadPDF(data)
            val renderer = PDFRenderer(document)
            val images = mutableListOf<ByteArray>()

            for (i in 0 until document.numberOfPages) {
                val image = renderer.renderImageWithDPI(i, 150f)
                ByteArrayOutputStream().use { out ->
                    ImageIO.write(image, "png", out)
                    images.add(out.toByteArray())
                }
            }
            document.close()
            images
        }
}
