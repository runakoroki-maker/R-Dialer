package com.example.ui.recents

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CallLogRepository
import com.example.data.models.CallLogEntry
import com.example.telecom.TelecomHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecentsUiState(
    val isLoading: Boolean = false,
    val callLogs: List<CallLogEntry> = emptyList(),
    val hasPermission: Boolean = false,
    val errorMessage: String? = null,
    val selectedProfileEntry: CallLogEntry? = null,
    val profileCallLogs: List<CallLogEntry> = emptyList(),
    val isNumberBlocked: Boolean = false,
    val reportStatusMessage: String? = null
)

class RecentsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CallLogRepository(application)

    private val _uiState = MutableStateFlow(RecentsUiState())
    val uiState: StateFlow<RecentsUiState> = _uiState.asStateFlow()

    init {
        checkPermissionAndLoad()
    }

    fun checkPermissionAndLoad() {
        val hasPerm = TelecomHelper.hasCallLogPermission(getApplication())
        _uiState.value = _uiState.value.copy(hasPermission = hasPerm)
        if (hasPerm) {
            loadCallLogs()
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(hasPermission = granted)
        if (granted) {
            loadCallLogs()
        }
    }

    fun refresh() {
        if (_uiState.value.hasPermission) {
            loadCallLogs()
        }
    }

    fun openCallerProfile(entry: CallLogEntry) {
        viewModelScope.launch {
            val logsForNumber = repository.getCallLogsForNumber(entry.number)
            val blocked = repository.isNumberBlocked(entry.number)
            _uiState.update {
                it.copy(
                    selectedProfileEntry = entry,
                    profileCallLogs = logsForNumber,
                    isNumberBlocked = blocked,
                    reportStatusMessage = null
                )
            }
        }
    }

    fun closeCallerProfile() {
        _uiState.update {
            it.copy(
                selectedProfileEntry = null,
                profileCallLogs = emptyList(),
                reportStatusMessage = null
            )
        }
    }

    fun toggleBlockCurrentNumber() {
        val entry = _uiState.value.selectedProfileEntry ?: return
        val number = entry.number
        val currentlyBlocked = _uiState.value.isNumberBlocked
        viewModelScope.launch {
            val success = repository.setNumberBlocked(number, !currentlyBlocked)
            if (success) {
                val blocked = repository.isNumberBlocked(number)
                _uiState.update { it.copy(isNumberBlocked = blocked) }
            }
        }
    }

    fun reportCurrentNumber() {
        val entry = _uiState.value.selectedProfileEntry ?: return
        _uiState.update { it.copy(reportStatusMessage = "Number ${entry.number} reported as spam/fraud.") }
    }

    fun deleteCallLogEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteCallLog(id)
            loadCallLogs()
            val profileEntry = _uiState.value.selectedProfileEntry
            if (profileEntry != null) {
                val updatedLogs = repository.getCallLogsForNumber(profileEntry.number)
                _uiState.update { it.copy(profileCallLogs = updatedLogs) }
                if (updatedLogs.isEmpty()) {
                    closeCallerProfile()
                }
            }
        }
    }

    private fun loadCallLogs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val list = repository.getCallLogs()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    callLogs = list
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Unable to read call logs"
                )
            }
        }
    }
}

