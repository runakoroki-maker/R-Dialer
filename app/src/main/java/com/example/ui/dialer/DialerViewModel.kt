package com.example.ui.dialer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ContactRepository
import com.example.data.models.ContactItem
import com.example.data.models.SimAccountInfo
import com.example.telecom.SubscriptionHelper
import com.example.telecom.TelecomHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DialerUiState(
    val inputNumber: String = "",
    val matchedContact: ContactItem? = null,
    val isDefaultDialer: Boolean = false,
    val errorMessage: String? = null,
    val activeSims: List<SimAccountInfo> = emptyList(),
    val selectedSim: SimAccountInfo? = null,
    val preferredSimMode: String = SubscriptionHelper.PREF_ALWAYS_ASK,
    val isSimSelectionVisible: Boolean = false
)

class DialerViewModel(application: Application) : AndroidViewModel(application) {
    private val contactRepository = ContactRepository(application)

    private val _uiState = MutableStateFlow(DialerUiState())
    val uiState: StateFlow<DialerUiState> = _uiState.asStateFlow()

    private var contactLookupJob: Job? = null

    init {
        checkDefaultDialerStatus()
        refreshSimInfo()
    }

    fun checkDefaultDialerStatus() {
        val isDefault = TelecomHelper.isDefaultDialer(getApplication())
        _uiState.value = _uiState.value.copy(isDefaultDialer = isDefault)
    }

    fun refreshSimInfo() {
        val sims = SubscriptionHelper.getActiveSubscriptions(getApplication())
        val prefMode = SubscriptionHelper.getPreferredSimMode(getApplication())
        val selected = when (prefMode) {
            SubscriptionHelper.PREF_SIM_1 -> sims.firstOrNull { it.simNumber == 1 }
            SubscriptionHelper.PREF_SIM_2 -> sims.firstOrNull { it.simNumber == 2 }
            else -> sims.firstOrNull()
        }
        _uiState.value = _uiState.value.copy(
            activeSims = sims,
            selectedSim = selected,
            preferredSimMode = prefMode
        )
    }

    fun setSimSelectionVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(isSimSelectionVisible = visible)
    }

    fun appendDigit(digit: String) {
        val newNumber = _uiState.value.inputNumber + digit
        updateNumber(newNumber)
    }

    fun setNumber(number: String) {
        updateNumber(number)
    }

    fun deleteDigit() {
        val current = _uiState.value.inputNumber
        if (current.isNotEmpty()) {
            val newNumber = current.dropLast(1)
            updateNumber(newNumber)
        }
    }

    fun clearNumber() {
        updateNumber("")
    }

    private fun updateNumber(number: String) {
        _uiState.value = _uiState.value.copy(
            inputNumber = number,
            errorMessage = null
        )

        contactLookupJob?.cancel()
        if (number.length >= 2) {
            contactLookupJob = viewModelScope.launch {
                delay(150) // Debounce rapid keypad typing
                val contact = contactRepository.findContactByNumber(number)
                _uiState.value = _uiState.value.copy(matchedContact = contact)
            }
        } else {
            _uiState.value = _uiState.value.copy(matchedContact = null)
        }
    }

    fun onCallInitiated(onFailure: (String) -> Unit) {
        val number = _uiState.value.inputNumber.trim()
        if (number.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid phone number.")
            onFailure("Please enter a valid phone number.")
            return
        }

        refreshSimInfo()
        val sims = _uiState.value.activeSims

        if (sims.size >= 2) {
            val prefMode = _uiState.value.preferredSimMode
            when (prefMode) {
                SubscriptionHelper.PREF_SIM_1 -> {
                    val sim1 = sims.firstOrNull { it.simNumber == 1 }
                    placeCallWithSim(sim1, onFailure)
                }
                SubscriptionHelper.PREF_SIM_2 -> {
                    val sim2 = sims.firstOrNull { it.simNumber == 2 }
                    placeCallWithSim(sim2, onFailure)
                }
                else -> {
                    // Always ask mode -> show dialog
                    _uiState.value = _uiState.value.copy(isSimSelectionVisible = true)
                }
            }
        } else {
            // Single SIM or device fallback
            placeCallWithSim(null, onFailure)
        }
    }

    fun selectSimAndCall(sim: SimAccountInfo, rememberChoice: Boolean, onFailure: (String) -> Unit) {
        _uiState.value = _uiState.value.copy(
            isSimSelectionVisible = false,
            selectedSim = sim
        )
        if (rememberChoice) {
            val newMode = if (sim.simNumber == 1) SubscriptionHelper.PREF_SIM_1 else SubscriptionHelper.PREF_SIM_2
            SubscriptionHelper.setPreferredSimMode(getApplication(), newMode)
            _uiState.value = _uiState.value.copy(preferredSimMode = newMode)
        }
        placeCallWithSim(sim, onFailure)
    }

    fun placeCall(onFailure: (String) -> Unit) {
        onCallInitiated(onFailure)
    }

    private fun placeCallWithSim(sim: SimAccountInfo?, onFailure: (String) -> Unit) {
        val number = _uiState.value.inputNumber.trim()
        if (number.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid phone number.")
            onFailure("Please enter a valid phone number.")
            return
        }

        val handle = if (sim != null) {
            SubscriptionHelper.getPhoneAccountHandleForSubscription(getApplication(), sim.subscriptionId)
        } else null

        val success = TelecomHelper.placeCall(getApplication(), number, handle)
        if (!success) {
            _uiState.value = _uiState.value.copy(errorMessage = "Unable to initiate call on this device.")
            onFailure("Unable to initiate call on this device.")
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
