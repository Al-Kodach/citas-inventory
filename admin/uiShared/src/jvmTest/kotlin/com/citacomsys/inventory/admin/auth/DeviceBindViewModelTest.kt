package com.citacomsys.inventory.admin.auth

import com.citacomsys.core.domain.security.ActionConfirmationManager
import com.citacomsys.core.domain.security.AuthService
import com.citacomsys.core.domain.storage.TokenStore
import com.citacomsys.core.notification.InAppNotificationService
import com.citacomsys.test.client.BaseViewModelTest
import com.citacomsys.device.service.DeviceService
import com.citacomsys.types.BindDeviceResponse
import com.citacomsys.types.HardwareInfo
import eu.anifantakis.lib.ksafe.KSafe
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.properties.ReadOnlyProperty
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceBindViewModelTest : BaseViewModelTest() {

    private val deviceService = mockk<DeviceService>(relaxed = true)
    private val authApi = mockk<AuthService>()
    private val tokenStore = mockk<TokenStore>(relaxed = true)
    private val ksafe = mockk<KSafe>(relaxed = true)
    private val actionConfirmationManager = mockk<ActionConfirmationManager>(relaxed = true)
    private val notificationService = mockk<InAppNotificationService>(relaxed = true)

    private lateinit var viewModel: DeviceBindViewModel

    @BeforeTest
    fun setup() {
        setupMain()
        
        every { deviceService.deviceInfo } returns MutableStateFlow(HardwareInfo.EMPTY.copy(osName = "Test OS"))
        coEvery { deviceService.getDeviceId() } returns "test-device-id"
        
        // Mock KSafe StateFlows
        every { ksafe.asMutableStateFlow(any<DeviceBindUiState>(), any(), any()) } returns ReadOnlyProperty { _, _ -> MutableStateFlow(DeviceBindUiState()) }
        every { ksafe.asMutableStateFlow<BindDeviceResponse?>(any(), any(), any()) } returns ReadOnlyProperty { _, _ -> MutableStateFlow(null) }

        viewModel = DeviceBindViewModel(
            deviceService,
            authApi,
            tokenStore,
            ksafe,
            actionConfirmationManager,
            notificationService
        )
    }

    @AfterTest
    fun tearDown() {
        resetMain()
        unmockkAll()
    }

    @Test
    fun `initial state shows device info`() = runTest {
        assertEquals("Test OS", viewModel.state.value.deviceName)
        assertEquals("test-device-id", viewModel.state.value.deviceId)
    }

    @Test
    fun `successful OTP request transitions to OtpRequired state`() = runTest {
        val mockResponse = BindDeviceResponse.OtpRequired("session-1", "+233XXXXXX90")
        coEvery { authApi.bindAdminDevice(any()) } returns Result.success(mockResponse)

        viewModel.initiateBinding()

        assertTrue(viewModel.flowResponse.value is BindDeviceResponse.OtpRequired)
        assertEquals("+233XXXXXX90", (viewModel.flowResponse.value as BindDeviceResponse.OtpRequired).maskedIdentifier)
    }

    @Test
    fun `awaiting approval state is handled`() = runTest {
        val mockResponse = BindDeviceResponse.AwaitingApproval("session-1", "test-device-id")
        coEvery { authApi.bindAdminDevice(any()) } returns Result.success(mockResponse)

        viewModel.initiateBinding()

        assertTrue(viewModel.flowResponse.value is BindDeviceResponse.AwaitingApproval)
    }
}
