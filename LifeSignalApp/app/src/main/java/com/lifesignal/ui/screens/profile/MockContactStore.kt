package com.lifesignal.ui.screens.profile

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color

data class EmergencyContact(
    val id: String,
    val initials: String,
    val name: String,
    val relation: String,
    val color: Color,
    val phone: String
)

object MockContactStore {
    val contacts = mutableStateListOf(
        EmergencyContact(
            id = "1",
            initials = "SM",
            name = "Sarah Miller",
            relation = "Daughter",
            color = Color(0xFF1E2D8C), // Primary Dark Blue
            phone = "1234567890"
        ),
        EmergencyContact(
            id = "2",
            initials = "AC",
            name = "Arthur Chen",
            relation = "Son",
            color = Color(0xFF2E7D32), // Secondary Green
            phone = "0987654321"
        )
    )

    fun addContact(name: String, relation: String, phone: String) {
        // Generate initials
        val parts = name.trim().split(" ")
        val initials = if (parts.size >= 2) {
            "${parts[0].firstOrNull()?.uppercase() ?: ""}${parts[1].firstOrNull()?.uppercase() ?: ""}"
        } else {
            name.take(2).uppercase()
        }

        // Pick a color (cycle through some defaults)
        val colors = listOf(Color(0xFF1E2D8C), Color(0xFF2E7D32), Color(0xFFD32F2F), Color(0xFFE65100), Color(0xFF6A1B9A))
        val color = colors[contacts.size % colors.size]

        contacts.add(
            EmergencyContact(
                id = System.currentTimeMillis().toString(),
                initials = initials,
                name = name.ifEmpty { "Unknown" },
                relation = relation.ifEmpty { "Family" },
                color = color,
                phone = phone
            )
        )
    }
}
