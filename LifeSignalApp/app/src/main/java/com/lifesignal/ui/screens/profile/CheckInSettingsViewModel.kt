package com.lifesignal.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifesignal.data.model.CheckInSettings
import com.lifesignal.data.repository.AuthRepository
import com.lifesignal.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CheckInSettingsViewModel : ViewModel() {
    private val authRepo = AuthRepository()
    private val userRepository = UserRepository()

    private val _settings = MutableStateFlow(CheckInSettings())
    val settings: StateFlow<CheckInSettings> = _settings.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _isLoading.value = true
            val uid = authRepo.currentUid
            if (uid != null) {
                val result = userRepository.getCheckInSettings(uid)
                result.onSuccess {
                    _settings.value = it
                }
            }
            _isLoading.value = false
        }
    }

    private fun updateAndSave(newSettings: CheckInSettings) {
        _settings.value = newSettings
        viewModelScope.launch {
            val uid = authRepo.currentUid
            if (uid != null) {
                userRepository.updateCheckInSettings(uid, newSettings)
            }
        }
    }

    fun setFrequencyHours(hours: Int) {
        updateAndSave(_settings.value.copy(frequencyHours = hours))
    }

    fun setGracePeriodMinutes(minutes: Int) {
        updateAndSave(_settings.value.copy(gracePeriodMinutes = minutes))
    }
}
