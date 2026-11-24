package com.citacomsys.feature.support.client.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.feature.api.telemetry.PlatformAppUpdater
import com.citacomsys.feature.support.api.AddMessageRequest
import com.citacomsys.feature.support.api.CreateTicketRequest
import com.citacomsys.feature.support.api.SupportTicketRecord
import com.citacomsys.feature.support.api.TicketPriority
import com.citacomsys.feature.support.api.TicketType
import com.citacomsys.feature.support.api.service.SupportApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SupportUiState(
    val tickets: List<SupportTicketRecord> = emptyList(),
    val selectedTicket: SupportTicketRecord? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class SupportViewModel(
    private val supportApiService: SupportApiService,
    private val sessionManager: AppSessionManager,
    private val telemetryService: PlatformAppUpdater
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupportUiState())
    val uiState: StateFlow<SupportUiState> = _uiState.asStateFlow()

    init {
        loadTickets()
    }

    fun loadTickets() {
        val workspaceId = sessionManager.activeWorkspaceId ?: "default-workspace"
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val tickets = supportApiService.getTickets(workspaceId)
                _uiState.update { it.copy(tickets = tickets, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load support items") }
            }
        }
    }

    fun loadTicketDetail(ticketId: String) {
        viewModelScope.launch {
            try {
                val ticket = supportApiService.getTicket(ticketId)
                _uiState.update { it.copy(selectedTicket = ticket) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun submitTicket(
        title: String,
        description: String,
        type: TicketType,
        priority: TicketPriority,
        rating: Int? = null,
        onSuccess: () -> Unit
    ) {
        val workspaceId = sessionManager.activeWorkspaceId ?: "default-workspace"
        val user = sessionManager.appUser.value
        val userId = user?.id ?: sessionManager.currentUserId ?: "anonymous"
        val userEmail = user?.email ?: "user@citacomsys.com"

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val request = CreateTicketRequest(
                    workspaceId = workspaceId,
                    userId = userId,
                    userEmail = userEmail,
                    title = title,
                    description = description,
                    type = type,
                    priority = priority,
                    rating = rating,
                    appVersion = "1.0.0",
                    deviceModel = "Cross-Platform Client"
                )
                val created = supportApiService.createTicket(request)
                _uiState.update { state ->
                    state.copy(
                        tickets = listOf(created) + state.tickets,
                        isLoading = false,
                        successMessage = "Successfully submitted $type!"
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to submit request") }
            }
        }
    }

    fun addMessage(ticketId: String, content: String, onMessageAdded: () -> Unit) {
        val user = sessionManager.appUser.value
        val senderId = user?.id ?: sessionManager.currentUserId ?: "anonymous"
        val senderName = user?.name ?: user?.username ?: "Customer"

        viewModelScope.launch {
            try {
                val request = AddMessageRequest(
                    ticketId = ticketId,
                    senderId = senderId,
                    senderName = senderName,
                    content = content,
                    isSupportAgent = false
                )
                val newMessage = supportApiService.addMessage(request)
                _uiState.update { state ->
                    val updatedTicket = state.selectedTicket?.let { ticket ->
                        ticket.copy(messages = ticket.messages + newMessage)
                    }
                    state.copy(selectedTicket = updatedTicket)
                }
                onMessageAdded()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to send message") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
