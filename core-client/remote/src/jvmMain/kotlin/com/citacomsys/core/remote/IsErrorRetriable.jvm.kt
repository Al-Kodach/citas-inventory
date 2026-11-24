package com.citacomsys.core.remote

import java.io.IOException
import java.util.concurrent.TimeoutException

actual fun canRetry(error: Throwable?): Boolean {
    return when (error) {
        is IOException -> true
        is TimeoutException -> true
        else -> false
    }
}