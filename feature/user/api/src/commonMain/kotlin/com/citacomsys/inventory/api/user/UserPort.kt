package com.citacomsys.inventory.api.user

interface UserPort: UserRepository {
   suspend fun updateBranch(userId: String, branchId: String)
   suspend fun updateOrganization(userId: String, organizationId: String)
}
