package com.citacomsys.core.database.util

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * A wrapper around [Mutex] that allows reentrant calls from the same coroutine.
 */
internal class ReentrantMutex {
    private val mutex = Mutex()

    suspend fun <T> withLock(block: suspend () -> T): T {
        val element = currentCoroutineContext()[LockElement]
        return if (element != null && element.mutex === mutex) {
            block()
        } else {
            withContext(currentCoroutineContext() + LockElement(mutex)) {
                mutex.withLock {
                    block()
                }
            }
        }
    }

    private class LockElement(val mutex: Mutex) : CoroutineContext.Element {
        override val key: CoroutineContext.Key<*> get() = Key
        companion object Key : CoroutineContext.Key<LockElement>
    }
}
