package com.example.solesense.model.support

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class ReturnRequest(
    @DocumentId val returnId: String = "",
    val orderId: String = "",
    val orderItemId: String = "",
    val userId: String = "",
    val type: String = "RETURN", // RETURN hoặc EXCHANGE
    val reason: String = "",
    val description: String? = null,
    val imageUrls: List<String> = emptyList(),
    val currentSize: String? = null,
    val requestedSize: String? = null,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val adminNote: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)
