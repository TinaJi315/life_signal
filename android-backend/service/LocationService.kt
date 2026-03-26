package com.lifesignal.data.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import com.lifesignal.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.Locale

/**
 * 位置服务
 * 集成 Google Maps / Fused Location Provider
 * 提供用户实时定位、地理编码、位置更新和好友位置查看功能
 *
 * 对应前端 App.tsx 中:
 * - Member.location 字段 ("Home", "Community Center", "Office")
 * - FriendDetailPage 中的位置信息展示
 * - 签到时的位置记录
 */
class LocationService(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val geocoder: Geocoder = Geocoder(context, Locale.getDefault())

    /**
     * 位置数据类
     */
    data class LocationData(
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val address: String = "",           // 地理编码后的地址
        val locationName: String = "",      // 简短位置名, 如 "Home", "Office"
        val timestamp: Date = Date()
    )

    // ==================== 获取当前位置 ====================

    /**
     * 获取用户当前位置（一次性）
     * 用于签到时记录位置
     */
    suspend fun getCurrentLocation(): Result<LocationData> {
        if (!hasLocationPermission()) {
            return Result.failure(SecurityException("缺少位置权限，请在设置中授予位置权限"))
        }

        return try {
            val location = fusedLocationClient.lastLocation.await()
            if (location != null) {
                val address = getAddressFromCoordinates(location.latitude, location.longitude)
                Result.success(
                    LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        address = address,
                        locationName = getShortLocationName(address)
                    )
                )
            } else {
                Result.failure(Exception("无法获取当前位置"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 实时监听位置变化
     * 可用于在地图上实时显示用户位置
     */
    fun observeLocationUpdates(intervalMs: Long = 30000): Flow<LocationData> = callbackFlow {
        if (!hasLocationPermission()) {
            close(SecurityException("缺少位置权限"))
            return@callbackFlow
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    val address = getAddressFromCoordinates(location.latitude, location.longitude)
                    trySend(
                        LocationData(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            address = address,
                            locationName = getShortLocationName(address)
                        )
                    )
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        awaitClose { fusedLocationClient.removeLocationUpdates(callback) }
    }

    // ==================== Firestore 位置同步 ====================

    /**
     * 将用户当前位置保存到 Firestore
     * 签到时自动调用，也可手动触发
     */
    suspend fun updateUserLocation(uid: String): Result<LocationData> {
        val locationResult = getCurrentLocation()
        if (locationResult.isFailure) return locationResult

        val locationData = locationResult.getOrThrow()
        return try {
            val updates = mapOf(
                "location" to locationData.locationName,
                "geoPoint" to GeoPoint(locationData.latitude, locationData.longitude),
                "lastLocationUpdate" to Date(),
                "updatedAt" to Date()
            )
            firestore.collection(User.COLLECTION)
                .document(uid)
                .set(updates, SetOptions.merge())
                .await()
            Result.success(locationData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取好友的位置信息
     * 对应前端 FriendDetailPage 和 NetworkPage 中的位置展示
     */
    suspend fun getFriendLocation(friendUid: String): Result<LocationData> {
        return try {
            val doc = firestore.collection(User.COLLECTION)
                .document(friendUid)
                .get()
                .await()
            val geoPoint = doc.getGeoPoint("geoPoint")
            val locationName = doc.getString("location") ?: ""

            if (geoPoint != null) {
                val address = getAddressFromCoordinates(geoPoint.latitude, geoPoint.longitude)
                Result.success(
                    LocationData(
                        latitude = geoPoint.latitude,
                        longitude = geoPoint.longitude,
                        address = address,
                        locationName = locationName
                    )
                )
            } else {
                Result.success(LocationData(locationName = locationName))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取群组所有成员的位置
     * 可在 Google Maps 上以标记 (Markers) 显示
     */
    suspend fun getGroupMemberLocations(memberIds: List<String>): Result<Map<String, LocationData>> {
        return try {
            val locations = mutableMapOf<String, LocationData>()
            for (memberId in memberIds) {
                val result = getFriendLocation(memberId)
                if (result.isSuccess) {
                    locations[memberId] = result.getOrThrow()
                }
            }
            Result.success(locations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 地理编码：坐标 → 地址
     */
    private fun getAddressFromCoordinates(lat: Double, lng: Double): String {
        return try {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                buildString {
                    addr.thoroughfare?.let { append(it) }
                    addr.subLocality?.let { append(", $it") }
                    addr.locality?.let { append(", $it") }
                }
            } else ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 从完整地址中提取简短位置名
     * 对应前端中 "Home", "Office", "Community Center" 格式
     */
    private fun getShortLocationName(address: String): String {
        if (address.isBlank()) return "Unknown"
        val parts = address.split(",").map { it.trim() }
        return parts.firstOrNull()?.ifBlank { "Unknown" } ?: "Unknown"
    }

    /**
     * 检查是否有位置权限
     */
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
