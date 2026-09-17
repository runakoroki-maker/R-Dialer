package com.example.ui.contacts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ContactRepository
import com.example.data.models.ContactItem
import com.example.telecom.TelecomHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ContactsUiState(
    val isLoading: Boolean = false,
    val contacts: List<ContactItem> = emptyList(),
    val searchQuery: String = "",
    val hasPermission: Boolean = false,
    val errorMessage: String? = null
)

class ContactsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ContactRepository(application)

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        checkPermissionAndLoad()
    }

    fun checkPermissionAndLoad() {
        val hasPerm = TelecomHelper.hasContactsPermission(getApplication())
        _uiState.value = _uiState.value.copy(hasPermission = hasPerm)
        if (hasPerm) {
            loadContacts()
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(hasPermission = granted)
        if (granted) {
            loadContacts()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(200)
            loadContacts(query)
        }
    }

    fun refresh() {
        if (_uiState.value.hasPermission) {
            loadContacts(_uiState.value.searchQuery)
        }
    }

    private fun loadContacts(query: String = _uiState.value.searchQuery) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val list = repository.getContacts(query)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    contacts = list
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load contacts"
                )
            }
        }
    }
}
