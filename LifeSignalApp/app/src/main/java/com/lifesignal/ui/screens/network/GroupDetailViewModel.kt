package com.lifesignal.ui.screens.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifesignal.data.model.Group
import com.lifesignal.data.model.GroupMemberStatus
import com.lifesignal.data.repository.NetworkRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupDetailViewModel : ViewModel() {
    private val networkRepository = NetworkRepository()

    private val _group = MutableStateFlow<Group?>(null)
    val group: StateFlow<Group?> = _group.asStateFlow()

    private val _memberStatuses = MutableStateFlow<List<GroupMemberStatus>>(emptyList())
    val memberStatuses: StateFlow<List<GroupMemberStatus>> = _memberStatuses.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadGroupData(groupId: String, uid: String) {
        viewModelScope.launch {
            _isLoading.value = true
            // In a better implementation, getGroupById should be in NetworkRepository.
            // For now, we fetch all groups for the user and find the one that matches.
            // networkRepository returns a Flow<List<Group>>
            try {
                val groupList = networkRepository.observeGroups(uid).first()
                val targetGroup = groupList.find { it.id == groupId }
                _group.value = targetGroup
                
                if (targetGroup != null) {
                    val statusResult = networkRepository.getGroupMemberStatuses(targetGroup)
                    if (statusResult.isSuccess) {
                        _memberStatuses.value = statusResult.getOrDefault(emptyList())
                    }
                }
            } catch (e: Exception) {
                // handle error
            }
            _isLoading.value = false
        }
    }

    fun remindAllGroupCheckIn(groupId: String, uid: String) {
        val targetGroup = _group.value ?: return
        viewModelScope.launch {
            // Remind all except the sender (uid)
            targetGroup.memberIds.filter { it != uid }.forEach { memberUid ->
                // Send reminder logic (assumes a user name, we could use a default or fetch it)
                networkRepository.sendReminder(
                    fromUid = uid,
                    fromName = "Group Member",
                    toUid = memberUid
                )
            }
        }
    }
}
