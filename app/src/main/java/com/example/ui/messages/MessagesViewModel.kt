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

import com.example.ai.SmartSmsInfo
import com.example.ai.SmartSmsRecognizer

data class MessagesUiState(
    val hasPermission: Boolean = false,
    val isLoading: Boolean = false,
    val conversations: List<SmsConversation> = emptyList(),
    val selectedThreadId: Long? = null,
    val selectedAddress: String? = null,
    val selectedName: String? = null,
    val messages: List<SmsMessageItem> = emptyList(),
    val newMessageText: String = "",
    val smartInfoMap: Map<String, SmartSmsInfo> = emptyMap(),
    val currentSmartInfo: SmartSmsInfo? = null
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
            // Analyze each conversation snippet
            val map = mutableMapOf<String, SmartSmsInfo>()
            for (conv in list) {
                val info = SmartSmsRecognizer.analyzeSms(conv.address, conv.snippet)
                map[conv.address] = info
            }
            _uiState.update { it.copy(conversations = list, smartInfoMap = map, isLoading = false) }
        }
    }

    fun selectConversation(threadId: Long, address: String, name: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedThreadId = threadId, selectedAddress = address, selectedName = name, currentSmartInfo = null) }
            val msgs = smsRepo.getMessagesForThread(threadId)
            val info = SmartSmsRecognizer.analyzeSms(address, msgs.lastOrNull()?.body ?: "")
            _uiState.update { it.copy(messages = msgs, currentSmartInfo = info, isLoading = false) }
        }
    }

    fun loadDemoZomatoMessage() {
        viewModelScope.launch {
            val sender = "Zomato"
            val body = "Use 483921 to log in to your Zomato account. Do not share it with anyone."
            val smartInfo = SmartSmsRecognizer.analyzeSms(sender, body)
            val demoMsg = SmsMessageItem(
                id = -999L,
                threadId = -999L,
                address = sender,
                body = body,
                date = System.currentTimeMillis(),
                type = 1
            )
            _uiState.update {
                it.copy(
                    selectedThreadId = -999L,
                    selectedAddress = sender,
                    selectedName = "Zomato",
                    messages = listOf(demoMsg),
                    currentSmartInfo = smartInfo
                )
            }
        }
    }

    fun closeConversation() {
        _uiState.update { it.copy(selectedThreadId = null, selectedAddress = null, selectedName = null, messages = emptyList(), currentSmartInfo = null) }
        loadConversations()
    }

    fun rescanCurrentConversation() {
        val address = _uiState.value.selectedAddress ?: return
        val lastMsgBody = _uiState.value.messages.lastOrNull()?.body ?: ""
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val info = SmartSmsRecognizer.analyzeSms(address, lastMsgBody)
            _uiState.update { it.copy(currentSmartInfo = info, isLoading = false) }
        }
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
