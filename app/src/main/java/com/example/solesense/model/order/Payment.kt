package com.example.solesense.model.order

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Payment(
    @DocumentId val paymentId: String = "",
    val orderId: String = "",
    val userId: String = "",
    val provider: String = "",
    val amount: Double = 0.0,
    val status: String = "PENDING",
    val transactionId: String? = null,
    val transactionRef: String? = null,
    val qrCodeUrl: String? = null,
    val expiresAt: Timestamp? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val paidAt: Timestamp? = null,
    val updatedAt: Timestamp = Timestamp.now()
)