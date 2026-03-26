package com.lifesignal.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.lifesignal.data.model.User
import kotlinx.coroutines.tasks.await

/**
 * 认证存储库
 * 处理 Firebase Authentication 登录/注册/注销
 * 对应前端 ProfilePage 中的 Sign Out 按钮
 */
class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    /** 获取当前登录用户 */
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /** 获取当前用户 UID */
    val currentUid: String?
        get() = auth.currentUser?.uid

    /** 判断用户是否已登录 */
    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    /**
     * 邮箱密码注册
     * 注册成功后会自动在 Firestore 中创建用户文档
     */
    suspend fun register(email: String, password: String, name: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("注册失败：用户为空")

            // 在 Firestore 中创建用户文档
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
     * 邮箱密码登录
     */
    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("登录失败：用户为空")
            Result.success(firebaseUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 注销登录
     * 对应前端 ProfilePage 中的 "Sign Out" 按钮
     */
    fun logout() {
        auth.signOut()
    }

    /**
     * 发送密码重置邮件
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
