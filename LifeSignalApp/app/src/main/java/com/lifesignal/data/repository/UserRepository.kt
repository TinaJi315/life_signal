package com.lifesignal.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.lifesignal.data.model.Contact
import com.lifesignal.data.model.NotificationSettings
import com.lifesignal.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * User Repository
 * Handles CRUD operations for user profiles, emergency contacts, and notification settings
 * Corresponds to frontend ProfilePage, NotificationSettingsPage, AddContactPage
 */
class UserRepository {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // ==================== User Profile ====================

    /**
     * Get user profile
     */
    suspend fun getUser(uid: String): Result<User> {
        return try {
            val doc = firestore.collection(User.COLLECTION)
                .document(uid)
                .get()
                .await()
            val user = doc.toObject(User::class.java) ?: throw Exception("User not found")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe user profile changes in real-time
     * Corresponds to real-time check-in status display on frontend HomePage
     */
    fun observeUser(uid: String): Flow<User?> = callbackFlow {
        val listener: ListenerRegistration = firestore.collection(User.COLLECTION)
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(User::class.java)
                trySend(user)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Update user profile
     * Corresponds to avatar upload on frontend ProfilePage
     */
    suspend fun updateUser(uid: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection(User.COLLECTION)
                .document(uid)
                .set(updates, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user avatar URL
     * Corresponds to Edit button photo upload on frontend ProfilePage
     */
    suspend fun updateProfileImage(uid: String, imageUrl: String): Result<Unit> {
        return updateUser(uid, mapOf("profileImageUrl" to imageUrl))
    }

    // ==================== Emergency Contacts ====================

    /**
     * Get all emergency contacts for user
     * Corresponds to Emergency Contacts list on frontend ProfilePage
     */
    suspend fun getContacts(uid: String): Result<List<Contact>> {
        return try {
            val snapshot = firestore.collection(User.COLLECTION)
                .document(uid)
                .collection(Contact.COLLECTION)
                .get()
                .await()
            val contacts = snapshot.toObjects(Contact::class.java)
            Result.success(contacts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * One-time fetch of all emergency contacts (for Worker background hard pull)
     */
    suspend fun getContactsOnce(uid: String): Result<List<Contact>> {
        return try {
            val snapshot = firestore.collection(User.COLLECTION)
                .document(uid)
                .collection(Contact.COLLECTION)
                .get()
                .await()
            val contacts = snapshot.toObjects(Contact::class.java)
            Result.success(contacts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe emergency contacts list changes in real-time
     */
    fun observeContacts(uid: String): Flow<List<Contact>> = callbackFlow {
        val listener = firestore.collection(User.COLLECTION)
            .document(uid)
            .collection(Contact.COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val contacts = snapshot?.toObjects(Contact::class.java) ?: emptyList()
                trySend(contacts)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Add emergency contact
     * Corresponds to "Save Contact" button on frontend AddContactPage
     */
    suspend fun addContact(uid: String, contact: Contact): Result<String> {
        return try {
            val docRef = firestore.collection(User.COLLECTION)
                .document(uid)
                .collection(Contact.COLLECTION)
                .add(contact)
                .await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete emergency contact
     */
    suspend fun deleteContact(uid: String, contactId: String): Result<Unit> {
        return try {
            firestore.collection(User.COLLECTION)
                .document(uid)
                .collection(Contact.COLLECTION)
                .document(contactId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Notification Settings ====================

    /**
     * Get notification settings
     * Corresponds to initial state of frontend NotificationSettingsPage
     */
    suspend fun getNotificationSettings(uid: String): Result<NotificationSettings> {
        return try {
            val doc = firestore.collection(User.COLLECTION)
                .document(uid)
                .collection("settings")
                .document("notifications")
                .get()
                .await()
            val settings = doc.toObject(NotificationSettings::class.java)
                ?: NotificationSettings(userUid = uid)
            Result.success(settings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update notification settings
     * Corresponds to toggle switches on frontend NotificationSettingsPage
     */
    suspend fun updateNotificationSettings(
        uid: String,
        settings: NotificationSettings
    ): Result<Unit> {
        return try {
            firestore.collection(User.COLLECTION)
                .document(uid)
                .collection("settings")
                .document("notifications")
                .set(settings)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Check-in Settings ====================

    /**
     * Get check-in settings
     */
    suspend fun getCheckInSettings(uid: String): Result<com.lifesignal.data.model.CheckInSettings> {
        return try {
            val doc = firestore.collection(User.COLLECTION)
                .document(uid)
                .collection("settings")
                .document("check_in")
                .get()
                .await()
            val settings = doc.toObject(com.lifesignal.data.model.CheckInSettings::class.java)
                ?: com.lifesignal.data.model.CheckInSettings(userUid = uid)
            Result.success(settings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update check-in settings
     */
    suspend fun updateCheckInSettings(
        uid: String,
        settings: com.lifesignal.data.model.CheckInSettings
    ): Result<Unit> {
        return try {
            firestore.collection(User.COLLECTION)
                .document(uid)
                .collection("settings")
                .document("check_in")
                .set(settings)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
