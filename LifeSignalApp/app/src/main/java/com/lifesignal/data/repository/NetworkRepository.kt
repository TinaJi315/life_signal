package com.lifesignal.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.lifesignal.data.model.Friend
import com.lifesignal.data.model.FriendRequest
import com.lifesignal.data.model.Group
import com.lifesignal.data.model.GroupMemberStatus
import com.lifesignal.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * 网络存储库
 * 处理好友关系和群组功能
 * 对应前端 NetworkPage、AddFriendPage、AddGroupPage、FriendDetailPage、GroupDetailPage
 */
class NetworkRepository {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // ==================== 好友功能 ====================

    /**
     * 获取用户的好友列表
     * 对应前端 NetworkPage 中的 Friends 列表
     */
    suspend fun getFriends(uid: String): Result<List<Friend>> {
        return try {
            val snapshot = firestore.collection(User.COLLECTION)
                .document(uid)
                .collection(Friend.COLLECTION)
                .get()
                .await()
            val friends = snapshot.toObjects(Friend::class.java)
            Result.success(friends)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 根据 UID 获取完整用户信息
     * 用于通过二维码扫出的 UID 添加好友
     */
    suspend fun getUserById(uid: String): User? {
        return try {
            firestore.collection(User.COLLECTION)
                .document(uid)
                .get()
                .await()
                .toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 实时监听好友列表变化
     */
    fun observeFriends(uid: String): Flow<List<Friend>> = callbackFlow {
        val listener: ListenerRegistration = firestore.collection(User.COLLECTION)
            .document(uid)
            .collection(Friend.COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val friends = snapshot?.toObjects(Friend::class.java) ?: emptyList()
                trySend(friends)
            }
        awaitClose { listener.remove() }
    }

    /**
     * 实时监听单个好友文档
     * 用于 FriendDetailScreen 和 FriendProfileScreen 展示真实数据
     */
    fun observeFriendById(uid: String, friendId: String): Flow<Friend?> = callbackFlow {
        val listener: ListenerRegistration = firestore.collection(User.COLLECTION)
            .document(uid)
            .collection(Friend.COLLECTION)
            .document(friendId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(Friend::class.java))
            }
        awaitClose { listener.remove() }
    }

    /**
     * 发送好友请求
     * 对应前端 AddFriendPage 中的 "Add" 按钮
     */
    suspend fun sendFriendRequest(
        fromUid: String,
        fromName: String,
        toUid: String,
        toName: String,
        relation: String = ""
    ): Result<String> {
        return try {
            val request = FriendRequest(
                fromUid = fromUid,
                toUid = toUid,
                fromName = fromName,
                toName = toName,
                status = "pending",
                relation = relation
            )
            val docRef = firestore.collection(FriendRequest.COLLECTION)
                .add(request)
                .await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 接受好友请求
     * 双向添加好友关系
     */
    suspend fun acceptFriendRequest(request: FriendRequest): Result<Unit> {
        return try {
            val batch = firestore.batch()

            // 更新请求状态为已接受
            val requestRef = firestore.collection(FriendRequest.COLLECTION).document(request.id)
            batch.update(requestRef, "status", "accepted")

            // 获取双方用户资料
            val fromUser = firestore.collection(User.COLLECTION).document(request.fromUid).get().await()
                .toObject(User::class.java) ?: throw Exception("发送方用户不存在")
            val toUser = firestore.collection(User.COLLECTION).document(request.toUid).get().await()
                .toObject(User::class.java) ?: throw Exception("接收方用户不存在")

            // 在发送方的好友列表中添加接收方
            val friendForSender = Friend(
                id = request.toUid,
                name = toUser.name,
                status = toUser.status,
                location = toUser.location,
                imageUrl = toUser.profileImageUrl,
                email = toUser.email,
                phone = toUser.phone
            )
            val senderFriendRef = firestore.collection(User.COLLECTION)
                .document(request.fromUid)
                .collection(Friend.COLLECTION)
                .document(request.toUid)
            batch.set(senderFriendRef, friendForSender)

            // 在接收方的好友列表中添加发送方
            val friendForReceiver = Friend(
                id = request.fromUid,
                name = fromUser.name,
                status = fromUser.status,
                location = fromUser.location,
                imageUrl = fromUser.profileImageUrl,
                email = fromUser.email,
                phone = fromUser.phone
            )
            val receiverFriendRef = firestore.collection(User.COLLECTION)
                .document(request.toUid)
                .collection(Friend.COLLECTION)
                .document(request.fromUid)
            batch.set(receiverFriendRef, friendForReceiver)

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除好友
     * 对应前端 FriendDetailPage 中的 "Remove" 按钮
     */
    suspend fun removeFriend(uid: String, friendUid: String): Result<Unit> {
        return try {
            val batch = firestore.batch()

            // 从双方的好友列表中删除
            val myFriendRef = firestore.collection(User.COLLECTION)
                .document(uid)
                .collection(Friend.COLLECTION)
                .document(friendUid)
            batch.delete(myFriendRef)

            val theirFriendRef = firestore.collection(User.COLLECTION)
                .document(friendUid)
                .collection(Friend.COLLECTION)
                .document(uid)
            batch.delete(theirFriendRef)

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 通过搜索查找用户
     * 对应前端 AddFriendPage 中的搜索栏
     */
    fun searchUsers(query: String): Flow<List<User>> = kotlinx.coroutines.flow.flow {
        if (query.isBlank()) {
            emit(emptyList())
            return@flow
        }
        try {
            val users = mutableListOf<User>()
            
            // 姓名按前缀搜索 (区分大小写原版逻辑，尽量保留)
            val nameSnapshot = firestore.collection(User.COLLECTION)
                .whereGreaterThanOrEqualTo("name", query)
                .whereLessThanOrEqualTo("name", query + "\uf8ff")
                .limit(20)
                .get()
                .await()
            users.addAll(nameSnapshot.toObjects(User::class.java))

            // 邮箱按前缀搜索 (转换为小写，解决大小写找不到的问题)
            val queryLower = query.lowercase()
            if (queryLower.contains("@") || queryLower.isNotEmpty()) {
                val emailSnapshot = firestore.collection(User.COLLECTION)
                    .whereGreaterThanOrEqualTo("email", queryLower)
                    .whereLessThanOrEqualTo("email", queryLower + "\uf8ff")
                    .limit(20)
                    .get()
                    .await()
                users.addAll(emailSnapshot.toObjects(User::class.java))
            }
            
            // 去重并返回
            emit(users.distinctBy { it.uid })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    // ==================== 群组功能 ====================

    /**
     * 获取用户所在的群组列表
     * 对应前端 NetworkPage 中的 Groups 列表
     */
    suspend fun getGroups(uid: String): Result<List<Group>> {
        return try {
            val snapshot = firestore.collection(Group.COLLECTION)
                .whereArrayContains("memberIds", uid)
                .get()
                .await()
            val groups = snapshot.toObjects(Group::class.java)
            Result.success(groups)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 实时监听群组列表变化
     */
    fun observeGroups(uid: String): Flow<List<Group>> = callbackFlow {
        val listener: ListenerRegistration = firestore.collection(Group.COLLECTION)
            .whereArrayContains("memberIds", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val groups = snapshot?.toObjects(Group::class.java) ?: emptyList()
                trySend(groups)
            }
        awaitClose { listener.remove() }
    }

    /**
     * 创建群组
     * 对应前端 AddGroupPage 中的 "Create Group" 按钮
     */
    suspend fun createGroup(
        name: String,
        ownerUid: String,
        memberIds: List<String>,
        avatarUrls: List<String> = emptyList()
    ): Result<String> {
        return try {
            // 确保群主也在成员列表中
            val allMembers = (memberIds + ownerUid).distinct()
            val group = Group(
                name = name,
                ownerUid = ownerUid,
                memberIds = allMembers,
                memberCount = allMembers.size,
                avatarUrls = avatarUrls
            )
            val docRef = firestore.collection(Group.COLLECTION)
                .add(group)
                .await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 向群组添加成员
     * 对应前端 GroupDetailPage 中的 "Add Member" 按钮
     */
    suspend fun addGroupMember(groupId: String, memberUid: String): Result<Unit> {
        return try {
            firestore.collection(Group.COLLECTION)
                .document(groupId)
                .update(
                    mapOf(
                        "memberIds" to FieldValue.arrayUnion(memberUid),
                        "memberCount" to FieldValue.increment(1)
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取群组成员状态列表
     * 对应前端 GroupDetailPage 中的 "Member Status" 部分
     */
    suspend fun getGroupMemberStatuses(group: Group): Result<List<GroupMemberStatus>> {
        return try {
            val statuses = mutableListOf<GroupMemberStatus>()
            for (memberId in group.memberIds) {
                val userDoc = firestore.collection(User.COLLECTION)
                    .document(memberId)
                    .get()
                    .await()
                val user = userDoc.toObject(User::class.java)
                if (user != null) {
                    statuses.add(
                        GroupMemberStatus(
                            name = user.name,
                            status = user.status
                        )
                    )
                }
            }
            Result.success(statuses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除群组
     */
    suspend fun deleteGroup(groupId: String): Result<Unit> {
        return try {
            firestore.collection(Group.COLLECTION)
                .document(groupId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 一键互加好友（免同意全自动直接写入，专门用于原型核心演示）
     */
    suspend fun instantAddFriend(myUid: String, friendUser: User): Result<Unit> {
        return try {
            val batch = firestore.batch()
            val myUser = firestore.collection(User.COLLECTION).document(myUid).get().await()
                .toObject(User::class.java) ?: throw Exception("Current user not found")

            // Me -> Them (写入我的通讯录)
            val friendForMe = Friend(
                id = friendUser.uid,
                name = friendUser.name,
                status = friendUser.status,
                location = friendUser.location,
                imageUrl = friendUser.profileImageUrl,
                email = friendUser.email,
                phone = friendUser.phone
            )
            batch.set(firestore.collection(User.COLLECTION).document(myUid).collection(Friend.COLLECTION).document(friendUser.uid), friendForMe)

            // Them -> Me (写入对方通讯录)
            val friendForThem = Friend(
                id = myUid,
                name = myUser.name,
                status = myUser.status,
                location = myUser.location,
                imageUrl = myUser.profileImageUrl,
                email = myUser.email,
                phone = myUser.phone
            )
            batch.set(firestore.collection(User.COLLECTION).document(friendUser.uid).collection(Friend.COLLECTION).document(myUid), friendForThem)

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 发送签到提醒
     */
    suspend fun sendReminder(fromUid: String, fromName: String, toUid: String) {
        val reminder = hashMapOf(
            "fromUid" to fromUid,
            "fromName" to fromName,
            "type" to "check_in_reminder",
            "timestamp" to FieldValue.serverTimestamp(),
            "status" to "unread"
        )
        firestore.collection(User.COLLECTION)
            .document(toUid)
            .collection("reminders")
            .add(reminder)
            .await()
    }

    /**
     * 拉黑用户
     */
    suspend fun blockUser(uid: String, blockUid: String) {
        removeFriend(uid, blockUid)
        firestore.collection(User.COLLECTION)
            .document(uid)
            .collection("blocked_users")
            .document(blockUid)
            .set(mapOf("blockedAt" to FieldValue.serverTimestamp()))
            .await()
    }

    /**
     * 举报用户
     */
    suspend fun reportUser(uid: String, reportUid: String, reason: String) {
        val report = hashMapOf(
            "reporterUid" to uid,
            "reportedUid" to reportUid,
            "reason" to reason,
            "timestamp" to FieldValue.serverTimestamp(),
            "status" to "pending"
        )
        firestore.collection("reports")
            .add(report)
            .await()
    }

    /**
     * 注入模拟用户数据
     */
    suspend fun seedMockUsers() {
        val mockUsers = listOf(
            User(uid = "user_mock_1", name = "Arthur Chen", email = "arthur.chen@example.com", phone = "+1 (555) 123-4567", shareUrl = "user_mock_1", status = "safe", location = "New York, USA"),
            User(uid = "user_mock_2", name = "Sarah Miller", email = "sarah.m@lifesignal.io", phone = "+1 (555) 987-6543", shareUrl = "user_mock_2", status = "safe", location = "London, UK"),
            User(uid = "user_mock_3", name = "James Wilson", email = "j.wilson@gmail.com", phone = "+1 (555) 444-5555", shareUrl = "user_mock_3", status = "overdue", location = "Toronto, CA"),
            User(uid = "user_mock_4", name = "Emily Davis", email = "emily.davis@outlook.com", phone = "+1 (555) 222-3333", shareUrl = "user_mock_4", status = "safe", location = "Sydney, AU")
        )
        val batch = firestore.batch()
        mockUsers.forEach { user ->
            batch.set(firestore.collection(User.COLLECTION).document(user.uid), user)
        }
        batch.commit().await()
    }
}
