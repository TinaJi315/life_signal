package com.lifesignal.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifesignal.data.model.CheckIn
import com.lifesignal.data.repository.AuthRepository
import com.lifesignal.data.repository.UserRepository
import com.lifesignal.data.service.CheckInService
import com.lifesignal.data.service.LocationService
import com.lifesignal.data.worker.AlertScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val checkInService = CheckInService()
    private val userRepository = UserRepository()
    private val locationService = LocationService(application)

    private val _isCheckedIn = MutableStateFlow(false)
    val isCheckedIn: StateFlow<Boolean> = _isCheckedIn.asStateFlow()

    private val _lastCheckIn = MutableStateFlow<CheckIn?>(null)
    val lastCheckIn: StateFlow<CheckIn?> = _lastCheckIn.asStateFlow()

    private val _isCheckingIn = MutableStateFlow(false)
    val isCheckingIn: StateFlow<Boolean> = _isCheckingIn.asStateFlow()

    /** Next check-in time (read from Firestore in real-time) */
    private val _nextCheckInDate = MutableStateFlow<Date?>(null)
    val nextCheckInDate: StateFlow<Date?> = _nextCheckInDate.asStateFlow()

    /** Formatted next check-in time text */
    private val _nextCheckInText = MutableStateFlow("—")
    val nextCheckInText: StateFlow<String> = _nextCheckInText.asStateFlow()

    /** Countdown remaining time text */
    private val _remainingTimeText = MutableStateFlow(Pair("—", "—"))
    val remainingTimeText: StateFlow<Pair<String, String>> = _remainingTimeText.asStateFlow()

    /** Check-in history */
    private val _checkInHistory = MutableStateFlow<List<CheckIn>>(emptyList())
    val checkInHistory: StateFlow<List<CheckIn>> = _checkInHistory.asStateFlow()

    /** User safety status: "safe" | "warning" | "emergency" */
    private val _userStatus = MutableStateFlow("safe")
    val userStatus: StateFlow<String> = _userStatus.asStateFlow()

    init {
        loadCheckInStatus()
        observeUserData()
        startCountdownTicker()
    }

    private fun loadCheckInStatus() {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            val result = checkInService.getLastCheckIn(uid)
            if (result.isSuccess) {
                val checkIn = result.getOrNull()
                _lastCheckIn.value = checkIn
                if (checkIn != null) {
                    val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                    val checkInDay = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(checkIn.timestamp)
                    _isCheckedIn.value = today == checkInDay
                }
            }
            // Load check-in history
            val historyResult = checkInService.getCheckInHistory(uid, 20)
            _checkInHistory.value = historyResult.getOrNull() ?: emptyList()
        }
    }

    /** Observe user document in real-time, get nextCheckInTime and status */
    private fun observeUserData() {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.observeUser(uid).collect { user ->
                val nextTime = user?.nextCheckInTime
                _nextCheckInDate.value = nextTime
                _nextCheckInText.value = nextTime?.let { formatNextCheckIn(it) } ?: "Not scheduled"
                updateCountdown(nextTime)

                // Sync user safety status
                _userStatus.value = user?.status ?: "safe"
            }
        }
    }

    /** Triggered every second to update countdown display */
    private fun startCountdownTicker() {
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                updateCountdown(_nextCheckInDate.value)
            }
        }
    }

    private fun updateCountdown(nextTime: Date?) {
        if (nextTime == null) {
            _remainingTimeText.value = Pair("—", "—")
            return
        }
        val now = Date()
        val diffMs = nextTime.time - now.time
        if (diffMs <= 0) {
            _remainingTimeText.value = Pair("0", "00")
            return
        }
        val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs) % 60
        _remainingTimeText.value = Pair(hours.toString(), minutes.toString().padStart(2, '0'))
    }

    private fun formatNextCheckIn(date: Date): String {
        val now = Date()
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(now)
        val dateDay = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(date)
        val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return when (dateDay) {
            today -> "Today ${timeFmt.format(date)}"
            else -> {
                val dayFmt = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                "${dayFmt.format(date)} ${timeFmt.format(date)}"
            }
        }
    }

    /** Force reset to pre-check-in state (for debugging) */
    fun debugResetState() {
        _isCheckedIn.value = false
    }

    fun doCheckIn() {
        val uid = authRepository.currentUser?.uid ?: return
        _isCheckingIn.value = true

        viewModelScope.launch {
            val locationResult = locationService.updateUserLocation(uid)
            val locationName = locationResult.getOrNull()?.locationName ?: "Unknown"

            val result = checkInService.checkIn(uid, locationName)
            if (result.isSuccess) {
                _isCheckedIn.value = true
                _lastCheckIn.value = checkInService.getLastCheckIn(uid).getOrNull()
                // Reload check-in history
                val historyResult = checkInService.getCheckInHistory(uid, 20)
                _checkInHistory.value = historyResult.getOrNull() ?: emptyList()
                // Fetch user settings or use defaults
                val settingsResult = userRepository.getCheckInSettings(uid)
                val settings = settingsResult.getOrNull() ?: com.lifesignal.data.model.CheckInSettings()
                
                val calendar = java.util.Calendar.getInstance().apply {
                    add(java.util.Calendar.DAY_OF_MONTH, 1)
                    set(java.util.Calendar.HOUR_OF_DAY, settings.checkInHour)
                    set(java.util.Calendar.MINUTE, settings.checkInMinute)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                
                // For testing purposes, hardcode delays. 
                // In production, use the real delay calculated from calendar.
                val warningDelay = 30L
                val emergencyDelay = 60L
                
                AlertScheduler.scheduleAlerts(getApplication(), warningDelay, emergencyDelay)
            }
            _isCheckingIn.value = false
        }
    }
}
