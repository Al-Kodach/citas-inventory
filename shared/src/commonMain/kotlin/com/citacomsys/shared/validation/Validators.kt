package com.citacomsys.shared.validation

expect fun phoneValidatory(
    message: String = "Invalid phone number"
): Validator<String>

object Validators {

    fun required(message: String = "This field is required"): Validator<String> = { value ->
        if (value.isBlank()) ValidationResult.Failure(message) else ValidationResult.Success
    }

    fun minLength(length: Int, message: String? = null): Validator<String> = { value ->
        val msg = message ?: "Must be at least $length characters"
        if (value.length < length) ValidationResult.Failure(msg) else ValidationResult.Success
    }

    fun email(message: String = "Invalid email address"): Validator<String> = { value ->
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()
        if (value.isBlank() || !emailRegex.matches(value)) ValidationResult.Failure(message) else ValidationResult.Success
    }

    fun website(message: String = "Invalid website URL"): Validator<String> = { value ->
        val websiteRegex =
            "^(https?://)?(www\\.)?[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)+([/?].*)?$".toRegex()
        if (value.isBlank() || !websiteRegex.matches(value)) ValidationResult.Failure(message) else ValidationResult.Success
    }

    fun phone(
        message: String = "Invalid phone number"
    ): Validator<String> = phoneValidatory()

    fun matches(other: String, message: String = "Values do not match"): Validator<String> =
        { value ->
            if (value != other) ValidationResult.Failure(message) else ValidationResult.Success
        }
}

/**
 * Executes a list of validators on a target value and returns the first failure found, or Success.
 */
fun <T> validate(value: T, vararg validators: Validator<T>): ValidationResult {
    for (validator in validators) {
        val result = validator(value)
        if (result is ValidationResult.Failure) return result
    }
    return ValidationResult.Success
}

fun MutableList<String>.addNotNull(vararg err: String?) {
    val list = err.toList().filterNotNull()
    this.addAll(list)
}
