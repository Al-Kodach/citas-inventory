package com.citacomsys.shared.validation

sealed interface ValidationResult {
    data object Success : ValidationResult
    data class Failure(val message: String) : ValidationResult

    val isValid: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure
    val errorMessage: String? get() = (this as? Failure)?.message
}

typealias Validator<T> = (T) -> ValidationResult
