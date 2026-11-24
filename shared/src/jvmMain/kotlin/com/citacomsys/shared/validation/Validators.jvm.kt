package com.citacomsys.shared.validation

import com.citacomsys.types.jvm.JvmPhoneNumber
import java.lang.Exception

actual fun phoneValidatory(message: String): Validator<String> = { value ->
    try {
        JvmPhoneNumber.create(value)
        ValidationResult.Success
    } catch (e: Exception) {
        ValidationResult.Failure(e.message ?: "Invalid phone number")
    }
}