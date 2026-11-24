package com.citacomsys.core.data.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

actual fun defaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
