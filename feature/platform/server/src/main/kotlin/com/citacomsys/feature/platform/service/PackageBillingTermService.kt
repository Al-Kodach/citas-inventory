package com.citacomsys.feature.platform.service

import com.citacomsys.api.platform.model.PackageBillingTerm
import com.citacomsys.api.platform.repository.PackageBillingTermRepository
import com.citacomsys.api.platform.remote.PackageBillingTermService
import com.citacomsys.types.BillingCycle

internal class PackageBillingTermServiceIml(
    private val billingTermRepository: PackageBillingTermRepository
) : PackageBillingTermService {

    override suspend fun getTerm(
        packageId: String,
        billingCycle: BillingCycle
    ): PackageBillingTerm {
        return billingTermRepository.getTerm(packageId, billingCycle) ?: error("Billing Term not found")
    }

    override suspend fun getById(termId: String): PackageBillingTerm {
        return billingTermRepository.getById(termId) ?: error("Billing Term not found")
    }

    override suspend fun getTerms(packageId: String): List<PackageBillingTerm> {
        return billingTermRepository.getTerms(packageId)
    }

    override suspend fun saveTerm(term: PackageBillingTerm) {
        billingTermRepository.saveTerm(term)
    }

    override suspend fun deleteTerm(termId: String) {
        billingTermRepository.deleteTerm(termId)
    }

}