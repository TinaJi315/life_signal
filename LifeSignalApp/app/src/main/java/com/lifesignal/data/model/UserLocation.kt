package com.lifesignal.data.model

import com.google.firebase.firestore.GeoPoint
import java.util.Date

/**
 * User location data model (extended)
 * Stored as additional fields in Firestore users/{uid} document
 * Works with Google Maps SDK to display friend locations on map
 *
 * Firestore field descriptions:
 *   geoPoint: GeoPoint          — Latitude/Longitude coordinates (for map Markers)
 *   location: String            — Short location name (e.g. "Home", "Office")
 *   lastLocationUpdate: Date    — Last location update timestamp
 */
data class UserLocation(
    val uid: String = "",
    val name: String = "",
    val geoPoint: GeoPoint? = null,
    val location: String = "",
    val lastLocationUpdate: Date? = null,
    val status: String = "safe",       // For map Marker color display
    val profileImageUrl: String = ""   // For map Marker avatar display
)
