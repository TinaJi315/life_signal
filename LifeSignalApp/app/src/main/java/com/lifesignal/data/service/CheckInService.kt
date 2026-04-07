package com.lifesignal.data.service

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.lifesignal.data.model.CheckIn
import com.lifesignal.data.model.EmergencyIncident
import com.lifesignal.data.model.User
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

/**
 * Check-in Service
 * Handles check-in logic, check-in history, and check-in reminders
 * Corresponds to frontend HomePage's check-in button and check-in status display
 */
class CheckInService {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    /**
     * Execute check-in operation
     * Corresponds to "Safe" button click on frontend HomePage
     * 1. Adds a check-in record to user's checkins subcollection
     * 2. Updates user's isCheckedIn, status, and lastCheckInTime fields
     * 3. Calculates and sets next check-in time
     * 4. If previously in emergency state, resolves the active incident
     */
    suspend fun checkIn(uid: String, location: String = ""): Result<Unit> {
        return try {
            val now = Date()
            val batch = firestore.batch()

            // 1. Add check-in record
            val checkIn = CheckIn(
                userUid = uid,
                status = "safe",
                location = location
            )
            val checkInRef = firestore.collection(User.COLLECTION)
                .document(uid)
                .collection(CheckIn.COLLECTION)
                .document()
            batch.set(checkInRef, checkIn)

            // Get user settings to determine check-in time
            val settingsDoc = firestore.collection(User.COLLECTION)
                .document(uid)
                .collection("settings")
                .document("check_in")
                .get()
                .await()
            val settings = settingsDoc.toObject(com.lifesignal.data.model.CheckInSettings::class.java)
                ?: com.lifesignal.data.model.CheckInSettings()

            // 2. Update user document
            val calendar = Calendar.getInstance().apply {
                time = now
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, settings.checkInHour)
                set(Calendar.MINUTE, settings.checkInMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val userUpdates = mapOf(
                "isCheckedIn" to true,
                "status" to "safe",
                "lastCheckInTime" to now,
                "nextCheckInTime" to calendar.time,
                "updatedAt" to now
            )
            val userRef = firestore.collection(User.COLLECTION).document(uid)
            batch.set(userRef, userUpdates, SetOptions.merge())

            batch.commit().await()

            // 3. Resolve active emergency incidents (if any)
            resolveActiveIncidents(uid)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resolve all unclosed emergency incidents
     * Called when user recovers from warning/emergency state via check-in
     */
    private suspend fun resolveActiveIncidents(uid: String) {
        try {
            val snapshot = firestore.collection(User.COLLECTION)
                .document(uid)
                .collection(EmergencyIncident.COLLECTION)
                .whereEqualTo("resolved", false)
                .get()
                .await()

            val now = Date()
            for (doc in snapshot.documents) {
                doc.reference.update(
                    mapOf(
                        "resolved" to true,
                        "resolvedAt" to now
                    )
                )
            }
        } catch (e: Exception) {
            // Resolving incident failure doesn't affect main check-in flow
            android.util.Log.w("CheckInService", "Failed to resolve active incidents", e)
        }
    }

    /**
     * Get check-in history records
     * Corresponds to "Last Record" and "Previous" display on frontend HomePage
     */
    suspend fun getCheckInHistory(
        uid: String,
        limit: Long = 10
    ): Result<List<CheckIn>> {
        return try {
            val snapshot = firestore.collection(User.COLLECTION)
                .document(uid)
                .collection(CheckIn.COLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
            val history = snapshot.toObjects(CheckIn::class.java)
            Result.success(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get the most recent check-in record
     * Corresponds to "Last Check-in" display on frontend HomePage
     */
    suspend fun getLastCheckIn(uid: String): Result<CheckIn?> {
        return try {
            val snapshot = firestore.collection(User.COLLECTION)
                .document(uid)
                .collection(CheckIn.COLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            val lastCheckIn = snapshot.toObjects(CheckIn::class.java).firstOrNull()
            Result.success(lastCheckIn)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if user has checked in today
     * Determines whether to show "Checked-in" or "Safe" button on frontend HomePage
     */
    suspend fun isCheckedInToday(uid: String): Result<Boolean> {
        return try {
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            val snapshot = firestore.collection(User.COLLECTION)
                .document(uid)
                .collection(CheckIn.COLLECTION)
                .whereGreaterThanOrEqualTo("timestamp", todayStart)
                .limit(1)
                .get()
                .await()

            Result.success(!snapshot.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send check-in reminder to all group members
     * Corresponds to "Remind All Members" button on GroupDetailPage
     * Note: Actual push notifications require Firebase Cloud Messaging (FCM)
     *       This only logs reminder events to Firestore
     */
    suspend fun sendGroupReminder(
        groupId: String,
        senderUid: String,
        memberIds: List<String>
    ): Result<Unit> {
        return try {
            val reminder = mapOf(
                "groupId" to groupId,
                "senderUid" to senderUid,
                "memberIds" to memberIds,
                "type" to "check_in_reminder",
                "timestamp" to Date(),
                "read" to false
            )
            firestore.collection("reminders")
                .add(reminder)
                .await()

            // TODO: Integrate FCM to send push notifications to each member
            // This can be done via Cloud Functions triggers, listening for new documents in reminders collection
            // Then send push notifications to each member's FCM token

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calculate check-in success rate
     * Corresponds to "100% Success Rate" display on FriendDetailPage
     */
    suspend fun getCheckInSuccessRate(uid: String, days: Int = 30): Result<String> {
        return try {
            val startDate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, -days)
            }.time

            val snapshot = firestore.collection(User.COLLECTION)
                .document(uid)
                .collection(CheckIn.COLLECTION)
                .whereGreaterThanOrEqualTo("timestamp", startDate)
                .get()
                .await()

            val totalCheckIns = snapshot.size()
            val rate = if (days > 0) {
                ((totalCheckIns.toFloat() / days) * 100).coerceAtMost(100f).toInt()
            } else {
                0
            }
            Result.success("${rate}% Success Rate")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
