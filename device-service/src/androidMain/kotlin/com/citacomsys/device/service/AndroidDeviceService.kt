package com.citacomsys.device.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.citacomsys.types.DeviceStatus
import com.citacomsys.types.HardwareMasterStatus
import com.citacomsys.types.HardwareTechInfo
import com.citacomsys.types.UserDeviceInfo
import eu.anifantakis.lib.ksafe.KSafe
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import kotlin.time.Clock
import androidx.core.graphics.createBitmap

internal class AndroidDeviceService(
    private val context: Context,
    private val ksafe: KSafe
) : DeviceService {

    private val deviceId: String by lazy {
        val stored = ksafe.getDirect("persistent_device_id", "")
        if (stored.isNotBlank()) {
            stored
        } else {
            val newId = UUID.randomUUID().toString()
            ksafe.putDirect("persistent_device_id", newId)
            newId
        }
    }
    private val deviceName: String by lazy {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        if (model.startsWith(manufacturer, ignoreCase = true)) {
            model.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } else {
            "${manufacturer.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} $model"
        }
    }

    private val _deviceInfo = MutableStateFlow(
        UserDeviceInfo(
            hardwareId = deviceId,
            status = DeviceStatus.ACTIVE,
            displayName = deviceName,
            lastUsedAt = Clock.System.now()
        )
    )
    override val deviceInfo: StateFlow<UserDeviceInfo> = _deviceInfo.asStateFlow()

    override suspend fun getDeviceId(): String = deviceId

    override suspend fun getHardwareInfo(): HardwareTechInfo {
        return HardwareTechInfo(
            hardwareId = getDeviceId(),
            fingerprint = Build.FINGERPRINT,
            model = "${Build.MANUFACTURER} ${Build.MODEL}",
            osName = "Android ${Build.VERSION.RELEASE}",
            architecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
            appFlavor = "Android",
            masterStatus = HardwareMasterStatus.ACTIVE,
            updatedAt = Clock.System.now()
        )
    }

    override suspend fun updateDeviceInfo(info: UserDeviceInfo) {
        _deviceInfo.value = info
    }

    override suspend fun generateContentPdf(content: List<PdfLineInfo>): ByteArray =
        withContext(Dispatchers.IO) {
            val pdfDocument = PdfDocument()
            val pageInfo =
                PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 dimensions (595x842 pt)
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 12f
                isAntiAlias = true
                typeface = Typeface.DEFAULT
            }

            var currentY = 50f
            var currentX = 40f
            var currentFontSize = 12f

            content.forEach { line ->
                when (line.type) {
                    ValueType.FONT_BOLD -> {
                        currentFontSize = line.value.toFloatOrNull() ?: 12f
                        paint.textSize = currentFontSize
                        paint.typeface = Typeface.DEFAULT_BOLD
                    }

                    ValueType.FONT_REGULAR -> {
                        currentFontSize = line.value.toFloatOrNull() ?: 12f
                        paint.textSize = currentFontSize
                        paint.typeface = Typeface.DEFAULT
                    }

                    ValueType.TEXT -> {
                        canvas.drawText(line.value, currentX, currentY, paint)
                        currentY += currentFontSize + 6f
                    }

                    ValueType.NEW_LINE -> {
                        currentY += currentFontSize + 6f
                    }

                    ValueType.SPACE -> {
                        currentX += 10f
                    }
                }
            }

            pdfDocument.finishPage(page)

            ByteArrayOutputStream().use { out ->
                pdfDocument.writeTo(out)
                pdfDocument.close()
                out.toByteArray()
            }
        }

    override suspend fun downloadFile(data: ByteArray, fileName: String): Unit =
        withContext(Dispatchers.IO) {
            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val targetFile = File(downloadsDir, fileName)
            FileOutputStream(targetFile).use { out ->
                out.write(data)
                out.flush()
            }
            Napier.d(
                "File saved to Downloads: ${targetFile.absolutePath}",
                tag = "AndroidDeviceService"
            )
        }

    override suspend fun print(data: ByteArray, fileName: String, silent: Boolean): Unit =
        withContext(Dispatchers.Main) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                ?: throw IllegalStateException("PrintManager not available on this device")

            val tempPdfFile = File(context.cacheDir, fileName.ifBlank { "print_doc.pdf" })
            withContext(Dispatchers.IO) {
                FileOutputStream(tempPdfFile).use { out ->
                    out.write(data)
                    out.flush()
                }
            }

            val printAdapter = object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }

                    val info = PrintDocumentInfo.Builder(fileName)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1)
                        .build()

                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    try {
                        FileInputStream(tempPdfFile).use { input ->
                            FileOutputStream(destination?.fileDescriptor).use { output ->
                                input.copyTo(output)
                            }
                        }
                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    }
                }
            }

            printManager.print(
                fileName,
                printAdapter,
                PrintAttributes.Builder().build()
            )
        }

    override suspend fun renderPdfToImages(data: ByteArray): List<ByteArray> =
        withContext(Dispatchers.IO) {
            val tempFile = File.createTempFile("pdf_render", ".pdf", context.cacheDir)
            FileOutputStream(tempFile).use { out ->
                out.write(data)
            }

            val fileDescriptor =
                ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)
            val pageImages = mutableListOf<ByteArray>()

            for (i in 0 until pdfRenderer.pageCount) {
                val page = pdfRenderer.openPage(i)
                val bitmap = createBitmap(page.width * 2, page.height * 2)
                page.render(
                    bitmap,
                    null,
                    null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                )
                page.close()

                ByteArrayOutputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    pageImages.add(out.toByteArray())
                }
            }

            pdfRenderer.close()
            fileDescriptor.close()
            tempFile.delete()

            pageImages
        }
}
