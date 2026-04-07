package com.lifesignal.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Emergency contact data model
 * Corresponds to Contact interface in frontend App.tsx and Emergency Contacts in ProfilePage
 *
 * Firestore collection: users/{uid}/contacts/{contactId}
 */
data class Contact(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val relation: String = "",      // e.g. "Daughter", "Son", "Friend"
    val phone: String = "",
    val initials: String = "",      // e.g. "SM", "AC"
    val colorClass: String = "",    // Color identifier: "primary", "secondary"
    val ownerUid: String = ""       // Which user this contact belongs to
) {
    companion object {
        const val COLLECTION = "contacts"
    }
}
