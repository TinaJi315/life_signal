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

    /** 通过扫码获得的 ID 直接添加好友 */
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

    /** 发送提醒 */
    fun sendReminder(friendId: String, onComplete: () -> Unit = {}) {
        val uid = authRepository.currentUid ?: return
        viewModelScope.launch {
            // 这里为了简单，假设给自己发的名字是 "User"
            networkRepository.sendReminder(uid, "Your Friend", friendId)
            onComplete()
        }
    }

    /** 拉黑用户 */
    fun blockUser(friendId: String, onComplete: () -> Unit = {}) {
        val uid = authRepository.currentUid ?: return
        viewModelScope.launch {
            networkRepository.blockUser(uid, friendId)
            onComplete()
        }
    }

    /** 举报用户 */
    fun reportUser(friendId: String, reason: String, onComplete: () -> Unit = {}) {
        val uid = authRepository.currentUid ?: return
        viewModelScope.launch {
            networkRepository.reportUser(uid, friendId, reason)
            onComplete()
        }
    }

    /** 删除好友，双向移除 Firestore 关系 */
    fun removeFriend(friendId: String, onComplete: () -> Unit = {}) {
        val uid = authRepository.currentUid ?: return
        viewModelScope.launch {
            networkRepository.removeFriend(uid, friendId)
            onComplete()
        }
    }

    /** 实时监听单个好友文档 */
    fun getFriendById(friendId: String): Flow<Friend?> {
        val uid = authRepository.currentUid ?: return flow { emit(null) }
        return networkRepository.observeFriendById(uid, friendId)
    }

    /** 注入测试数据 (可以在 init 中调用一次) */
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
