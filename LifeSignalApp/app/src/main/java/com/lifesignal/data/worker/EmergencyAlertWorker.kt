package com.lifesignal.data.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.lifesignal.R
import com.lifesignal.data.model.EmergencyIncident
import com.lifesignal.data.model.User
import com.lifesignal.data.repository.AuthRepository
import com.lifesignal.data.repository.UserRepository
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * 48-hour Emergency Alert Worker
 *
 * Triggered automatically when user hasn't checked in for 48+ hours:
 * 1. Updates user.status to "emergency" in Firestore
 * 2. Gets real GPS coordinates (falls back to Firestore geoPoint)
 * 3. Sends SMS with location link to all emergency contacts
 * 4. Creates EmergencyIncident event log in Firestore
 * 5. Sends high-priority local notification
 */
class EmergencyAlertWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "lifesignal_emergency"
        const val CHANNEL_NAME = "Emergency Alert"
        const val NOTIFICATION_ID = 1002
    }

    override suspend fun doWork(): Result {
        return try {
            val authRepo = AuthRepository()
            val userRepo = UserRepository()
            val firestore = FirebaseFirestore.getInstance()

            val uid = authRepo.currentUid ?: return Result.success()

            // 1. Update status to "emergency"
            val statusUpdates = mapOf(
                "status" to "emergency",
                "updatedAt" to Date()
            )
            firestore.collection(User.COLLECTION)
                .document(uid)
                .set(statusUpdates, SetOptions.merge())
                .await()

            // 2. Get user document (name and stored location)
            val userDoc = firestore.collection(User.COLLECTION)
                .document(uid)
                .get()
                .await()
            val userName = userDoc.getString("name") ?: "LifeSignal User"
            val storedGeoPoint = userDoc.getGeoPoint("geoPoint")
            val storedAddress = userDoc.getString("location") ?: ""

            // 3. Try to get real device GPS coordinates
            var lat = storedGeoPoint?.latitude ?: 0.0
            var lng = storedGeoPoint?.longitude ?: 0.0
            var address = storedAddress

            if (hasLocationPermission()) {
                try {
                    val fusedClient = LocationServices.getFusedLocationProviderClient(appContext)
                    val deviceLocation = fusedClient.lastLocation.await()
                    if (deviceLocation != null) {
                        lat = deviceLocation.latitude
                        lng = deviceLocation.longitude
                        address = if (storedAddress.isNotBlank()) storedAddress else "GPS: $lat, $lng"
                    }
                } catch (e: Exception) {
                    Log.w("EmergencyAlertWorker", "Unable to get device location, using Firestore cache", e)
                }
            }

            // 4. Compose emergency SMS
            val mapLink = "https://maps.google.com/?q=$lat,$lng"
            val message = "🚨 LifeSignal Emergency Alert: $userName has not checked in for over 48 hours!\nLast known location: $address\nMap: $mapLink"

            // 5. Get emergency contacts and send SMS
            val contacts = userRepo.getContactsOnce(uid).getOrNull() ?: emptyList()
            val notifiedPhones = mutableListOf<String>()

            if (contacts.isNotEmpty()) {
                val smsManager = SmsManager.getDefault()
                for (contact in contacts) {
                    val phone = contact.phone
                    if (phone.isNotBlank()) {
                        try {
                            // Split long messages for sending
                            val parts = smsManager.divideMessage(message)
                            smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
                            notifiedPhones.add(phone)
                            Log.d("EmergencyAlertWorker", "Emergency SMS sent to $phone")
                        } catch (e: Exception) {
                            Log.e("EmergencyAlertWorker", "Failed to send SMS to $phone", e)
                        }
                    }
                }
            } else {
                Log.w("EmergencyAlertWorker", "No emergency contacts found, unable to send SMS")
            }

            // 6. Firebase event log
            val incident = mapOf(
                "userUid" to uid,
                "triggeredAt" to Date(),
                "lastKnownLatitude" to lat,
                "lastKnownLongitude" to lng,
                "lastKnownAddress" to address,
                "contactsNotified" to notifiedPhones,
                "resolved" to false,
                "resolvedAt" to null
            )
            firestore.collection(User.COLLECTION)
                .document(uid)
                .collection(EmergencyIncident.COLLECTION)
                .add(incident)
                .await()
            Log.d("EmergencyAlertWorker", "Emergency incident logged to Firebase")

            // 7. Send local emergency notification
            sendEmergencyNotification()

            Result.success()
        } catch (e: Exception) {
            Log.e("EmergencyAlertWorker", "Emergency alert execution failed", e)
            Result.failure()
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            appContext, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(
            appContext, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun sendEmergencyNotification() {
        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Emergency alert — emergency contacts notified"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🚨 LifeSignal — Emergency Alert Triggered")
            .setContentText("Your emergency contacts have been automatically alerted via SMS. Please check in immediately to resolve.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Because you haven't checked in for over 48 hours, the system has automatically sent emergency alert SMS messages with your location to all your emergency contacts. Please open the app and check in immediately to resolve the emergency status.")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
