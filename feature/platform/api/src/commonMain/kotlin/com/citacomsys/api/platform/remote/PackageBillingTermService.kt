package com.citacomsys.api.platform.remote

import com.citacomsys.api.platform.model.PackageBillingTerm
import com.citacomsys.types.BillingCycle

interface PackageBillingTermService {
    suspend fun getTerm(packageId: String, billingCycle: BillingCycle): PackageBillingTerm
    suspend fun getById(termId: String): PackageBillingTerm
    suspend fun getTerms(packageId: String): List<PackageBillingTerm>
    suspend fun saveTerm(term: PackageBillingTerm)
    suspend fun deleteTerm(termId: String)
}