package com.citacomsys.inventory.admin.appRelease

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.citacomsys.admin.api.security.SelectedBinaryAsset
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.BaseViewModel
import com.citacomsys.types.User
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.asMutableStateFlow
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.header
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.collections.emptyList


class AdminReleaseUploadViewModel(
    notification: InAppNotificationService,
    private val httpClient: HttpClient,
    vault: KSafe
) : BaseViewModel(notification) {

    private val activeUser by vault.asMutableStateFlow<User?>(null, viewModelScope)
    val user = activeUser.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0.0f)
    val uploadProgress = _uploadProgress.asStateFlow()

    var versionInput by mutableStateOf("")
    var changelogInput by mutableStateOf("")
    var appFlavorInput by mutableStateOf("PHARMACY_RETAIL")
    var isMandatoryByForce by mutableStateOf(false)

    private val _selectedBinaries = MutableStateFlow<List<SelectedBinaryAsset>>(emptyList())
    val selectedBinaries = _selectedBinaries.asStateFlow()

    private val _isPublishing = MutableStateFlow(false)
    val isPublishing = _isPublishing.asStateFlow()

    /**
     * Integrates FileKit picked components smoothly into the UI staging cache
     */
    fun appendPickedFile(file: PlatformFile) {
        val name = file.name
        val size = file.size()

        // Infer OS target based on extension type safely
        val inferredOs = when {
            name.endsWith(".msi") || name.endsWith(".exe") -> "WINDOWS"
            name.endsWith(".dmg") || name.endsWith(".pkg") -> "MACOS"
            name.endsWith(".apk") -> "ANDROID"
            else -> "LINUX"
        }

        _selectedBinaries.value += SelectedBinaryAsset(
            fileName = name,
            fileSize = size,
            osTarget = inferredOs,
            platformFile = file
        )
    }

    fun removeAsset(asset: SelectedBinaryAsset) {
        _selectedBinaries.value -= asset
    }

    /**
     * Executes an authenticated Multipart Form multi-stream transmission to Ktor server
     */

    fun publishBuildRelease(onSuccessCallback: () -> Unit) {
        if (versionInput.isBlank() || changelogInput.isBlank() || _selectedBinaries.value.isEmpty()) {
            showError("Please complete all mandatory release form properties.")
            return
        }

        viewModelScope.launch {
            _isPublishing.value = true
            _uploadProgress.value = 0.0f // Reset

            runCatching {
                httpClient.submitFormWithBinaryData(
                    url = "/api/v1/admin/app-releases/publish",
                    formData = formData {
                        append("version", versionInput)
                        append("appFlavor", appFlavorInput)
                        append("changelog", changelogInput)
                        append("isMandatory", isMandatoryByForce.toString())

                        _selectedBinaries.value.forEachIndexed { index, asset ->
                            val fileRef = asset.platformFile ?: return@forEachIndexed
                            val fileBytes = fileRef.readBytes()

                            append("binary_$index", fileBytes, Headers.build {
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "filename=\"${asset.fileName}\""
                                )
                                append("X-OS-Target", asset.osTarget)
                            })
                        }
                    }
                ) {
                    // KTOR NATIVE PROGRESS HOOK [1, 2]
                    onUpload { bytesSent, totalBytes ->
                        if (totalBytes != null && totalBytes > 0) {
                            _uploadProgress.value = bytesSent.toFloat() / totalBytes.toFloat()
                        }
                    }
                }

                showSuccess("Enterprise software upgrade build package deployment finalized!")
                onSuccessCallback()
            }.onFailure { error ->
                showError("Handshake failure: ${error.message}")
            }
            _isPublishing.value = false
        }
    }

}
