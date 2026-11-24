package com.citacomsys.backup.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citacomsys.backup.api.BackupBytesResult
import com.citacomsys.backup.api.BackupService
import com.citacomsys.backup.api.RestoreResult
import com.citacomsys.backup.api.RestoreScope
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock

sealed interface BackupRestoreUiState {
    data object Idle : BackupRestoreUiState

    data class Processing(
        val title: String,
        val progress: Float,
        val message: String
    ) : BackupRestoreUiState

    data class Success(val message: String) : BackupRestoreUiState

    data class Error(val message: String) : BackupRestoreUiState
}


class BackupRestoreViewModel(
    private val backupService: BackupService
) : ViewModel() {

    private val _uiState = MutableStateFlow<BackupRestoreUiState>(BackupRestoreUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun createBackup(password: String) {
        viewModelScope.launch {
            try {
                _uiState.value = BackupRestoreUiState.Processing(
                    title = "Compiling Archive",
                    progress = 0.3f,
                    message = "Building safe workspace database snapshot...\nPlease do not close this screen"
                )

                // Generate encrypted archive payload bytes in intermediate staging memory
                when (val byteResult = backupService.generateBackupBytes(password)) {
                    is BackupBytesResult.Success -> {
                        _uiState.value = BackupRestoreUiState.Processing(
                            title = "Exporting Workspace Data",
                            progress = 0.8f,
                            message = "Dispatching file to native platform container..."
                        )

                        val timestamp = Clock.System.now().toEpochMilliseconds()
                        val suggestedName = "pharmacy_backup_$timestamp"

                        // 2. Multiplatform call! Natively triggers Android SAF / JVM Dialogs / Web Blobs
                        FileKit.saveFile(
                            baseName = suggestedName,
                            extension = "cita-backup",
                            bytes = byteResult.bytes
                        )

                        _uiState.value =
                            BackupRestoreUiState.Success("Backup successfully completed.")
                    }

                    is BackupBytesResult.Failure -> {
                        _uiState.value = BackupRestoreUiState.Error(byteResult.message)
                    }
                }
            } catch (e: Exception) {
                _uiState.value =
                    BackupRestoreUiState.Error(e.message ?: "Backup execution collapsed.")
            }
        }
    }

    fun restoreBackup(password: String, scope: RestoreScope = RestoreScope.BOTH, onRestoreComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.value = BackupRestoreUiState.Processing(
                    title = "Selecting Archive File",
                    progress = 0.1f,
                    message = "Waiting for file selection..."
                )

                // Pick the backup archive file via FileKit
                val selectedFile: PlatformFile? = FileKit.openFilePicker(
                    type = FileKitType.File(extensions = listOf("cita-backup"))
                )

                if (selectedFile == null) {
                    _uiState.value = BackupRestoreUiState.Idle
                    return@launch
                }

                _uiState.value = BackupRestoreUiState.Processing(
                    title = "Decrypting Workspace",
                    progress = 0.4f,
                    message = "Verifying authentication parameters and codes..."
                )

                // Run the secure extraction pipeline
                val result = backupService.restoreBackup(
                    backupPath = selectedFile.path,
                    password = password,
                    scope = scope
                )

                when (result) {
                    is RestoreResult.Success -> {
                        _uiState.value =
                            BackupRestoreUiState.Success("Workspace database applied successfully.")
                        // Invoke transition callback to trigger staging orchestrator shift
                        onRestoreComplete()
                    }

                    is RestoreResult.Failure -> {
                        _uiState.value = BackupRestoreUiState.Error(result.message)
                    }
                }

            } catch (e: Exception) {
                _uiState.value =
                    BackupRestoreUiState.Error(e.message ?: "Restore processing broken.")
            }
        }
    }


    fun resetStateToIdle() {
        _uiState.value = BackupRestoreUiState.Idle
    }
}


@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect suspend fun FileKit.saveFile(
    baseName: String,
    extension: String,
    bytes: ByteArray
)
