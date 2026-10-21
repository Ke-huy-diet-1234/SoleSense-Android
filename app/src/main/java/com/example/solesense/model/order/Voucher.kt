package com.example.solesense.model.order

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Voucher(
    @DocumentId val voucherId: String = "",
    val code: String = "",
    val description: String? = null,
    val discountType: String = "PERCENT", // PERCENT hoặc AMOUNT
    val discountValue: Double = 0.0,
    val minOrderValue: Double = 0.0,
    val maxDiscount: Double? = null,
    val usageLimit: Int = 0,
    val usedCount: Int = 0,
    val startAt: Timestamp = Timestamp.now(),
    val endAt: Timestamp = Timestamp.now(),
    val status: String = "ACTIVE"
)