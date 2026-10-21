package com.example.solesense.model.order

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Order(
    @DocumentId val orderId: String = "",
    val orderCode: String = "",
    val userId: String = "",
    val subtotal: Double = 0.0,
    val shippingFee: Double = 0.0,
    val voucherId: String? = null,
    val discount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val currency: String = "VND",
    val receiverName: String = "",
    val receiverPhone: String = "",
    val shippingAddress: String = "",
    val note: String? = null,
    val cancelReason: String? = null,
    val paymentMethod: String = "COD", // COD, VNPAY, MOMO
    val paymentStatus: String = "PENDING", // PENDING, SUCCESS, FAILED
    val orderStatus: String = "PENDING", // PENDING, SHIPPING, DELIVERED, CANCELLED
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)
