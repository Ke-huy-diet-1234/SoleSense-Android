package com.example.solesense.model.chat

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Message(
    @DocumentId val messageId: String = "",
    val senderType: String = "USER",
    val senderId: String? = null,
    val content: String = "",
    val messageType: String = "TEXT",
    val recommendedProductIds: List<String> = emptyList(),
    val createdAt: Timestamp = Timestamp.now()
)