package com.citacomsys.app.shared.di

actual fun iODispatcher(): kotlinx.coroutines.CoroutineDispatcher {
    return kotlinx.coroutines.Dispatchers.IO
}