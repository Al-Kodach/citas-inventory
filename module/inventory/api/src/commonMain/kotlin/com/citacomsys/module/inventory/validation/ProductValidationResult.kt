package com.citacomsys.module.inventory.validation


sealed interface ProductValidationResult {
    object Success : ProductValidationResult

    data class Failure(
        val messages: List<String>,
        val fieldErrors: Map<String, String>
    ) : ProductValidationResult {

        // Helper to cleanly merge parent and child validation failures
        fun merge(other: Failure): Failure {
            return Failure(
                messages = this.messages + other.messages,
                fieldErrors = this.fieldErrors + other.fieldErrors
            )
        }
    }
}
