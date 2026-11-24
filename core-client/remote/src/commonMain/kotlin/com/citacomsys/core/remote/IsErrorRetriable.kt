package com.citacomsys.core.remote

import io.github.aakira.napier.Napier
import kotlinx.io.IOException

fun isErrorRetriable(error: Throwable?): Boolean {
    Napier.e(error) { "Error is retriable check for ${error?.message}" }
    return when (error) {
        is IOException -> true
        is ApiServerException -> true
        is ApiNetworkException -> true
        is ApiNotFoundException -> true
        is ApiValidationException -> error.message.contains("not found", ignoreCase = true)
        else -> error?.message?.contains("not found", ignoreCase = true) == true
    } || canRetry(error)
}

expect fun canRetry(error: Throwable?): Boolean
