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

    /** 下次签到时间（从Firestore实时读取）*/
    private val _nextCheckInDate = MutableStateFlow<Date?>(null)
    val nextCheckInDate: StateFlow<Date?> = _nextCheckInDate.asStateFlow()

    /** 格式化后的下次签到时间文本 */
    private val _nextCheckInText = MutableStateFlow("—")
    val nextCheckInText: StateFlow<String> = _nextCheckInText.asStateFlow()

    /** 倒计时剩余时间文本 */
    private val _remainingTimeText = MutableStateFlow(Pair("—", "—"))  // Pair(hours, minutes)
    val remainingTimeText: StateFlow<Pair<String, String>> = _remainingTimeText.asStateFlow()

    /** 签到历史 */
    private val _checkInHistory = MutableStateFlow<List<CheckIn>>(emptyList())
    val checkInHistory: StateFlow<List<CheckIn>> = _checkInHistory.asStateFlow()

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
            // 加载历史记录
            val historyResult = checkInService.getCheckInHistory(uid, 20)
            _checkInHistory.value = historyResult.getOrNull() ?: emptyList()
        }
    }

    /** 实时监听用户文档，获取 nextCheckInTime */
    private fun observeUserData() {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.observeUser(uid).collect { user ->
                val nextTime = user?.nextCheckInTime
                _nextCheckInDate.value = nextTime
                _nextCheckInText.value = nextTime?.let { formatNextCheckIn(it) } ?: "Not scheduled"
                updateCountdown(nextTime)
            }
        }
    }

    /** 每分钟触发一次，更新倒计时显示 */
    private fun startCountdownTicker() {
        viewModelScope.launch {
            while (true) {
                delay(60_000L) // 每分钟
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

    /** 强置重置，退回签到前状态（调试用）*/
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
                // 重新加载历史
                val historyResult = checkInService.getCheckInHistory(uid, 20)
                _checkInHistory.value = historyResult.getOrNull() ?: emptyList()

                AlertScheduler.scheduleAlert(getApplication(), 30)
            }
            _isCheckingIn.value = false
        }
    }
}
