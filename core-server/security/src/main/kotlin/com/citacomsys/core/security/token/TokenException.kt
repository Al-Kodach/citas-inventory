package com.citacomsys.core.security.token

/**
 * Custom exceptions for token-related operations.
 */
sealed class TokenException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause) {
    class TokenGenerationException(message: String, cause: Throwable? = null) :
        TokenException(message, cause)

    class InvalidToken(message: String, cause: Throwable? = null) :
        TokenException(message, cause)
    class ExpiredToken(message: String, cause: Throwable? = null) :
        TokenException(message, cause)

    class TokenReuseDetected(message: String, cause: Throwable? = null) :
        TokenException(message, cause)

    class TokenSignatureException(message: String, cause: Throwable? = null) :
        TokenException(message, cause)

}