package com.citacomsys.shared.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

inline fun <reified T> Flow<T>.mapStateFlow(
    scope: CoroutineScope,
    initialValue: T? = null
): StateFlow<T?> {
    return stateIn(scope, SharingStarted.Eagerly, initialValue)
}

inline fun <reified T, reified R> Flow<T>.mapStateFlow(
    scope: CoroutineScope,
    defaultValue: R,
    crossinline mapper: (value: T?) -> R?
): StateFlow<R> {
    return map {
        mapper(it) ?: defaultValue
    }.stateIn(scope, SharingStarted.Eagerly, defaultValue)
}

inline fun <reified T, reified R> Flow<T>.mapStateFlow(
    scope: CoroutineScope,
    crossinline mapper: (value: T?) -> R?
): StateFlow<R?> {
    return map { mapper(it) }.mapStateFlow(scope)
}