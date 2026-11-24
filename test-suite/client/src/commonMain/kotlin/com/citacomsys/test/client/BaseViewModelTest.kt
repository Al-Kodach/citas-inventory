package com.citacomsys.test.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Base class for client-side ViewModel unit tests.
 * Provides Main dispatcher mocking helpers.
 * 
 * Note: Subclasses should call [setupMain] in @BeforeTest and [resetMain] in @AfterTest.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class BaseViewModelTest {

    protected val testDispatcher = StandardTestDispatcher()

    fun setupMain() {
        Dispatchers.setMain(testDispatcher)
    }

    fun resetMain() {
        Dispatchers.resetMain()
    }
}
