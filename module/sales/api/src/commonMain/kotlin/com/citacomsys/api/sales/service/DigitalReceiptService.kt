package com.citacomsys.api.sales.service

import com.citacomsys.types.PhoneNumber

interface DigitalReceiptService {
    /**
     * Sends a condensed summary of a sale to a patient via SMS.
     */
    suspend fun sendSmsReceipt(saleId: String, phone: PhoneNumber, workspaceId: String): Result<Unit>

    suspend fun sendReceipt(phone: String, receipt: String, workspaceId: String): Result<Unit>
}
