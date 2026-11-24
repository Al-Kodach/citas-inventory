package com.citacomsys.shared.util

import io.github.aakira.napier.Napier

/**
 * Executes a suspending block of code and wraps its outcome in a [Result].
 * This provides a clean entry point for a chain of Result-based operations.
 *
 * @param block The suspend function to execute.
 * @return A [Result] containing the success value or the thrown exception.
 */
suspend inline fun <T> resultOf(crossinline block: suspend () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: Throwable) {
        Result.failure(e)
    }
}

/**
 * A custom operator for the [Result] type that attempts to recover from a failure.
 *
 * It inspects the exception from a failed Result. If the exception matches the
 * [shouldRecover] predicate, it executes the [recoverAction] and returns its
 * successful result. If the predicate is false, or if the recovery action itself
 * fails, the original failure is returned.
 *
 * @param shouldRecover A predicate that takes the `Throwable` and returns `true` if a recovery should be attempted.
 * @param recoverAction A suspending lambda that performs the recovery operation and returns a new success value `T`.
 * @return The original success `Result`, a new success `Result` from the recovery action, or the original failure `Result`.
 */
suspend inline fun <T> Result<T>.tryOrRecover(
    crossinline shouldRecover: (error: Throwable) -> Boolean,
    crossinline recoverAction: suspend (error: Throwable) -> T
): Result<T> {
    // Get the exception if the original result is a failure, otherwise return early.
    val exception = exceptionOrNull() ?: return this

    if (shouldRecover(exception)) {
        return runCatching {
            Napier.w(exception) { "Original operation failed. Attempting recovery..." }
            recoverAction(exception)
        }.onFailure { recoveryError ->
            Napier.e(recoveryError) { "Recovery action also failed. Propagating original error." }
             exception.addSuppressed(recoveryError)
        }
    }

    return this
}