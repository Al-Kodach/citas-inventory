package com.citacomsys.inventory.admin.security.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citacomsys.shared.event.EventBus
import com.citacomsys.shared.event.EntityChangeEvent
import eu.anifantakis.lib.ksafe.KSafe
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LiveSecurityActivityViewModel(
    private val prefs: KSafe
) : ViewModel() {

    private var seq: Long
        get() = prefs.getDirect("seq_long", 0L)
        set(value) = prefs.putDirect("seq_log", value)

    private val _events = MutableStateFlow<List<EntityChangeEvent>>(emptyList())
    val events: StateFlow<List<EntityChangeEvent>> = _events.asStateFlow()

    init {
        viewModelScope.launch {
            EventBus.subscribe<EntityChangeEvent>().collect { event ->
                _events.update { current ->
                    (listOf(event) + current).take(20) // Keep last 20 events
                }
            }
        }
    }
}
