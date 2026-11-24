package com.citacomsys.core.ui.result

import androidx.compose.runtime.Composable
import com.citacomsys.core.ui.result.UiResult.Success
import kotlinx.serialization.Serializable

/**
 * A generic sealed class to represent the result of a UI operation (like a use case call).
 * This provides a structured way for ViewModels to update their UI state.
 *
 * @param T The entityType of the successful data.
 */
@Serializable
sealed class UiResult<out T> {
    @Serializable
    data object Idle : UiResult<Nothing>()

    @Serializable
    data object Loading : UiResult<Nothing>()

    @Serializable
    data class Success<T>(
        val data: T
    ) : UiResult<T>()

    @Serializable
    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : UiResult<Nothing>()


    val isEmpty: Boolean
        get() {
            return (this as? UiResult.Success<List<Any>>)?.getOrDefault(emptySet())?.isEmpty()
                ?: false
        }

}

@Composable
fun<T> UiResult<T>.onSuccess(action: @Composable (T) -> Unit): UiResult<T> {
    if (this is Success) {
        action(data)
    }
    return this
}


fun UiResult<*>.isLoading() = this is UiResult.Loading
fun UiResult<Any>.isSuccess() = this is UiResult.Success
fun UiResult<Any>.isError() = this is UiResult.Error
fun UiResult<Any>.isEmpty() = this is UiResult.Success && this.data is List<*> && this.data.isEmpty()





