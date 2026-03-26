package com.lifesignal.data.model

import com.google.firebase.firestore.DocumentId

/**
 * 紧急联系人数据模型
 * 对应前端 App.tsx 中的 Contact 接口和 ProfilePage 中的 Emergency Contacts
 *
 * Firestore 集合: users/{uid}/contacts/{contactId}
 */
data class Contact(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val relation: String = "",      // 例如 "Daughter", "Son", "Friend"
    val phone: String = "",
    val initials: String = "",      // 例如 "SM", "AC"
    val colorClass: String = "",    // 颜色标识: "primary", "secondary"
    val ownerUid: String = ""       // 该联系人属于哪个用户
) {
    companion object {
        const val COLLECTION = "contacts"
    }
}
