package com.citacomsys.core.security.mfa.data

import com.citacomsys.core.security.mfa.OtpRecord
import java.util.UUID
import kotlin.time.Instant

internal interface OtpDataStore {

    suspend fun save(
        userId: UUID,
        purpose: String,
        prefix: String,
        requestId: String,
        hashedCode: String,
        expiresAt: Instant
    )

    /**
     * Finds the most recent OTP record for a user that is still active.
     * Used to determine the 'type' during generic resend requests.
     */
    suspend fun findLatestPending(
        userId: String,
        purpose: String
    ): OtpRecord?

    suspend fun markAsUsed(id: String)
    
    suspend fun markAsCancelled(id: String)
}
