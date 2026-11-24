package com.citacomsys.user.ui.screen

import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.database.UserListItem
import com.citacomsys.types.UserStatus
import com.citacomsys.user.data.ClientUserRepository
import com.citacomsys.test.client.BaseViewModelTest
import com.citacomsys.shared.AppConfig
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TenantUserListViewModelTest : BaseViewModelTest() {

    private val notificationService = mockk<InAppNotificationService>(relaxed = true)
    private val userRepository = mockk<ClientUserRepository>()
    private val sessionManager = mockk<AppSessionManager>()
    private val appConfig = mockk<AppConfig>()

    private lateinit var viewModel: TenantUserListViewModel

    @BeforeTest
    fun setup() {
        setupMain()
        every { sessionManager.activeFilterBranchId } returns MutableStateFlow(null)
        
        viewModel = TenantUserListViewModel(
            notificationService,
            userRepository,
            sessionManager,
            appConfig
        )
    }

    @AfterTest
    fun tearDown() {
        resetMain()
        unmockkAll()
    }

    @Test
    fun `local observation is used by default`() = runTest {
        val mockUsers = listOf(
            UserListItem("1", "User 1", "u1", UserStatus.ACTIVE, "ADMIN", "2024-01-01")
        )
        coEvery { userRepository.observer() } returns flowOf(mockUsers)

        val result = viewModel.itemsState.first { it is UiResult.Success }
        assertTrue(result is UiResult.Success)
        assertEquals(mockUsers, result.data)
    }
}
