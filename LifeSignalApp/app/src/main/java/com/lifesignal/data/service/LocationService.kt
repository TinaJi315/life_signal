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
 * Location Service
 * Integrates Google Maps / Fused Location Provider
 * Provides real-time user positioning, geocoding, location updates, and friend location viewing
 *
 * Corresponds to frontend App.tsx:
 * - Member.location field ("Home", "Community Center", "Office")
 * - Location info display in FriendDetailPage
 * - Location recording during check-in
 */
class LocationService(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val geocoder: Geocoder = Geocoder(context, Locale.getDefault())

    /**
     * Location data class
     */
    data class LocationData(
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val address: String = "",           // Geocoded address
        val locationName: String = "",      // Short location name, e.g. "Home", "Office"
        val timestamp: Date = Date()
    )

    // ==================== Get Current Location ====================

    /**
     * Get user's current location (one-time)
     * Used for recording location during check-in
     */
    suspend fun getCurrentLocation(): Result<LocationData> {
        if (!hasLocationPermission()) {
            return Result.failure(SecurityException("Missing location permission. Please grant location permission in settings."))
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
                Result.failure(Exception("Unable to get current location"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe real-time location changes
     * Can be used to show user location on map in real-time
     */
    fun observeLocationUpdates(intervalMs: Long = 30000): Flow<LocationData> = callbackFlow {
        if (!hasLocationPermission()) {
            close(SecurityException("Missing location permission"))
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

    // ==================== Firestore Location Sync ====================

    /**
     * Save user's current location to Firestore
     * Called automatically during check-in, or triggered manually
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
     * Get friend's location info
     * Corresponds to location display in FriendDetailPage and NetworkPage
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
     * Get all group members' locations
     * Can display as markers on Google Maps
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

    // ==================== Utility Methods ====================

    /**
     * Geocoding: coordinates -> address
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
     * Extract short location name from full address
     * Corresponds to "Home", "Office", "Community Center" format in frontend
     */
    private fun getShortLocationName(address: String): String {
        if (address.isBlank()) return "Unknown"
        val parts = address.split(",").map { it.trim() }
        return parts.firstOrNull()?.ifBlank { "Unknown" } ?: "Unknown"
    }

    /**
     * Check for location permission
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
