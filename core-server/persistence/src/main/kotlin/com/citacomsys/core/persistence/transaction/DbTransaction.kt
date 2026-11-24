package com.citacomsys.core.persistence.transaction

import com.citacomsys.shared.error.DuplicateRecordException
import com.citacomsys.shared.error.ForeignKeyViolationException
import com.citacomsys.shared.error.PersistenceException
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.VarCharColumnType
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.postgresql.util.PSQLException

// Standard SQL state codes
private const val UNIQUE_VIOLATION = "23505"
private const val FOREIGN_KEY_VIOLATION = "23503"
private const val H2_UNIQUE_VIOLATION_CODE = 23505

suspend fun <T> dbQuery(block: suspend Transaction.() -> T): T {
    return suspendTransaction {
        try {
            block(this)
        } catch (e: ExposedSQLException) {
            e.printStackTrace()
            throw translateException(e)
        }
    }
}

/**
 * Translates a low-level ExposedSQLException into a clean, specific domain exception.
 * This function is now multi-database aware.
 */

private fun translateException(e: ExposedSQLException): Exception {
    val cause = e.cause

    when (cause) {
        // --- PRODUCTION DATABASE (PostgreSQL) ---
        is PSQLException -> {
            val serverMessage = cause.serverErrorMessage
            val constraintName = serverMessage?.constraint

            return when (cause.sqlState) {
                UNIQUE_VIOLATION -> resolveUniqueConstraint(
                    constraintName,
                    serverMessage?.detail,
                    e
                )

                FOREIGN_KEY_VIOLATION -> ForeignKeyViolationException(
                    constraint = constraintName,
                    message = "A related parent record (workspace, product, or branch) was not found.",
                    cause = e
                )

                else -> PersistenceException("Database error: ${e.message}", e)
            }
        }

        // --- TESTING DATABASE (H2) ---
//        is JdbcSQLIntegrityConstraintViolationException -> {
//            val errorMessage = cause.message ?: ""
//            // H2 dumps the index ID directly inside uppercase text quotes or after a string identifier
//            val constraintName = when {
//                errorMessage.contains("UIDX_PRODUCTS_TENANT_SKU", ignoreCase = true) -> "uidx_products_tenant_sku"
//                errorMessage.contains("UIDX_PRODUCTS_TENANT_BARCODE", ignoreCase = true) -> "uidx_products_tenant_barcode"
//                errorMessage.contains("UIDX_STOCK_BATCHES_UNIQUE_ENTRY", ignoreCase = true) -> "uidx_stock_batches_unique_entry"
//                else -> null
//            }
//
//            // H2 uses standard SQL State "23505" for unique violations
//            return if (cause.sqlState == UNIQUE_VIOLATION) {
//                resolveUniqueConstraint(constraintName, errorMessage, e)
//            } else {
//                ForeignKeyViolationException(
//                    constraint = constraintName,
//                    message = "A related constraint was breached during testing.",
//                    cause = e
//                )
//            }
//        }
//    }
    }

    return PersistenceException("An unexpected database error occurred. ${e.message}", e)
}

/**
 * Clean routing block targeting specific database constraints safely across drivers.
 */
private fun resolveUniqueConstraint(
    constraintName: String?,
    rawDetail: String?,
    cause: ExposedSQLException
): Exception {
    return when (constraintName) {
        "uidx_products_tenant_sku" -> DuplicateRecordException(
            constraint = constraintName,
            message = "The SKU you entered is already in use within this workspace.",
            cause = cause
        )

        "uidx_products_tenant_barcode" -> DuplicateRecordException(
            constraint = constraintName,
            message = "The Barcode you entered is already registered in this workspace.",
            cause = cause
        )
        // Multi-column constraint match!
        "uidx_stock_batches_unique_entry" -> DuplicateRecordException(
            constraint = constraintName,
            message = "A batch with this exact batch number already exists for this product at this branch.",
            cause = cause
        )

        else -> {
            val fallbackMessage = rawDetail ?: "A record with this value already exists."
            DuplicateRecordException(
                constraint = constraintName,
                message = parsePostgresDetail(fallbackMessage),
                cause = cause
            )
        }
    }
}


/**
 * A helper to parse the verbose detail message from PostgreSQL.
 * E.g., "Key (pharmacy_id, product_id, batch_number)=(..., ..., ...) already exists."
 * -> "An item with this batch number already exists for this product."
 */
private fun parsePostgresDetail(detail: String): String {
    if (detail.contains("batch_number")) {
        return "An inventory item with this batch number already exists for this product."
    }
    if (detail.contains("username")) {
        return "A user with this username already exists."
    }


    return "A record with these details already exists." // Generic fallback
}

