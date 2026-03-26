package com.lifesignal.ui.screens.network

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifesignal.data.model.Friend
import com.lifesignal.data.model.Group
import com.lifesignal.data.model.User
import com.lifesignal.data.repository.AuthRepository
import com.lifesignal.data.repository.NetworkRepository
import com.lifesignal.data.repository.UserRepository
import com.lifesignal.data.service.CheckInService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
        loadNetworkData()
    }

    fun loadNetworkData() {
        val uid = authRepository.currentUid ?: return
        viewModelScope.launch {
            launch {
                networkRepository.observeFriends(uid).collectLatest { list ->
                    _friends.value = list
                }
            }
            launch {
                networkRepository.observeGroups(uid).collectLatest { list ->
                    _groups.value = list
                }
            }
            
            // 为了维持无缝的演示体验，如果发现通讯录为空，我们塞入之前的假名单作为真数据源
            injectMockData(uid)
        }
    }

    private fun injectMockData(uid: String) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val friendsRef = db.collection(User.COLLECTION).document(uid).collection(Friend.COLLECTION)
        
        friendsRef.limit(1).get().addOnSuccessListener { snap ->
            if (snap.isEmpty) {
                val batch = db.batch()
                
                // 还原当初的三名好友
                val f1 = Friend(id = "mock_1", name = "Sarah Miller", status = "safe")
                val f2 = Friend(id = "mock_2", name = "Arthur Chen", status = "overdue")
                val f3 = Friend(id = "mock_3", name = "Elena Rodriguez", status = "safe")
                
                batch.set(friendsRef.document("mock_1"), f1)
                batch.set(friendsRef.document("mock_2"), f2)
                batch.set(friendsRef.document("mock_3"), f3)
                
                // 还原经典的 Family Circle 群组
                val groupRef = db.collection(Group.COLLECTION).document("mock_group_$uid")
                val group = Group(
                    id = "mock_group_$uid", 
                    name = "Family Circle", 
                    ownerUid = uid, 
                    memberIds = listOf(uid, "mock_1", "mock_2", "mock_3"), 
                    memberCount = 4
                )
                batch.set(groupRef, group)
                
                batch.commit()
            }
        }
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        _isSearching.value = true
        // 为了支持边打字边搜（简单版本，未防抖）
        viewModelScope.launch {
            val result = networkRepository.searchUsers(query)
            // 过滤掉当前用户自己
            val uid = authRepository.currentUid
            _searchResults.value = result.getOrNull()?.filter { it.uid != uid } ?: emptyList()
            _isSearching.value = false
        }
    }

    // 强写式好友添加（为了流畅测试体验跳过了发送请求然后等对方同意的繁琐逻辑）
    fun addFriendInstant(user: User, onComplete: () -> Unit = {}) {
        val uid = authRepository.currentUid ?: return
        viewModelScope.launch {
            networkRepository.instantAddFriend(uid, user)
            onComplete()
        }
    }

    fun createGroup(name: String, selectedFriends: List<Friend>, onComplete: () -> Unit = {}) {
        val uid = authRepository.currentUid ?: return
        if (name.isBlank() || selectedFriends.isEmpty()) {
            onComplete()
            return
        }
        
        viewModelScope.launch {
            val memberIds = selectedFriends.map { it.id }
            // 取前三个好友的头像用于群组堆叠UI
            val avatarUrls = selectedFriends.map { it.imageUrl }.take(3) 
            networkRepository.createGroup(name, uid, memberIds, avatarUrls)
            onComplete()
        }
    }

    fun remindAllGroupCheckIn(group: Group) {
        val uid = authRepository.currentUid ?: return
        viewModelScope.launch {
            checkInService.sendGroupReminder(group.id, uid, group.memberIds)
        }
    }
}
