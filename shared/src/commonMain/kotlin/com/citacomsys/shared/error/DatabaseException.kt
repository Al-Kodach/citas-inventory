package com.citacomsys.shared.error

/**
 * A base exception for specific database-related errors that have been translated
 * from low-level driver exceptions. Inherits from PersistenceException to be caught
 * by existing StatusPages handlers if not handled more specifically.
 */
open class DatabaseException(message: String, cause: Throwable? = null) : PersistenceException(message, cause)

/**
 * Thrown when a unique constraint is violated.
 * The 'message' will contain the specific detail from the database.
 * @param constraint The ID of the database constraint that was violated, if available.
 */
class DuplicateRecordException(
    val constraint: String?,
    message: String,
    cause: Throwable? = null
) : DatabaseException(message, cause)

/**
 * Thrown when a foreign key constraint is violated.
 * The 'message' will contain the specific detail from the database.
 * @param constraint The ID of the database constraint that was violated, if available.
 */
class ForeignKeyViolationException(
    val constraint: String?,
    message: String,
    cause: Throwable? = null
) : DatabaseException(message, cause)