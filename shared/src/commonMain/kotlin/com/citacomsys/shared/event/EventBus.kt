package com.citacomsys.shared.event

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlin.reflect.KClass

object EventBus {
    private val _allEvents = MutableSharedFlow<CitasEvent>(
        replay = 0,
        extraBufferCapacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val allEvents = _allEvents.asSharedFlow()

    /**
     * Publishes an event to the bus.
     */
    suspend fun publish(event: CitasEvent) {
        _allEvents.emit(event)
    }

    fun subscribeAll(
        vararg events: CitasEvent,
    ): Flow<CitasEvent> {
        return _allEvents.filter { event -> events.contains(event) }
    }

    /**
     * Returns a Flow that is filtered to a specific event type.
     * The caller is responsible for collecting this flow.
     */
    inline fun <reified T : CitasEvent> subscribe(): Flow<T> =
        subscribe(T::class)

    fun <T : CitasEvent> subscribe(clazz: KClass<T>): Flow<T> {
        return allEvents.filterIsInstance(clazz)
    }
}

