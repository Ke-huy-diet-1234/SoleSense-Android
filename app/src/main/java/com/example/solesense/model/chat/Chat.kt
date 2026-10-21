package com.example.solesense.model.chat

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Chat(
    @DocumentId val chatId: String = "",
    val userId: String = "",
    val status: String = "ACTIVE",
    val assignedStaffId: String? = null,
    val lastMessage: String = "",
    val lastMessageAt: Timestamp = Timestamp.now(),
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)