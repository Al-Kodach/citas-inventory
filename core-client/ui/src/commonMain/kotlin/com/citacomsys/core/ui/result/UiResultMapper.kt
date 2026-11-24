package com.citacomsys.core.ui.result

import com.citacomsys.core.remote.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.io.IOException

/**
 * Converts a domain-layer Result<T> into a presentation-layer UiResult<T>.
 * Intelligently maps the refined ApiException hierarchy to user-friendly messages.
 */
fun <T> Result<T>.asUiResult(): UiResult<T> {
    return fold(
        onSuccess = { UiResult.Success(it) },
        onFailure = { throwable ->
            throwable.printStackTrace()
            when (throwable) {
                is ApiException -> {
                    val canRetry = when (throwable) {
                        is ApiThrottlingException -> false
                        else -> true

                    }
                    UiResult.Error(message = throwable.message, canRetry = canRetry)
                }

                is IOException -> {
                    UiResult.Error(
                        message = "Please check your internet connection.",
                        canRetry = true
                    )
                }

                is NoSuchElementException -> {
                    UiResult.Error(
                        message = "The requested item could not be found.",
                        canRetry = false
                    )
                }

                else -> {
                    UiResult.Error(
                        message = throwable.message ?: "An unexpected error occurred",
                        canRetry = true
                    )
                }
            }
        }
    )
}

inline fun <reified T> Flow<T>.asUiResult(): Flow<UiResult<T>> =
    map<T, UiResult<T>> { UiResult.Success(it) }
        .onStart { emit(UiResult.Loading) }
        .catch { e ->
            e.printStackTrace()

            emit(
                when (e) {
                    is ApiException -> UiResult.Error(e.message, canRetry = true)
                    is IOException -> UiResult.Error("Please check your internet connection.", true)
                    else -> UiResult.Error(
                        message = e.message ?: e::class.simpleName ?: "Unknown error",
                        canRetry = true
                    )
                }
            )
        }

fun <T> UiResult<T>.getOrNull(): T? = if (this is UiResult.Success) data else null
fun <T> UiResult<T>.getOrDefault(default: T): T = if (this is UiResult.Success) data else default
fun <T> UiResult<List<T>>.getOrEmpty(): List<T> = if (this is UiResult.Success) data else emptyList()
fun <T> UiResult<T>.get(): T =
    if (this is UiResult.Success) data else throw IllegalStateException("UiResult is not a Success")

val UiResult<*>.isSuccess: Boolean get() = this is UiResult.Success
val UiResult<*>.isFailure: Boolean get() = this is UiResult.Error

val UiResult<*>.isLoading: Boolean get() = this is UiResult.Loading
val UiResult<*>.isError: Boolean get() = this is UiResult.Error

fun UiResult<*>.getErrorOrNull(): String? = if (this is UiResult.Error) message else null
fun Throwable.asUiError(): UiResult.Error =
    Result.failure<Nothing>(this).asUiResult() as UiResult.Error

