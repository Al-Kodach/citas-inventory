package com.citacomsys.client.support.dispatchers

import kotlinx.coroutines.CoroutineDispatcher

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class DispatchersProvider {
    fun ioDispatcher(): CoroutineDispatcher
    fun defaultDispatcher(): CoroutineDispatcher
}