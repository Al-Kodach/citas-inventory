package com.citacomsys.app.shared.di

import io.ktor.client.utils.clientDispatcher
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@OptIn(InternalAPI::class)
actual fun iODispatcher(): CoroutineDispatcher {
    return Dispatchers.clientDispatcher(3)
}

