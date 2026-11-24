package com.citacomsys.client.support.dispatchers

import kotlinx.coroutines.CoroutineDispatcher

import kotlinx.coroutines.Dispatchers

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class DispatchersProvider {
    actual fun ioDispatcher(): CoroutineDispatcher = Dispatchers.Default
    actual fun defaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}