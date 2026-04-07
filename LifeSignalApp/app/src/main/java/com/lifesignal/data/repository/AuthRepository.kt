package com.lifesignal.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.lifesignal.data.model.User
import kotlinx.coroutines.tasks.await

/**
 * Authentication Repository
 * Handles Firebase Authentication login/registration/logout
 * Corresponds to Sign Out button on frontend ProfilePage
 */
class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    /** Get current logged-in user */
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /** Get current user UID */
    val currentUid: String?
        get() = auth.currentUser?.uid

    /** Check if user is logged in */
    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    /**
     * Register with email and password
     * After registration, automatically creates user document in Firestore
     */
    suspend fun register(email: String, password: String, name: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Registration failed: user is null")

            // Create user document in Firestore
            val user = User(
                uid = firebaseUser.uid,
                name = name,
                email = email,
                shareUrl = "https://lifesignal.app/invite/${name.lowercase().replace(" ", "-")}-${firebaseUser.uid.take(6)}"
            )
            firestore.collection(User.COLLECTION)
                .document(firebaseUser.uid)
                .set(user)
                .await()

            Result.success(firebaseUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Login with email and password
     */
    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Login failed: user is null")
            Result.success(firebaseUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign out
     * Corresponds to "Sign Out" button on frontend ProfilePage
     */
    fun logout() {
        auth.signOut()
    }

    /**
     * Send password reset email
     */
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
