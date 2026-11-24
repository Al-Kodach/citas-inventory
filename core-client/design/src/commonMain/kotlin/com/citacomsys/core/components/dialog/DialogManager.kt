package com.citacomsys.core.components.dialog

import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

abstract class DialogManager<TData : Any> {
    protected val _runtimeState = MutableStateFlow(DialogRuntimeState())
    val runtimeState: StateFlow<DialogRuntimeState> = _runtimeState.asStateFlow()

    protected abstract val _dialogStateFlow: MutableStateFlow<DialogState<TData?>>
    val dialogStateFlow: StateFlow<DialogState<TData?>> get() = _dialogStateFlow.asStateFlow()

    fun showDialog(
        initialData: TData? = null,
        initialDState: DialogRuntimeState = DialogRuntimeState(),
        onDismiss: () -> Unit = {},
        dialogContent: @Composable (uiState: DialogRuntimeState, data: TData?) -> Unit
    ) {
        _runtimeState.value = initialDState
        _dialogStateFlow.value = DialogState(
            isShown = true,
            dialogContent = dialogContent,
            data = initialData,
            dState = initialDState,
            onDismiss = onDismiss
        )
    }

    fun dismissAndClearState() {
        _dialogStateFlow.value = DialogState(isShown = false, data = null)
        _runtimeState.value = DialogRuntimeState()
    }

    fun updateRuntimeState(fn: (currentState: DialogRuntimeState) -> DialogRuntimeState) {
        val newDState = fn(_runtimeState.value)
        _runtimeState.value = newDState
        _dialogStateFlow.update { it.copy(dState = newDState) }
    }

    fun updateData(fn: (currentData: TData?) -> TData?) {
        _dialogStateFlow.update { it.copy(data = fn(it.data)) }
    }

    data class DialogState<TData>(
        val isShown: Boolean = false,
        val dialogContent: @Composable (uiState: DialogRuntimeState, data: TData) -> Unit = { _, _ -> },
        val data: TData,
        val dState: DialogRuntimeState = DialogRuntimeState(),
        val onDismiss: () -> Unit = {}
    )

    data class DialogRuntimeState(
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val successMessage: String? = null
    )

    companion object {
        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        fun <T : Any> DialogHost(
            state: DialogState<T?>,
            onDismissRequest: () -> Unit,
            shape: Shape = MaterialTheme.shapes.extraLarge,
        ) {
            if (!state.isShown) return

            BasicAlertDialog(onDismissRequest = onDismissRequest) {
                Surface(shape = shape, tonalElevation = 6.dp) {
                    state.dialogContent.invoke(state.dState, state.data)
                }
            }
        }
    }
}
