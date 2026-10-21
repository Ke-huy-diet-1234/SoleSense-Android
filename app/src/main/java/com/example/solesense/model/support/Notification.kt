package com.example.solesense.model.support

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Notification(
    @DocumentId val notificationId: String = "",
    val type: String = "SYSTEM", // SYSTEM, ORDER, PROMO
    val title: String = "",
    val body: String = "",
    val referenceId: String? = null, // ID của đơn hàng hoặc khuyến mãi liên quan
    val isRead: Boolean = false,
    val createdAt: Timestamp = Timestamp.now()
)