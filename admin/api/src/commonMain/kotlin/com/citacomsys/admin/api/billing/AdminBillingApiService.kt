package com.citacomsys.admin.api.billing

import com.citacomsys.api.billing.BillInvoice
import com.citacomsys.api.billing.BillingRecord

interface AdminBillingApiService {
    suspend fun fetch(): List<BillInvoice>

    suspend fun getRecord(id: String): BillingRecord

    suspend fun verified(reference: String)

    suspend fun reject(
        invoiceId: String,
        reason: String
    )
}