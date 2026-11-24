package com.citacomsys.core.sync

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class KmpPriorityQueue<T : Comparable<T>> {
    private val lock = Mutex()
    private val items = mutableListOf<T>()
    private val signal = Channel<Unit>(Channel.CONFLATED)

    suspend fun put(item: T) {
        lock.withLock {
            items.add(item)
            items.sort() // Keeps items sorted by priority
        }
        signal.trySend(Unit)
    }

    suspend fun take(): T {
        while (true) {
            lock.withLock {
                if (items.isNotEmpty()) {
                    return items.removeAt(0)
                }
            }
            signal.receive() // Suspend until an item is added
        }
    }
}