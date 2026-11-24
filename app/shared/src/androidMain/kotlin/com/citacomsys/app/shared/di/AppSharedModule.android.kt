package com.citacomsys.app.shared.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual fun iODispatcher(): CoroutineDispatcher = Dispatchers.IO
