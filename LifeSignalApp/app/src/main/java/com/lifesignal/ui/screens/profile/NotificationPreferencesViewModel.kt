package com.lifesignal.ui.screens.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifesignal.data.model.NotificationSettings
import com.lifesignal.data.repository.AuthRepository
import com.lifesignal.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationPreferencesViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    private val _settings = MutableStateFlow(NotificationSettings())
    val settings: StateFlow<NotificationSettings> = _settings.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val uid = authRepository.currentUid ?: return
        viewModelScope.launch {
            val result = userRepository.getNotificationSettings(uid)
            result.getOrNull()?.let { _settings.value = it }
            _isLoading.value = false
        }
    }

    fun setCheckInAlerts(enabled: Boolean) {
        updateAndSave(_settings.value.copy(missedCheckIn = enabled))
    }

    fun setGroupReminders(enabled: Boolean) {
        updateAndSave(_settings.value.copy(groupReminder = enabled))
    }

    fun setNewFriendAlerts(enabled: Boolean) {
        updateAndSave(_settings.value.copy(newFriend = enabled))
    }

    fun setSystemAlerts(enabled: Boolean) {
        updateAndSave(_settings.value.copy(systemAlerts = enabled))
    }

    private fun updateAndSave(newSettings: NotificationSettings) {
        _settings.value = newSettings
        val uid = authRepository.currentUid ?: return
        viewModelScope.launch {
            userRepository.updateNotificationSettings(uid, newSettings)
        }
    }
}
