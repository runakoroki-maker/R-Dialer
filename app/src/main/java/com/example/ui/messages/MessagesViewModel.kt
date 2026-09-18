package com.example.ui.messages

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SmsRepository
import com.example.data.models.SmsConversation
import com.example.data.models.SmsMessageItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MessagesUiState(
    val hasPermission: Boolean = false,
    val isLoading: Boolean = false,
    val conversations: List<SmsConversation> = emptyList(),
    val selectedThreadId: Long? = null,
    val selectedAddress: String? = null,
    val selectedName: String? = null,
    val messages: List<SmsMessageItem> = emptyList(),
    val newMessageText: String = ""
)

class MessagesViewModel(application: Application) : AndroidViewModel(application) {
    private val smsRepo = SmsRepository(application)

    private val _uiState = MutableStateFlow(MessagesUiState())
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

    init {
        checkPermissionAndLoad()
    }

    fun checkPermissionAndLoad() {
        val granted = smsRepo.hasPermission()
        _uiState.update { it.copy(hasPermission = granted) }
        if (granted) {
            loadConversations()
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasPermission = granted) }
        if (granted) {
            loadConversations()
        }
    }

    fun loadConversations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val list = smsRepo.getConversations()
            _uiState.update { it.copy(conversations = list, isLoading = false) }
        }
    }

    fun selectConversation(threadId: Long, address: String, name: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedThreadId = threadId, selectedAddress = address, selectedName = name) }
            val msgs = smsRepo.getMessagesForThread(threadId)
            _uiState.update { it.copy(messages = msgs, isLoading = false) }
        }
    }

    fun closeConversation() {
        _uiState.update { it.copy(selectedThreadId = null, selectedAddress = null, selectedName = null, messages = emptyList()) }
        loadConversations()
    }

    fun updateNewMessageText(text: String) {
        _uiState.update { it.copy(newMessageText = text) }
    }

    fun sendSms(address: String, body: String, onComplete: (Boolean) -> Unit) {
        if (address.isBlank() || body.isBlank()) {
            onComplete(false)
            return
        }
        viewModelScope.launch {
            val success = smsRepo.sendSms(address, body)
            if (success) {
                _uiState.update { it.copy(newMessageText = "") }
                _uiState.value.selectedThreadId?.let { tId ->
                    val msgs = smsRepo.getMessagesForThread(tId)
                    _uiState.update { it.copy(messages = msgs) }
                }
                loadConversations()
            }
            onComplete(success)
        }
    }
}
