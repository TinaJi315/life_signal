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
 * Network Repository
 * Handles friend relationships and group features
 * Corresponds to frontend NetworkPage, AddFriendPage, AddGroupPage, FriendDetailPage, GroupDetailPage
 */
class NetworkRepository {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // ==================== Friend Features ====================

    /**
     * Get user's friend list
     * Corresponds to Friends list on frontend NetworkPage
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
     * Get full user info by UID
     * Used for adding friends via scanned QR code UID
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
     * Observe friend list changes in real-time
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
     * Observe a single friend document in real-time
     * Used for FriendDetailScreen and FriendProfileScreen to show real data
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
     * Send friend request
     * Corresponds to "Add" button on frontend AddFriendPage
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
     * Accept friend request
     * Bidirectionally adds friend relationship
     */
    suspend fun acceptFriendRequest(request: FriendRequest): Result<Unit> {
        return try {
            val batch = firestore.batch()

            // Update request status to accepted
            val requestRef = firestore.collection(FriendRequest.COLLECTION).document(request.id)
            batch.update(requestRef, "status", "accepted")

            // Get profiles of both users
            val fromUser = firestore.collection(User.COLLECTION).document(request.fromUid).get().await()
                .toObject(User::class.java) ?: throw Exception("Sender user not found")
            val toUser = firestore.collection(User.COLLECTION).document(request.toUid).get().await()
                .toObject(User::class.java) ?: throw Exception("Receiver user not found")

            // Add receiver to sender's friend list
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

            // Add sender to receiver's friend list
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
     * Delete friend
     * Corresponds to "Remove" button on frontend FriendDetailPage
     */
    suspend fun removeFriend(uid: String, friendUid: String): Result<Unit> {
        return try {
            val batch = firestore.batch()

            // Remove from both friend lists
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
     * Search for users
     * Corresponds to search bar on frontend AddFriendPage
     */
    fun searchUsers(query: String): Flow<List<User>> = kotlinx.coroutines.flow.flow {
        if (query.isBlank()) {
            emit(emptyList())
            return@flow
        }
        try {
            val users = mutableListOf<User>()
            
            // Name prefix search (case-sensitive, keeping original logic)
            val nameSnapshot = firestore.collection(User.COLLECTION)
                .whereGreaterThanOrEqualTo("name", query)
                .whereLessThanOrEqualTo("name", query + "\uf8ff")
                .limit(20)
                .get()
                .await()
            users.addAll(nameSnapshot.toObjects(User::class.java))

            // Email prefix search (lowercase for case-insensitive matching)
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
            
            // Deduplicate and return
            emit(users.distinctBy { it.uid })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    // ==================== Group Features ====================

    /**
     * Get groups user belongs to
     * Corresponds to Groups list on frontend NetworkPage
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
     * Observe group list changes in real-time
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
     * Create group
     * Corresponds to "Create Group" button on frontend AddGroupPage
     */
    suspend fun createGroup(
        name: String,
        ownerUid: String,
        memberIds: List<String>,
        avatarUrls: List<String> = emptyList()
    ): Result<String> {
        return try {
            // Ensure owner is in the member list
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
     * Add member to group
     * Corresponds to "Add Member" button on frontend GroupDetailPage
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
     * Get group member status list
     * Corresponds to "Member Status" section on frontend GroupDetailPage
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
     * Delete group
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
     * Instant mutual friend add (auto-approved, used for prototype demo)
     */
    suspend fun instantAddFriend(myUid: String, friendUser: User): Result<Unit> {
        return try {
            val batch = firestore.batch()
            val myUser = firestore.collection(User.COLLECTION).document(myUid).get().await()
                .toObject(User::class.java) ?: throw Exception("Current user not found")

            // Me -> Them (write to my contacts)
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

            // Them -> Me (write to their contacts)
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
     * Send check-in reminder
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
     * Block user
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
     * Report user
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
     * Seed mock user data
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
