package com.citacomsys.inventory.admin.common.ui

import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.management.SaaSDetailsViewModel
import com.citacomsys.core.security.sudo.ActorSudoAuthHandler

abstract class AdminBaseDetailsViewModel<T>(
    notificationService: InAppNotificationService,
    sudoAuthHandler: ActorSudoAuthHandler
) : SaaSDetailsViewModel<T>(
    notificationService,
    sudoAuthHandler
)
