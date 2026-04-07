package com.lifesignal.ui.screens.network

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifesignal.data.model.Friend
import com.lifesignal.data.model.Group
import com.lifesignal.data.model.User
import com.lifesignal.data.repository.AuthRepository
import com.lifesignal.data.repository.NetworkRepository
import com.lifesignal.data.repository.UserRepository
import com.lifesignal.data.service.CheckInService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NetworkViewModel(application: Application) : AndroidViewModel(application) {
    private val networkRepository = NetworkRepository()
    private val authRepository = AuthRepository()
    private val checkInService = CheckInService()

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        val uid = authRepository.currentUid ?: return
        viewModelScope.launch {
            networkRepository.observeFriends(uid).collect { _friends.value = it }
        }
        viewModelScope.launch {
            networkRepository.observeGroups(uid).collect { _groups.value = it }
        }
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            networkRepository.searchUsers(query).collect { results ->
                _searchResults.value = results
                _isSearching.value = false
            }
        }
    }

    fun addFriendInstant(user: User, onComplete: () -> Unit = {}) {
        val uid = authRepository.currentUid ?: return
        viewModelScope.launch {
            networkRepository.instantAddFriend(uid, user)
            onComplete()
        }
    }

    /** Add friend by scanned QR code ID */
    fun addFriendById(friendId: String, onComplete: (Boolean) -> Unit) {
        val uid = authRepository.currentUid ?: return
        viewModelScope.launch {
            try {
                val user = networkRepository.getUserById(friendId)
                if (user != null) {
                    networkRepository.instantAddFriend(uid, user)
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    /** Send reminder */
    fun sendReminder(friendId: String, onComplete: () -> Unit = {}) {
        val uid = authRepository.currentUid ?: return
        viewModelScope.launch {
            // For simplicity, assume the sender name is "User"
            networkRepository.sendReminder(uid, "Your Friend", friendId)
            onComplete()
        }
    }

    /** Block user */
    fun blockUser(friendId: String, onComplete: () -> Unit = {}) {
        val uid = authRepository.currentUid ?: return
        viewModelScope.launch {
            networkRepository.blockUser(uid, friendId)
            onComplete()
        }
    }

    /** Report user */
    fun reportUser(friendId: String, reason: String, onComplete: () -> Unit = {}) {
        val uid = authRepository.currentUid ?: return
        viewModelScope.launch {
            networkRepository.reportUser(uid, friendId, reason)
            onComplete()
        }
    }

    /** Remove friend, bidirectionally remove Firestore relationship */
    fun removeFriend(friendId: String, onComplete: () -> Unit = {}) {
        val uid = authRepository.currentUid ?: return
        viewModelScope.launch {
            networkRepository.removeFriend(uid, friendId)
            onComplete()
        }
    }

    /** Observe single friend document in real-time */
    fun getFriendById(friendId: String): Flow<Friend?> {
        val uid = authRepository.currentUid ?: return flow { emit(null) }
        return networkRepository.observeFriendById(uid, friendId)
    }

    /** Seed test data (can call once in init) */
    fun seedTestData() {
        viewModelScope.launch {
            networkRepository.seedMockUsers()
        }
    }

    // --- Groups Logic ---
    fun createGroup(name: String, memberIds: List<String>) {
        val uid = authRepository.currentUid ?: return
        viewModelScope.launch {
            networkRepository.createGroup(name, uid, memberIds)
        }
    }

    fun remindAllGroupCheckIn(group: Group) {
        val uid = authRepository.currentUid ?: return
        viewModelScope.launch {
            checkInService.sendGroupReminder(group.id, uid, group.memberIds)
        }
    }
}
