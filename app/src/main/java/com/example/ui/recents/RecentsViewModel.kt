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
import kotlinx.coroutines.launch

data class RecentsUiState(
    val isLoading: Boolean = false,
    val callLogs: List<CallLogEntry> = emptyList(),
    val hasPermission: Boolean = false,
    val errorMessage: String? = null
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
