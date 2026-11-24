package com.citacomsys.inventory.admin.common.ui

import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.management.ListQueryState
import com.citacomsys.core.ui.management.SaaSListViewModel
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

abstract class AdminListItemsBaseViewModel<V>(
    notificationService: InAppNotificationService,
    sessionManager: AppSessionManager
) : SaaSListViewModel<V>(notificationService, sessionManager) {

    // Default implementation for Admin lists which might not have local DB
}
