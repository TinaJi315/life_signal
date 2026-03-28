package com.lifesignal.ui.screens.network

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.lifesignal.data.model.User
import com.lifesignal.data.repository.AuthRepository
import com.lifesignal.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class ShareProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    /** 实时 GPS LatLng（用于 Google Maps 地图标记）*/
    private val _currentLatLng = MutableStateFlow<LatLng?>(null)
    val currentLatLng: StateFlow<LatLng?> = _currentLatLng.asStateFlow()

    /** 实时位置地址字符串 */
    private val _locationText = MutableStateFlow<String>("Locating…")
    val locationText: StateFlow<String> = _locationText.asStateFlow()

    /** 是否已有位置权限 */
    private val _hasLocationPermission = MutableStateFlow(false)
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { updateAddress(it) }
        }
    }

    init {
        observeCurrentUser()
        checkPermissionAndStartLocation()
    }

    private fun observeCurrentUser() {
        val uid = authRepository.currentUid ?: return
        viewModelScope.launch {
            userRepository.observeUser(uid).collectLatest { user ->
                _user.value = user
            }
        }
    }

    fun checkPermissionAndStartLocation() {
        val ctx = getApplication<Application>()
        val granted = ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        _hasLocationPermission.value = granted
        if (granted) startLocationUpdates()
    }

    @Suppress("MissingPermission")
    fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L)
            .setMinUpdateIntervalMillis(5_000L)
            .build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, null)

        // 同时拉一次最新位置，避免等待第一次回调
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let { updateAddress(it) }
        }
    }

    private fun updateAddress(location: Location) {
        _currentLatLng.value = LatLng(location.latitude, location.longitude)
        viewModelScope.launch {
            try {
                val geocoder = Geocoder(getApplication(), Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                val addr = addresses?.firstOrNull()
                val text = when {
                    addr == null -> "%.4f°, %.4f°".format(location.latitude, location.longitude)
                    addr.locality != null && addr.adminArea != null ->
                        "${addr.locality}, ${addr.adminArea}"
                    addr.adminArea != null -> addr.adminArea
                    else -> "%.4f°, %.4f°".format(location.latitude, location.longitude)
                }
                _locationText.value = text
            } catch (e: Exception) {
                _locationText.value = "%.4f°, %.4f°".format(location.latitude, location.longitude)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
