package com.lifesignal.ui.screens.profile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.storage.FirebaseStorage
import com.lifesignal.data.model.User
import com.lifesignal.data.repository.AuthRepository
import com.lifesignal.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()
    private val storage = FirebaseStorage.getInstance()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isUploadingAvatar = MutableStateFlow(false)
    val isUploadingAvatar: StateFlow<Boolean> = _isUploadingAvatar.asStateFlow()

    init {
        observeCurrentUser()
    }

    private fun observeCurrentUser() {
        val uid = authRepository.currentUid ?: return
        viewModelScope.launch {
            userRepository.observeUser(uid).collectLatest { user ->
                _user.value = user
            }
        }
    }

    /** Upload local image to Firebase Storage, then update Firestore profileImageUrl */
    fun uploadAvatar(imageUri: Uri) {
        val uid = authRepository.currentUid ?: return
        _isUploadingAvatar.value = true
        viewModelScope.launch {
            try {
                val ref = storage.reference.child("profile_images/$uid.jpg")
                ref.putFile(imageUri).await()
                val downloadUrl = ref.downloadUrl.await().toString()
                userRepository.updateProfileImage(uid, downloadUrl)
            } catch (e: Exception) {
                // Silent failure, keep original avatar
            } finally {
                _isUploadingAvatar.value = false
            }
        }
    }

    /** Update profile text */
    fun updateProfile(name: String, phone: String) {
        val uid = authRepository.currentUid ?: return
        viewModelScope.launch {
            userRepository.updateUser(uid, mapOf(
                "name" to name,
                "phone" to phone
            ))
        }
    }
}
