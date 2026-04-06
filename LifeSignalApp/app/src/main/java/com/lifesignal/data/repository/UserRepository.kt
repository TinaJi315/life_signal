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
 * 用户存储库
 * 处理用户个人资料、紧急联系人和通知设置的 CRUD 操作
 * 对应前端 ProfilePage、NotificationSettingsPage、AddContactPage
 */
class UserRepository {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // ==================== 用户资料 ====================

    /**
     * 获取用户资料
     */
    suspend fun getUser(uid: String): Result<User> {
        return try {
            val doc = firestore.collection(User.COLLECTION)
                .document(uid)
                .get()
                .await()
            val user = doc.toObject(User::class.java) ?: throw Exception("用户不存在")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 实时监听用户资料变化
     * 对应前端 HomePage 中实时显示签到状态
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
     * 更新用户资料
     * 对应前端 ProfilePage 中上传头像等操作
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
     * 更新用户头像 URL
     * 对应前端 ProfilePage 中点击 Edit 按钮上传照片
     */
    suspend fun updateProfileImage(uid: String, imageUrl: String): Result<Unit> {
        return updateUser(uid, mapOf("profileImageUrl" to imageUrl))
    }

    // ==================== 紧急联系人 ====================

    /**
     * 获取用户的所有紧急联系人
     * 对应前端 ProfilePage 中的 Emergency Contacts 列表
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
     * 一次性获取所有紧急联系人（供 Worker 后台硬拉取）
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
     * 实时监听紧急联系人列表变化
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
     * 添加紧急联系人
     * 对应前端 AddContactPage 中的 "Save Contact" 按钮
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
     * 删除紧急联系人
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

    // ==================== 通知设置 ====================

    /**
     * 获取通知设置
     * 对应前端 NotificationSettingsPage 的初始状态
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
     * 更新通知设置
     * 对应前端 NotificationSettingsPage 中的 toggle 开关
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

    // ==================== 签到设置 ====================

    /**
     * 获取签到设置
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
     * 更新签到设置
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
