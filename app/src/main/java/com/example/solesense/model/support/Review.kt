package com.example.solesense.model.support

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Review(
    @DocumentId val reviewId: String = "",
    val productId: String = "",
    val userId: String = "",
    val orderId: String = "",
    val orderItemId: String = "",
    val rating: Int = 5,
    val comment: String? = null,
    val imageUrls: List<String> = emptyList(),
    val status: String = "APPROVED", // PENDING, APPROVED, HIDDEN
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)