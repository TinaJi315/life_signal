package com.lifesignal.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifesignal.data.model.CheckIn
import com.lifesignal.data.repository.AuthRepository
import com.lifesignal.data.service.CheckInService
import com.lifesignal.data.service.LocationService
import com.lifesignal.data.worker.AlertScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    
    private val authRepository = AuthRepository()
    private val checkInService = CheckInService()
    // 实例化 LocationService 并传入 Application Context以便地理定位
    private val locationService = LocationService(application)

    private val _isCheckedIn = MutableStateFlow(false)
    val isCheckedIn: StateFlow<Boolean> = _isCheckedIn.asStateFlow()

    private val _lastCheckIn = MutableStateFlow<CheckIn?>(null)
    val lastCheckIn: StateFlow<CheckIn?> = _lastCheckIn.asStateFlow()

    private val _isCheckingIn = MutableStateFlow(false)
    val isCheckingIn: StateFlow<Boolean> = _isCheckingIn.asStateFlow()

    init {
        loadCheckInStatus()
    }

    private fun loadCheckInStatus() {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            val result = checkInService.getLastCheckIn(uid)
            if (result.isSuccess) {
                val checkIn = result.getOrNull()
                _lastCheckIn.value = checkIn
                // 暂时定义：如果是今天签到的，就算 isCheckedIn = true (简化逻辑)
                if (checkIn != null) {
                    val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                    val checkInDay = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(checkIn.timestamp)
                    _isCheckedIn.value = today == checkInDay
                }
            }
        }
    }

    // 强置重置，露出原版未打卡 UI 方便反复测！
    fun debugResetState() {
        _isCheckedIn.value = false
    }

    fun doCheckIn() {
        val uid = authRepository.currentUser?.uid ?: return
        _isCheckingIn.value = true
        
        viewModelScope.launch {
            // 先尝试获取定位同步到 Firestore
            val locationResult = locationService.updateUserLocation(uid)
            val locationName = locationResult.getOrNull()?.locationName ?: "Unknown"

            // 执行签到
            val result = checkInService.checkIn(uid, locationName)
            if (result.isSuccess) {
                _isCheckedIn.value = true
                // 重新拉取最新的签到对象
                _lastCheckIn.value = checkInService.getLastCheckIn(uid).getOrNull()
                
                // 【核心连接】签到成功后，在后台埋下一个 30秒倒计时引信！
                // 这意味着如果 30 秒后未再次签到，紧急发短信 Worker 就会开始运行
                AlertScheduler.scheduleAlert(getApplication(), 30)
            }
            _isCheckingIn.value = false
        }
    }
}
