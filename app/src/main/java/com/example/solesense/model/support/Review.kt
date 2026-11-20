package com.example.solesense.model.support

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Review(
    @DocumentId val reviewId: String = "",
    val productId: String = "",
    val userId: String = "",
    val userName: String = "Khách hàng",
    val userAvatarUrl: String? = null,
    val orderId: String = "",
    val orderItemId: String = "",
    val variantInfo: String = "",
    val rating: Int = 5,
    val comment: String? = null,
    val imageUrls: List<String> = emptyList(), // Tối đa 4 ảnh đã upload lên Cloudinary
    val status: String = "APPROVED", // PENDING, APPROVED, HIDDEN, REJECTED
    val storeReply: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)
